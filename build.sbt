name := """repose-playground"""

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
libraryDependencies += "com.spotify" % "docker-client" % "3.5.1"
libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19"

jacoco.settings

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

resolvers += "Repose OSS Snapshots" at "https://maven.research.rackspacecloud.com/content/repositories/releases"
resolvers += Resolver.url("Typesafe Ivy releases", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)

// fork in run := true
fork in run := false
//offline := true
