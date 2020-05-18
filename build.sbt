import ReleaseTransformations._
import xerial.sbt.Sonatype._

/** Versions */
val EnvisiaScalaVersion = "2.12.10"
val PlayVersion = "2.8.1"
val ScalaFmtVersion = "1.5.1"

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
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % PlayVersion)
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % ScalaFmtVersion)
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

