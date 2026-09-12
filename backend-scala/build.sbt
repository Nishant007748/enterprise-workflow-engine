name := "workflow-backend"
version := "1.0.0"
scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-actor-typed" % "1.0.2",
  "org.apache.pekko" %% "pekko-stream" % "1.0.2",
  "org.apache.pekko" %% "pekko-http" % "1.0.1",
  "org.apache.pekko" %% "pekko-http-spray-json" % "1.0.1",
  "com.typesafe.slick" %% "slick" % "3.4.1",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",
  "org.postgresql" % "postgresql" % "42.7.1",
  "ch.qos.logback" % "logback-classic" % "1.4.14"
)
