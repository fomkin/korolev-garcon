organization := "com.github.fomkin"

name := "korolev-garcon"

version := "0.1.0"

scalaVersion := "2.13.1"

val korolevVersion = "0.14.0"

libraryDependencies ++= Seq(
  "com.github.fomkin" %% "korolev" % korolevVersion,
  "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
  // Test
  "com.github.fomkin" %% "korolev-cats-effect-support" % korolevVersion % "test",
  "org.specs2" %% "specs2-core" % "4.6.0" % "test",
  "com.github.julien-truffaut" %% "monocle-macro" % "2.0.0" % "test",
  "org.typelevel" %% "cats-effect" % "2.0.0" % "test"
)
