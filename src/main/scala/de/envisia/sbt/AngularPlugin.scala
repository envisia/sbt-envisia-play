package de.envisia.sbt

import de.envisia.sbt.angular.{ Angular2, Angular2Exception }
import play.sbt.PlayImport.PlayKeys
import play.sbt.{ PlayInternalKeys, PlayService }
import sbt.Keys.*
import sbt.plugins.JvmPlugin
import sbt.*

object AngularPlugin extends AutoPlugin {
  override def requires: Plugins = JvmPlugin && PlayService

  object autoImport {
    val ngNodeMemory: SettingKey[Int]                 = settingKey[Int]("ng node memory")
    val ngProcessPrefix: SettingKey[String]           = settingKey[String]("ng process prefix (useful for windows)")
    val ngBaseHref: SettingKey[Option[String]]        = settingKey[Option[String]]("ng base href")
    val ngCommand: SettingKey[String]                 = settingKey[String]("ng command")
    val ngDirectory: SettingKey[File]                 = settingKey[File]("ng directory")
    val ngTarget: SettingKey[File]                    = settingKey[File]("ng target")
    val ngBaseDirectory: SettingKey[File]             = settingKey[File]("ngBaseDirectory")
    val npmInstall: TaskKey[Unit]                     = taskKey[Unit]("npmInstall")
    val yarnInstall: TaskKey[Unit]                    = taskKey[Unit]("yarnInstall")
    val ngBuild: TaskKey[Seq[(File, String)]]         = taskKey[Seq[(File, String)]]("ngBuild")
    val ngOutputDirectory: SettingKey[File]           = settingKey[File]("build output directory of angular")
    val ngDevOutputDirectory: SettingKey[File]        = settingKey[File]("dev build output directory of angular")
    val ngLint: TaskKey[Unit]                         = taskKey[Unit]("ng lint")
    val ngPackage: TaskKey[Seq[(File, String)]]       = taskKey[Seq[(File, String)]]("ng package")
    val ngDeployUrl: SettingKey[Option[String]]       = settingKey[Option[String]]("ng deploy url")
    val ngDevModeAot: SettingKey[Boolean]             = settingKey[Boolean]("ng dev mode aot")
    val ngUseYarn: SettingKey[Boolean]                = settingKey[Boolean]("use yarn")
    val ngDisableDevelopmentMode: SettingKey[Boolean] = settingKey[Boolean]("ng dev mode disabled")

    private[sbt] object ngInternal {
      val packageInstall: TaskKey[Unit] = taskKey[Unit]("packageInstall")
    }
  }

  import autoImport.*
  import autoImport.ngInternal.*
  import scala.sys.process.*
  import com.typesafe.sbt.packager.MappingsHelper.*

  class AngularLogger(logger: sbt.Logger) extends ProcessLogger {
    override def out(s: => String): Unit = logger.info(s)
    override def err(s: => String): Unit = logger.error(s)
    override def buffer[T](f: => T): T   = f
  }

  private def runProcessRetCode(log: Logger, command: String, base: File): Int = {
    log.info(s"Running $command...")
    Process(command, base).!(new AngularLogger(log))
  }

  private def runProcessSync(log: Logger, command: String, base: File): Unit = {
    val rc = runProcessRetCode(log, command, base)
    if (rc != 0) {
      throw new Angular2Exception(s"$command failed with $rc")
    }
  }

  private def ngBuildTask = Def.task {
    val baseHref      = ngBaseHref.value
    val ng            = ngCommand.value
    val dir           = ngDirectory.value
    val log           = streams.value.log
    val output        = ngOutputDirectory.value
    val deployUrl     = ngDeployUrl.value
    val withBaseHref  = baseHref.map(h => s"--base-href=$h").getOrElse("")
    val withDeployUrl = deployUrl.map(h => s"--deploy-url=$h").getOrElse("")
    runProcessSync(
      log,
      s"$ng build $withBaseHref --configuration production --progress=false --aot=true $withDeployUrl --output-path=${output.toString}",
      dir
    )
    contentOf(output)
  }

  private def ngBuildAndGzip: Def.Initialize[Task[Seq[(File, String)]]] = Def.taskDyn {
    val targetDir = ngTarget.value / "web" / "gzip"
    val mappings  = ngBuild.value
    val include   = "*.html" || "*.css" || "*.js"
    val exclude   = HiddenFileFilter
    Def.task {
      val gzipMappings = for {
        (file, path) <- mappings if !file.isDirectory && include.accept(file) && !exclude.accept(file)
      } yield {
        val gzipPath = path + ".gz"
        val gzipFile = targetDir / gzipPath
        IO.gzip(file, gzipFile)
        (gzipFile, gzipPath)
      }
      (mappings ++ gzipMappings).map { case (file, path) => (file, "public/" + path) }
    }
  }

  private def ngLintTask: Def.Initialize[Task[Unit]] = Def.task {
    val log  = streams.value.log
    val cmd  = ngCommand.value
    val base = ngDirectory.value

    val retCode1 = runProcessRetCode(log, s"$cmd lint", base)
    if (retCode1 != 0) {
      throw new RuntimeException("ng lint failed")
    }
    val retCode2 = runProcessRetCode(log, s"$cmd build --prod --progress=false", base)
    if (retCode2 != 0) {
      throw new RuntimeException("ng build --prod failed")
    }
  }

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    ngDisableDevelopmentMode := false,
    ngUseYarn                := true,
    ngNodeMemory             := 1024,
    ngDevModeAot             := false,
    ngDeployUrl              := None,
    ngBaseHref               := None,
    ngDirectory              := file("ui"),
    ngProcessPrefix := {
      sys.props("os.name").toLowerCase match {
        case os if os.contains("win") => "cmd /c "
        case _                        => ""
      }
    },
    ngCommand := s"${ngProcessPrefix.value}node --max_old_space_size=${ngNodeMemory.value} node_modules/@angular/cli/bin/ng",
    ngLint   := ngLintTask.dependsOn(packageInstall).value,
    ngTarget := target.value / "web",
    cleanFiles += ngDirectory.value / "dist",
    ngBaseDirectory      := ngDirectory.value,
    ngOutputDirectory    := target.value / "dist",
    ngDevOutputDirectory := ngTarget.value / "public" / "main",
    ngPackage            := ngBuildAndGzip.value,
    packageInstall := Def.taskDyn {
      if (ngDisableDevelopmentMode.value) {
        Def.task(())
      } else {
        if (ngUseYarn.value) Def.task(yarnInstall.value)
        else Def.task(npmInstall.value)
      }
    }.value,
    npmInstall := {
      val log = streams.value.log
      // resolve dependencies before installing
      runProcessSync(log, s"${ngProcessPrefix.value}npm install --legacy-peer-deps", ngDirectory.value)
      runProcessSync(log, s"${ngProcessPrefix.value}npm run postinstall", ngDirectory.value)
    },
    yarnInstall := {
      val log = streams.value.log
      // resolve dependencies before installing
      runProcessSync(log, s"${ngProcessPrefix.value}yarn install", ngDirectory.value)
    },
    Compile / run := (Compile / run).dependsOn(packageInstall).evaluated,
    // includes the angular application
    ngBuild := ngBuildTask.dependsOn(packageInstall).value,
    Compile / packageBin / mappings := {
      if (ngDisableDevelopmentMode.value) {
        (Compile / packageBin / mappings).value
      } else {
        (Compile / packageBin / mappings).value ++ ngPackage.value
      }
    },
    PlayKeys.playRunHooks := {
      if (ngDisableDevelopmentMode.value) {
        PlayKeys.playRunHooks.value
      } else {
        PlayKeys.playRunHooks.value ++ (Angular2(
          ngCommand.value,
          ngBaseHref.value,
          streams.value.log,
          ngBaseDirectory.value,
          target.value,
          ngDevOutputDirectory.value,
          ngDevModeAot.value
        ) :: Nil)
      }
    },
    // Sets the Angular output directory as Play's public directory. This completely replaces the
    // public directory, if you want to use this in addition to the assets in the public directory,
    // then use this instead:
    PlayInternalKeys.playAllAssets := Seq("public/" -> ngDevOutputDirectory.value)
  )

}
