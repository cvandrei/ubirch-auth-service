// see http://www.scala-sbt.org/0.13/docs/Parallel-Execution.html for details
concurrentRestrictions in Global := Seq(
  Tags.limit(Tags.Test, 1)
)

val commonSettings = Seq(

  scalaVersion := "2.11.12",
  scalacOptions ++= Seq("-feature"),
  organization := "com.ubirch.auth",

  homepage := Some(url("http://ubirch.com")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/ubirch/ubirch-auth-service"),
    "scm:git:git@github.com:ubirch/ubirch-auth-service.git"
  )),
  version := "0.4.7",
  test in assembly := {},
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  )

)

/*
 * MODULES
 ********************************************************/

lazy val authService = (project in file("."))
  .settings(
    commonSettings,
    skip in publish := true
  )
  .aggregate(
    server,
    clientRest,
    cmdtools,
    config,
    core,
    oidcUtil,
    model,
    modelDb,
    testTools,
    util
  )

lazy val server = project
  .settings(commonSettings: _*)
  .settings(mergeStrategy: _*)
  .dependsOn(config, core, model, util)
  .enablePlugins(DockerPlugin)
  .settings(
    description := "REST interface and Akka HTTP specific code",
    libraryDependencies ++= depServer,
    fork in run := true,
    resolvers ++= Seq(
      resolverSeebergerJson
    ),
    mainClass in(Compile, run) := Some("com.ubirch.auth.server.Boot"),
    resourceGenerators in Compile += Def.task {
      generateDockerFile(baseDirectory.value / ".." / "Dockerfile.input", (assemblyOutputPath in assembly).value)
    }.taskValue
  )

lazy val clientRest = (project in file("client-rest"))
  .settings(commonSettings: _*)
  .dependsOn(config, model, util)
  .settings(
    name := "client-rest",
    description := "REST client of the key-service",
    libraryDependencies ++= depClientRest
  )


lazy val config = project
  .settings(commonSettings: _*)
  .settings(
    description := "trackle specific config and config tools",
    libraryDependencies += ubirchUtilConfig
  )

lazy val cmdtools = project
  .settings(commonSettings: _*)
  .dependsOn(config, modelDb, testTools, testToolsExt)
  .settings(
    description := "command line tools"
  )

lazy val core = project
  .settings(commonSettings: _*)
  .dependsOn(model, modelDb, util, oidcUtil, testTools % "test")
  .settings(
    description := "business logic",
    libraryDependencies ++= depCore
  )

lazy val oidcUtil = (project in file("oidc-util"))
  .settings(commonSettings: _*)
  .dependsOn(config, modelDb)
  .settings(
    name := "oidc-util",
    description := "OpenID Connect utils",
    libraryDependencies ++= depOpenIdUtil
  )

lazy val model = project
  .settings(commonSettings: _*)
  .settings(
    description := "JSON models"
  )

lazy val modelDb = (project in file("model-db"))
  .settings(commonSettings: _*)
  .settings(
    name := "model-db",
    description := "database models"
  )

lazy val testTools = (project in file("test-tools"))
  .settings(commonSettings: _*)
  .dependsOn(config, modelDb, util)
  .settings(
    name := "test-tools",
    description := "tools useful in automated tests",
    libraryDependencies ++= depTestTools
  )

lazy val testToolsExt = (project in file("test-tools-ext"))
  .settings(commonSettings: _*)
  .dependsOn(core)
  .settings(
    name := "test-tools-ext",
    description := "tools useful in automated tests (not in test-tools to avoid circular dependencies between _test-tools_ and _core_)"
  )

lazy val util = project
  .settings(commonSettings: _*)
  .dependsOn(modelDb)
  .settings(
    description := "utils",
    libraryDependencies ++= depUtils
  )

/*
 * MODULE DEPENDENCIES
 ********************************************************/

lazy val depServer = Seq(

  akkaSlf4j,
  akkaHttp,
  akkaStream,
  ubirchUtilRestAkkaHttp,
  ubirchUtilRestAkkaHttpTest % "test",

  ubirchUtilResponse,
  ubirchUserRest

)

lazy val depCore = Seq(
  akkaActor,
  json4sNative,
  ubirchUtilJson,
  ubirchUtilOidcUtils,
  ubirchUtilRedisUtil,
  ubirchUtilResponse,
  scalatest % "test"
) ++ scalaLogging

lazy val depClientRest = Seq(
  akkaHttp,
  akkaStream,
  akkaSlf4j,
  ubirchUtilResponse,
  ubirchUtilDeepCheckModel
) ++ scalaLogging

lazy val depOpenIdUtil = Seq(
  nimbusOidc
) ++ scalaLogging

lazy val depModel = Seq(
  json4sNative
)

lazy val depTestTools = Seq(
  json4sNative,
  ubirchUtilRedisTestUtils,
  scalatest
) ++ scalaLogging

lazy val depUtils = Seq(
  ubirchUtilCrypto,
  ubirchUtilJson,
  rediscala
) ++ scalaLogging

/*
 * DEPENDENCIES
 ********************************************************/

// VERSIONS
val akkaV = "2.5.11"
val akkaHttpV = "10.1.3"
val json4sV = "3.6.0"

val scalaTestV = "3.0.5"

lazy val logbackV = "1.2.3"
lazy val logbackESV = "1.5"
lazy val slf4jV = "1.7.25"
lazy val log4jV = "2.9.1"
lazy val scalaLogV = "3.7.2"
lazy val scalaLogSLF4JV = "2.1.2"

// GROUP NAMES
val ubirchUtilG = "com.ubirch.util"
val json4sG = "org.json4s"
val akkaG = "com.typesafe.akka"
val slf4jG = "org.slf4j"
val typesafeLoggingG = "com.typesafe.scala-logging"
val logbackG = "ch.qos.logback"

lazy val scalatest = "org.scalatest" %% "scalatest" % scalaTestV

lazy val json4sNative = json4sG %% "json4s-native" % json4sV

lazy val scalaLogging = Seq(
  "org.slf4j" % "slf4j-api" % slf4jV,
  "org.slf4j" % "log4j-over-slf4j" % slf4jV,
  "org.slf4j" % "jul-to-slf4j" % slf4jV,
  "ch.qos.logback" % "logback-core" % logbackV,
  "ch.qos.logback" % "logback-classic" % logbackV,
  "net.logstash.logback" % "logstash-logback-encoder" % "5.0",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % scalaLogSLF4JV,
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLogV,
  "com.internetitem" % "logback-elasticsearch-appender" % logbackESV
)

lazy val akkaActor = akkaG %% "akka-actor" % akkaV
lazy val akkaHttp = akkaG %% "akka-http" % akkaHttpV
lazy val akkaSlf4j = akkaG %% "akka-slf4j" % akkaV
lazy val akkaStream = akkaG %% "akka-stream" % akkaV

lazy val nimbusOidc = "com.nimbusds" % "oauth2-oidc-sdk" % "5.64.2"

lazy val rediscala = "com.github.etaty" %% "rediscala" % "1.8.0" excludeAll ExclusionRule(organization = "com.typesafe.akka")

lazy val excludedLoggers = Seq(
  ExclusionRule(organization = typesafeLoggingG),
  ExclusionRule(organization = slf4jG),
  ExclusionRule(organization = logbackG)
)

lazy val ubirchUtilConfig = ubirchUtilG %% "config" % "0.2.3" excludeAll(excludedLoggers: _*)
lazy val ubirchUtilCrypto = ubirchUtilG %% "crypto" % "0.4.11" excludeAll(excludedLoggers: _*)
lazy val ubirchUtilDeepCheckModel = ubirchUtilG %% "deep-check-model" % "0.3.1" excludeAll(excludedLoggers: _*)
lazy val ubirchUtilJson = ubirchUtilG %% "json" % "0.5.1" excludeAll(excludedLoggers: _*)
lazy val ubirchUtilOidcUtils = ubirchUtilG %% "oidc-utils" % "0.8.3" excludeAll (excludedLoggers: _*)
lazy val ubirchUtilRedisTestUtils = ubirchUtilG %% "redis-test-util" % "0.5.2" excludeAll(excludedLoggers: _*)
lazy val ubirchUtilRedisUtil = ubirchUtilG %% "redis-util" % "0.5.2" excludeAll(excludedLoggers: _*)
lazy val ubirchUtilResponse = ubirchUtilG %% "response-util" % "0.4.1" excludeAll(excludedLoggers: _*)
lazy val ubirchUtilRestAkkaHttp = ubirchUtilG %% "rest-akka-http" % "0.4.0" excludeAll(excludedLoggers: _*)
lazy val ubirchUtilRestAkkaHttpTest = ubirchUtilG %% "rest-akka-http-test" % "0.4.0" excludeAll(excludedLoggers: _*)

lazy val ubirchUserRest = "com.ubirch.user" %% "client-rest" % "1.0.1"

/*
 * RESOLVER
 ********************************************************/

val resolverSeebergerJson = Resolver.bintrayRepo("hseeberger", "maven")

/*
 * MISC
 ********************************************************/

lazy val mergeStrategy = Seq(
  assemblyMergeStrategy in assembly := {
    case PathList("org", "joda", "time", xs@_*) => MergeStrategy.first
    case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
    case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
    case m if m.toLowerCase.endsWith("application.conf") => MergeStrategy.concat
    case m if m.toLowerCase.endsWith("application.dev.conf") => MergeStrategy.first
    case m if m.toLowerCase.endsWith("application.base.conf") => MergeStrategy.first
    case m if m.toLowerCase.endsWith("logback.xml") => MergeStrategy.first
    case m if m.toLowerCase.endsWith("logback-test.xml") => MergeStrategy.discard
    case "reference.conf" => MergeStrategy.concat
    case _ => MergeStrategy.first
  }
)

def generateDockerFile(file: File, jarFile: sbt.File): Seq[File] = {
  val contents =
    s"""SOURCE=server/target/scala-2.11/${jarFile.getName}
       |TARGET=${jarFile.getName}
       |""".stripMargin
  IO.write(file, contents)
  Seq(file)
}
