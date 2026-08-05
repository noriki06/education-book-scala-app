/*
 * Copyright IxiaS, Inc. All Rights Reserved.
 *
 * For the full copyright and license information,
 * please view the LICENSE file that was distributed with this source code.
 */

import scala.sys.process.*

// build mode: release branches resolve stable ixias, others the SNAPSHOT.
val branch       = "git branch".lineStream_!.find(_.head == '*').map(_.drop(2)).getOrElse("")
val release      = branch == "main" || branch.startsWith("release")
val ixiasVersion = if (release) "3.1.3" else "3.1.3-SNAPSHOT"

val commonSettings = Seq(
  organization := "net.ixias",
  scalaVersion := "3.9.0-RC1",
  resolvers ++= Seq(
    "Typesafe Releases" at "https://repo.typesafe.com/typesafe/ivy-releases/",
    "Sonatype Release"  at "https://oss.sonatype.org/content/repositories/releases/",
    "IxiaS Releases"    at "https://s3-ap-northeast-1.amazonaws.com/maven.ixias.net/releases",
    "IxiaS Snapshots"   at "https://s3-ap-northeast-1.amazonaws.com/maven.ixias.net/snapshots"
  ),
  scalacOptions ++= Seq(
    "-source:future",   // Warns about features deprecated in future language versions.
    "-feature",         // Warn on usages of features that should be imported explicitly.
    "-unchecked",       // Warn on unchecked type operations (generics, etc.).
    "-deprecation",     // Warn when deprecated APIs are used.
    "-Wunused:all",     // Warn on unused code (imports, locals, etc.).
    "-Wconf:any:e",     // Treat all warnings as errors.
    "-Werror"           // Fail the build on any warning.
  ),
  libraryDependencies ++= Seq(
    "net.ixias"    %% "ixias"        % ixiasVersion,
    "javax.inject"  % "javax.inject" % "1",
  ),
  Test / fork := true,
  Compile / run / fork := true,
  // Skip the javadoc jar on publishLocal. Scaladoc re-processes ixias' inherited
  // doc comments and warns on every `[[...]]` link it cannot resolve (types living
  // in the ixias binary dependency, Java/3rd-party types). The artifact has no
  // value for an internally published library.
  Compile / packageDoc / publishArtifact := false,
)

// app-core: domain model + ixias persistence (EntityModel / SlickTable / Repository).
// Add sibling libraries here as the domain grows (framework/app-xxx) and
// aggregate them below.
lazy val appCore = (project in file("framework/app-core"))
  .settings(name := "app-core")
  .settings(commonSettings*)

// Meta package aggregating the framework libraries. app-api depends on THIS
// artifact (published locally with `sbt publishLocal`).
lazy val lib = (project in file("."))
  .settings(name := "education-book-app-lib")
  .settings(commonSettings*)
  .aggregate(appCore)
  .dependsOn(appCore)
