package de.envisia.sbt

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

object CommonPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins      = JvmPlugin

  object autoImport {
    val scalaFatalWarnings: SettingKey[Boolean] = settingKey[Boolean]("enable fatal warnings")
    val formatAll: TaskKey[Unit]                = taskKey[Unit]("format test and compile")
    val formatLint: TaskKey[Unit]               = taskKey[Unit]("format and lint")
  }

  import autoImport._
  import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
  import scala.sys.process._

  private def formatLintTask: Def.Initialize[Task[Unit]] = Def.task {
    val exitCode = "git diff --exit-code".!
    if (exitCode != 0) {
      throw new RuntimeException(
        """|ERROR: Scalafmt check failed, see differences above.
           |To fix, format your sources using 'sbt formatAll' before submitting a pull request.
           |Additionally, please squash your commits (eg, use git commit --amend) if you're going to update this pull request.""".stripMargin
      )
    }
  }

  private def scalacOptions213(fatalWarnings: Boolean) = {
    val withFatalWarnings = if (fatalWarnings) "Werror" :: Nil else Nil
    withFatalWarnings ::: List(
      "-encoding",
      "UTF-8", // yes, this is 2 args
      "-deprecation",
      "-feature",
      "-unchecked",
      // Good warnings
      "-Xcheckinit",
      "-Wdead-code",
      "-Wextra-implicit", // Warn when more than one implicit parameter section is defined.
      "-Wnumeric-widen", // Warn when numerics are widened.
      "-Woctal-literal", // Warn on obsolete octal syntax.
      "-Wself-implicit", // Warn when an implicit resolves to an enclosing self-definition.
      "-Wunused:imports", // Warn if an import selector is not referenced.
      "-Wunused:patvars", // Warn if a variable bound in a pattern is unused.
      "-Wunused:privates", // Warn if a private member is unused.
      "-Wunused:locals", // Warn if a local definition is unused.
      "-Wunused:explicits", // Warn if an explicit parameter is unused.
      "-Wunused:implicits", // Warn if an implicit parameter is unused.
      "-Wunused:params", // Enable -Wunused:explicits,implicits.
      "-Wunused:linted", // -Xlint:unused.
      "-Wvalue-discard", // Warn when non-Unit expression results are unused.
      "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
      "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
      "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
      "-Xlint:infer-any", // Warn when a type argument is inferred to be Any.
      "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
      "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
      "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
      "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
      "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
      "-Xlint:option-implicit", // Option.apply used implicit view.
      "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
      "-Xlint:package-object-classes", // Class or object defined in package object.
      "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
      "-Xlint:unused", // Enable -Ywarn-unused:imports,privates,locals,implicits.
      "-Xlint:nonlocal-return", // A return statement used an exception for flow control.
      "-Xlint:implicit-not-found", // Check @implicitNotFound and @implicitAmbiguous messages.
      "-Xlint:serial", // @SerialVersionUID on traits and non-serializable classes.
      "-Xlint:valpattern", // Enable pattern checks in val definitions.
      "-Xlint:eta-zero", // Warn on eta-expansion (rather than auto-application) of zero-ary method.
      "-Xlint:eta-sam" // Warn on eta-expansion to meet a Java-defined functional interface that is not explicitly annotated with @FunctionalInterface.
    )
  }
  private def scalacOptions212(fatalWarnings: Boolean) = {
    // error on any warning
    val withFatalWarnings = if (fatalWarnings) "-Xfatal-warnings" :: Nil else Nil
    withFatalWarnings ::: List(
      "-encoding",
      "UTF-8", // yes, this is 2 args
      "-deprecation",
      "-feature",
      "-unchecked",
      // List from https://tpolecat.github.io/2017/04/25/scalac-flags.html:
      "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
      "-Xfatal-warnings", // Fail the compilation if there are any warnings.
      "-Xfuture", // Turn on future language features.
      "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
      "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
      "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
      "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
      "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
      "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
      "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
      "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
      "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
      "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
      "-Xlint:option-implicit", // Option.apply used implicit view.
      "-Xlint:package-object-classes", // Class or object defined in package object.
      "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
      "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
      "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
      "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
      "-Xlint:unsound-match",         // Pattern match may not be typesafe.

      // important params
      "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
      "-Ypartial-unification", // Enable partial unification in type constructor inference
      "-Ywarn-dead-code", // Warn when dead code is identified.
      "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
      "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
      "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
      "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
      "-Ywarn-nullary-unit",     // Warn when nullary methods return Unit.
      // currently we allow numeric-widening since a lot of projects use it
      // "-Ywarn-numeric-widen", // Warn when numerics are widened.
      "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
      "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
      "-Ywarn-unused:locals", // Warn if a local definition is unused.
      "-Ywarn-unused:params", // Warn if a value parameter is unused.
      "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
      "-Ywarn-unused:privates" // Warn if a private member is unused.
    )
  }

  override def projectSettings = Seq(
    formatAll := {
      (Compile / scalafmt).value
      (Test / scalafmt).value
    },
    formatLint := formatLintTask.dependsOn(formatAll).value,
    // disables fatal warnings in the sbt console
    scalacOptions in console in Compile -= "-Xfatal-warnings",
    scalacOptions in console in Test -= "-Xfatal-warnings",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    // disable gigahorse
    updateOptions := updateOptions.value.withGigahorse(false),
    updateOptions := updateOptions.value.withCachedResolution(true),
    resolvers ++= Seq(
      // currently only the internal repository needs a password while
      // the public repository is open for everybody, so maybe you need to remove resolvers
      "Envisia Internal" at "https://nexus.envisia.io/repository/internal/",
      "Envisia Open" at "https://nexus.envisia.io/repository/public/"
    ),
    // disable doc creation
    doc in Compile := (target.value / "none"),
    sources in (Compile, doc) := Seq.empty,
    publishArtifact in (Compile, packageDoc) := false,
    // Tests
    scalacOptions in Test ++= Seq("-Yrangepos"),
    scalaFatalWarnings := true,
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v >= 13 => scalacOptions213(scalaFatalWarnings.value)
        case Some((2, v)) if v == 12 => scalacOptions212(scalaFatalWarnings.value)
        case _ => throw new Exception("invalid scala version")
      }
    },
  )

}
