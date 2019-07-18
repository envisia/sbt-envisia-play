package de.envisia.sbt.angular

import java.net.InetSocketAddress

import play.sbt.PlayRunHook
import sbt._

import scala.sys.process.Process

object Angular2 {

  def apply(
      ng: String,
      ngBaseHref: Option[String],
      log: Logger,
      base: File,
      target: File,
      targetFolder: File,
      ngAot: Boolean
  ): PlayRunHook = {
    val withBaseHref = ngBaseHref.map(h => s"--base-href=$h").getOrElse("")
    val withAot = if (ngAot) s"--aot" else ""

    object Angular2Process extends PlayRunHook {
      private var watchProcess: Option[Process] = None

      override def beforeStarted(): Unit = {
        val cacheFile = target / "package-json-last-modified"
        val cacheLastModified = if (cacheFile.exists()) {
          try {
            IO.read(cacheFile).trim.toLong
          } catch {
            case _: NumberFormatException => 0l
          }
        }
        val lastModified = (base / "package.json").lastModified()
        // Check if package.json has changed since we last ran this
        if (cacheLastModified != lastModified) {
          IO.write(cacheFile, lastModified.toString)
        }
      }

      override def afterStarted(): Unit = {
        watchProcess = Some(
          Process(
            s"$ng build $withBaseHref $withAot --watch --delete-output-path=false --progress=false --output-path=${targetFolder.toString}",
            base
          ).run
        )
      }

      override def afterStopped(): Unit = {
        watchProcess.foreach(_.destroy())
        watchProcess = None
      }

    }

    Angular2Process
  }

}
