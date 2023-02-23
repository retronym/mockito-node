ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "mockito-node",
    idePackagePrefix := Some("demo.mockitonode"),
    libraryDependencies += "org.mockito" %% "mockito-scala" % "1.17.12",
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.1.0"
  )
