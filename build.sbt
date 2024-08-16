import ReleaseTransformations._
import xerial.sbt.Sonatype._

/** Versions */
val EnvisiaScalaVersion = "2.12.18"
val PlayVersion = "3.0.5"
val ScalaFmtVersion = "2.4.6"

name := "sbt-envisia-play"
organization := "de.envisia.sbt"
scalaVersion := EnvisiaScalaVersion
// publishing settings
sonatypeProfileName := "de.envisia"
publishMavenStyle := true
licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
sonatypeProjectHosting := Some(GitHubHosting("schmitch", "sbt-envisia-play", "c.schmitt@briefdomain.de"))
homepage := Some(url("https://www.envisia.de"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/envisia/sbt-envisia-play"),
    "scm:git@github.com:envisia/sbt-envisia-play.git"
  )
)
developers := List(
  Developer(
    id = "schmitch",
    name = "Christian Schmitt",
    email = "c.schmitt@briefdomain.de",
    url = url("https://github.com/envisia")
  )
)
publishTo := sonatypePublishTo.value

sbtPlugin := true
addSbtPlugin("org.playframework" % "sbt-plugin" % PlayVersion)
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % ScalaFmtVersion)
enablePlugins(BuildInfoPlugin)

buildInfoPackage := "de.envisia.sbt.info"
buildInfoKeys := Seq[BuildInfoKey](
  name,
  version,
  scalaVersion,
  sbtVersion,
  "playVersion" -> PlayVersion,
)

releaseCrossBuild := true
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)
