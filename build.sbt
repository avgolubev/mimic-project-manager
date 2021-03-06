name := """mimic-project-manager"""

version := "1.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

organization := "avgolubev"

scalacOptions ++= Seq("-encoding", "utf8", "-Xfatal-warnings", "-deprecation", "-unchecked")

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

libraryDependencies ++= Seq(
    jdbc
  , cache
  , ws
  , "org.scalaj"             %% "scalaj-http" 	     % "2.3.0"
  , "com.typesafe.play"      %% "anorm"       	     % "2.5.2"
  , "jp.t2v"                 %% "play2-auth"  	     % "0.14.2"
  , "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % "test"
)


fork in run := false
offline := false
