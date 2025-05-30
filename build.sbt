import ReleaseTransformations.*

/** Versions */
val EnvisiaScalaVersion = "2.12.20"
val PlayVersion = "3.0.6"
val ScalaFmtVersion = "2.5.4"

name := "sbt-envisia-play"
organization := "de.envisia.sbt"
scalaVersion := EnvisiaScalaVersion
// publishing settings
publishMavenStyle := true
pomIncludeRepository := (_ => false)
licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
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

publishTo := Some("envisia" at "https://nexus.envisia.io/repository/public/")

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

releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseCrossBuild := true
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  pushChanges
)

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)
