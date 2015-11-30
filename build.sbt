name := """my-first-app"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava, PlayEbean)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs
)

libraryDependencies += "junit" % "junit" % "4.11"
libraryDependencies += "org.json" % "json" % "20090211"


// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

resolvers += "Repose OSS Snapshots" at "https://maven.research.rackspacecloud.com/content/repositories/releases"

// fork in run := true
fork in run := false
//offline := true
