/*
 * Copyright IxiaS, Inc. All Rights Reserved.
 *
 * For the full copyright and license information,
 * please view the LICENSE file that was distributed with this source code.
 */

import play.sbt.routes.RoutesKeys
import com.typesafe.config.ConfigFactory

organization := "net.ixias"
name         := "education-book-app-api"

// ThisBuild scope so the flyway migration sub-projects inherit it too —
// otherwise they fall back to sbt's own Scala 2.12.
ThisBuild / scalaVersion := "3.9.0-RC1"

resolvers ++= Seq(
  "Typesafe Releases" at "https://repo.typesafe.com/typesafe/ivy-releases/",
  "Sonatype Release"  at "https://oss.sonatype.org/content/repositories/releases/",
  "IxiaS Releases"    at "https://s3-ap-northeast-1.amazonaws.com/maven.ixias.net/releases",
  "IxiaS Snapshots"   at "https://s3-ap-northeast-1.amazonaws.com/maven.ixias.net/snapshots"
)

libraryDependencies ++= Seq(
  // --[ Local framework ]-----------------------------------
  // Publish app-lib first:  (cd ../app-lib && sbt publishLocal)
  "net.ixias" %% "education-book-app-lib" % "1.0.0-SNAPSHOT",

  // --[ OSS ]-----------------------------------------------
  "mysql"          %  "mysql-connector-java" % "8.0.33",
  "ch.qos.logback" %  "logback-classic"      % "1.3.3",
  "org.typelevel"  %% "cats-core"            % "2.12.0",
  // Password hashing uses ixias.core.security.PBKDF2 (via app-lib) — no extra dependency.

  guice
)

// FlywayPlugin is enabled per-database on the migration sub-projects below,
// not here — the root project has no single database to point it at.
lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

// Play generates an injected router; controllers are @Inject-constructed.
routesGenerator := InjectedRoutesGenerator

// Custom PathBindable/QueryStringBindable (ixias) available in routes files.
RoutesKeys.routesImport := Seq(
  "mvc.Binders.{ *, given }"
)

scalacOptions ++= Seq(
  "-feature",                  // Warn on features that should be imported explicitly.
  "-Wunused:all",              // Warn on unused code.
  "-Wconf:any:e",              // Treat all warnings as errors.
  "-Werror",                   // Fail the build on any warning.
  "-Wconf:any&src=target/scala-.*/routes/.*:s",  // ...except generated routes.
)

// Point Play at the local config/logger when running from sbt.
javaOptions ++= Seq(
  "-Dconfig.file=conf/application.conf",
  "-Dlogger.file=conf/logback.xml"
)
Compile / run / fork := true

//- Setting flyway
// flyway-sbt exposes exactly one connection per project, so each database gets
// its own throwaway sub-project under `target/migration/<db>` carrying that
// database's settings. `migrateAll` then just sequences their `flywayMigrate`
// tasks. Adding a database is two lines: a `migrateXxx` project below and a
// `db.xxx` block in conf/application.conf.
//
//   sbt migrateAll             # every database
//   sbt migrateApp/flywayMigrate   # just one, plus flywayInfo / flywayClean / …
lazy val applicationConf = ConfigFactory.parseFile(new File("conf/application.conf")).resolve()
lazy val migrationSettings = (dbName: String) => Def.settings(
  flywayDriver    := applicationConf.getString(s"db.$dbName.driver"),
  flywayUrl       := applicationConf.getString(s"db.$dbName.url"),
  flywayUser      := applicationConf.getString(s"db.$dbName.username"),
  flywayPassword  := applicationConf.getString(s"db.$dbName.password"),
  flywayTable     := applicationConf.getString(s"db.$dbName.migration.table"),
  // SQL may legitimately contain `${...}`; don't let Flyway treat it as a placeholder.
  flywayPlaceholderReplacement := false,
  // Paths are relative to the sbt working directory (app-api), not to the
  // sub-project's baseDirectory under target/.
  flywayLocations := {
    val locations = applicationConf.getStringList(s"db.$dbName.migration.locations")
    locations.toArray(Array[String]()).toSeq.map(s"filesystem:../etc/database/migration/$dbName/" + _)
  }
)

lazy val migrateAll = taskKey[Unit]("Migrate all databases.")
lazy val migrateApp = (project in file("target/migration/app"))
  .enablePlugins(FlywayPlugin)
  .settings(migrationSettings("app"))
migrateAll := Def
  .sequential(
    migrateApp / flywayMigrate,
  )
  .value
