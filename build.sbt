import sbt.Keys._

name := "lifecycle"

organization := "com.itv"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-feature", "-deprecation", "-Xfatal-warnings")

tutSettings

tutTargetDirectory := baseDirectory.value

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

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
