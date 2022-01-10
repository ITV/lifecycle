import sbt.Keys._
import ReleaseTransformations._

name := "lifecycle"

organization := "com.itv"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10" % "test"

scalaVersion := "2.13.6"

crossScalaVersions := Seq("2.11.8", "2.12.1", "2.12.1")

scalacOptions ++= Seq("-feature", "-deprecation", "-Xfatal-warnings")

mdocOut := baseDirectory.value

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

releaseCrossBuild := true

releaseTagComment := s"Releasing ${(ThisBuild / version).value}"

releaseCommitMessage := s"Setting version to ${(ThisBuild / version).value}"

releasePublishArtifactsAction := PgpKeys.publishSigned.value

// For Travis CI - see http://www.cakesolutions.net/teamblogs/publishing-artefacts-to-oss-sonatype-nexus-using-sbt-and-travis-ci
credentials ++= (for {
  username <- Option(System.getenv().get("SONATYPE_USER"))
  password <- Option(System.getenv().get("SONATYPE_PASS"))
} yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq

pomExtra := (
  <url>https://github.com/ITV/lifecycle</url>
  <licenses>
    <license>
      <name>ITV-OSS</name>
      <url>http://itv.com/itv-oss-licence-v1.0</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:ITV/lifecycle.git</url>
    <connection>scm:git:git@github.com:ITV/lifecycle.git</connection>
  </scm>
  <developers>
    <developer>
      <id>jfwilson</id>
      <name>Jamie Wilson</name>
      <url>https://github.com/jfwilson</url>
    </developer>
    <developer>
      <id>mcarolan</id>
      <name>Martin Carolan</name>
      <url>https://mcarolan.net/</url>
      <organization>ITV</organization>
      <organizationUrl>http://www.itv.com</organizationUrl>
    </developer>
  </developers>
)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
	Some("snapshots" at nexus + "content/repositories/snapshots")
  else
	Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pgpPublicRing := file("./ci/public.asc")

pgpSecretRing := file("./ci/private.asc")

pgpSigningKey := Some("-5373332187933973712L")

pgpPassphrase := Option(System.getenv("GPG_KEY_PASSPHRASE")).map(_.toArray)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
)
