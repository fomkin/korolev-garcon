import xerial.sbt.Sonatype._

enablePlugins(GitVersioning, HeaderPlugin)

organization := "com.github.fomkin"

name := "korolev-garcon"

git.useGitDescribe := true

crossScalaVersions := Seq("2.12.10", "2.13.1")

val korolevVersion = "0.15.0"

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

publishTo := sonatypePublishTo.value

sonatypeProfileName := "org.github.fomkin"

sonatypeProjectHosting := Some(GitHubHosting("fomkin", "korolev", "Aleksey Fomkin", "aleksey.fomkin@gmail.com"))

headerLicense := Some(HeaderLicense.ALv2("2019", "Aleksey Fomkin"))

licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

libraryDependencies ++= Seq(
  "com.github.fomkin" %% "korolev" % korolevVersion,
  "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
  // Test
  "com.github.fomkin" %% "korolev-cats" % korolevVersion % "test",
  "org.specs2" %% "specs2-core" % "4.6.0" % "test",
  "com.github.julien-truffaut" %% "monocle-macro" % "2.0.0" % "test",
  "org.typelevel" %% "cats-effect" % "2.0.0" % "test"
)
