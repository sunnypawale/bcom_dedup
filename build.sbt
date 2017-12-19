import sbt.Keys._

/////////////////////////////////////////////////////////////////////////////
// PROJECT INFO
//////////////////////////////////////////////////////////////////////////////

val Organization = "com.agoda"

name := "bcom-dedup-app"

version := "1.0"

scalaVersion := "2.11.12"


exportJars := true

mainClass in Compile := Option("com.agoda.bcom.dedup.Boot")

mainClass in assembly := Some("com.agoda.bcom.dedup.Boot")

enablePlugins(UniversalPlugin, JavaServerAppPackaging)

//////////////////////////////////////////////////////////////////////////////
// PROJECT
//////////////////////////////////////////////////////////////////////////////

lazy val supply_dedup_app = Project(
  id = "bcom-dedup-app",
  base = file("."),
  settings = commonSettings
)


//////////////////////////////////////////////////////////////////////////////
// DEPENDENCY VERSIONS
//////////////////////////////////////////////////////////////////////////////

val TYPESAFE_CONFIG_VERSION = "1.3.1"
val JODA_VERSION = "2.9.9"
val LOGBACK_VERSION = "1.2.3"
val DCS_COMMON_VERSION = "3.3.11"
val ADP_MESSAGING = "2.3.2.11"
val SPEC2_VERSION = "4.0.0"
val CIRCE_VERSION = "0.8.0"
val JACKSON_VERSION = "2.8.8"


lazy val commonSettings = Defaults.coreDefaultSettings ++ basicSettings

lazy val basicSettings = Seq(
  organization := Organization,
  aggregate in update := false,

  updateOptions := updateOptions.value.withCachedResolution(true),
  resolvers ++= Seq(
    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  ),

  libraryDependencies ++= Seq(
    "com.typesafe" % "config" % TYPESAFE_CONFIG_VERSION,
    "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.0.0-M10",
    "ch.qos.logback" % "logback-classic" % LOGBACK_VERSION,
    "joda-time" % "joda-time" % JODA_VERSION,
    "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % JACKSON_VERSION,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % JACKSON_VERSION,
    "com.h2database" % "h2" % "1.4.196",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
    "com.google.guava" % "guava" % "19.0"
  ),

  scalacOptions in Compile ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature"
  ),

  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
    case PathList("META-INF", "LICENSE") => MergeStrategy.discard
    case "application.conf" => MergeStrategy.concat
    case "reference.conf" => MergeStrategy.concat
    case _ => MergeStrategy.first
  },

  ivyScala := ivyScala.value map {
    _.copy(overrideScalaVersion = true)
  },

  // ignore slf4j-log4j
  libraryDependencies ~= {
    _.map(_.exclude("org.slf4j", "slf4j-log4j12"))
  },

  publishArtifact in(Compile, packageDoc) := false,

  publishArtifact in packageDoc := false,

  sources in(Compile, doc) := Seq.empty
)

//////////////////////////////////////////////////////////////////////////////
// Build and Packaging
//////////////////////////////////////////////////////////////////////////////

// we specify the name for our fat jar
assemblyJarName in assembly := s"${name.value}-${version.value}-all.jar"

mappings in Universal ++= {
  val confDir = (sourceDirectory in Compile).value / "resources"
  for {
    (file, relativePath) <- (confDir.*** --- confDir) x relativeTo(confDir)
  } yield file -> s"conf/$relativePath"
}

// removes all jar mappings in universal and appends the fat jar
mappings in Universal <<= (mappings in Universal, assembly in Compile) map { (mappings, fatJar) =>
  val filtered = mappings filter { case (file, name) => !name.endsWith(".jar") }
  filtered :+ (fatJar -> ("lib/" + fatJar.getName))
}

// the bash scripts classpath only needs the fat jar
scriptClasspath := Seq((assemblyJarName in assembly).value)
