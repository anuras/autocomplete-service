import java.util.Properties
import Keys._

val appProperties = settingKey[Properties]("The project properties")

appProperties in ThisBuild := {
  val prop = new Properties()
  IO.load(prop, new File("./project.properties"))
  prop
}

lazy val commonResolvers = Seq[Resolver](
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  Resolver.jcenterRepo
)

lazy val nettyAllVersion = "4.1.12.Final"

lazy val commonSettings = Seq(
  version := appProperties.value.getProperty("version"),
  scalaVersion := "2.11.11",
  organization := "com.anuras",
  resolvers ++= commonResolvers,
  parallelExecution in Test := false//,
//  dependencyOverrides += "io.netty" % "netty-all" % nettyAllVersion,
//  dependencyOverrides += "io.netty" % "netty-buffer" % nettyAllVersion,
//  dependencyOverrides += "io.netty" % "netty-codec" % nettyAllVersion,
////  dependencyOverrides += "io.netty" % "netty-codec-http" % nettyAllVersion,
////  dependencyOverrides += "io.netty" % "netty-codec-socks" % nettyAllVersion,
//  dependencyOverrides += "io.netty" % "netty-common" % nettyAllVersion,
//  dependencyOverrides += "io.netty" % "netty-handler" % nettyAllVersion,
////  dependencyOverrides += "io.netty" % "netty-handler-proxy" % nettyAllVersion,
//  dependencyOverrides += "io.netty" % "netty-resolver" % nettyAllVersion,
//  dependencyOverrides += "io.netty" % "netty-transport" % nettyAllVersion
  ,
  assemblyMergeStrategy in assembly := {
    case PathList(ps @ _*) if ps.last endsWith "io.netty.versions.properties" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith "StaticLoggerBinder.class" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith "StaticMDCBinder.class" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith "StaticMarkerBinder.class" => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

lazy val serverDependencies = Seq(
  "com.github.finagle" %% "finch-core" % "0.20.0",
  "com.github.finagle" %% "finch-circe" % "0.20.0",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "io.circe" %% "circe-core" % "0.9.0-M1",
  "io.circe" %% "circe-generic" % "0.9.0-M1",
  "io.circe" %% "circe-parser" % "0.9.0-M1",
  "com.typesafe" % "config" % "1.3.2"
)

//val log4jVersion = "2.6.2"

lazy val testDependecies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "com.sksamuel.elastic4s" %% "elastic4s-testkit" % elastic4sVersion % "test"
).map(_.exclude("io.netty", "netty"))
  .map(_.exclude("io.netty", "netty-all"))

lazy val elastic4sVersion = "6.2.4"

val elastic4sDependencies = Seq(
  "com.sksamuel.elastic4s" %% "elastic4s-core" % elastic4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-embedded" % elastic4sVersion,
  "org.slf4j" % "slf4j-simple" % "1.7.12"
).map(_.exclude("io.netty", "netty"))
  .map(_.exclude("io.netty", "netty-all"))

val elasticJavaClient = Seq(
  "org.elasticsearch.client" % "transport" % "6.2.4"
)

lazy val schemaDependencies = Seq(
  "com.typesafe.play"      %% "play-json"        % "2.5.14"
)

lazy val utilDependencies = Seq(
  "com.github.scopt" %% "scopt" % "3.7.0"
)

lazy val root = (project in file("."))
  .settings(moduleName := "autocomplete")
  .settings(commonSettings: _*)
  .aggregate(api)
  .settings(
    publish := {},
    publishArtifact := false
  )

lazy val schemas = (project in file("schemas"))
  .settings(moduleName := "autocomplete-schemas")
  .settings(commonSettings: _*)

lazy val api = (project in file("api"))
  .settings(moduleName := "autocomplete-api")
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= serverDependencies ++ elastic4sDependencies  ++ testDependecies ++ elasticJavaClient)
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(publish := {})
  .settings(
    Seq(
      packageName in Docker := "anuras/autocomplete",
      dockerBaseImage := "java:8",
      publishArtifact := false
    ))
  .dependsOn(schemas)

lazy val populator = (project in file("populator"))
  .settings(moduleName := "autocomplete-populator")
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= elastic4sDependencies  ++ testDependecies ++ utilDependencies)
  .dependsOn(schemas)
