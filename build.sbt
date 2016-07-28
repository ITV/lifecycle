import sbt.Keys._

name := "lifecycle"

organization := "itv"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-feature", "-deprecation", "-Xfatal-warnings")

