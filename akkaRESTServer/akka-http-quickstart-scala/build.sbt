import sbt.Keys.scalaVersion

lazy val akkaHttpVersion = "10.2.7"
lazy val akkaVersion    = "2.6.17"
lazy val awsVersion = "1.12.122"
val scalaVersion = "2.13.4"


lazy val root = (project in file(".")).

  settings(
    inThisBuild(List(
      organization    := "com.example",
    )),
    name := "akka-http-quickstart-scala",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "com.typesafe.akka" %% "akka-actor"               % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster"             % akkaVersion,
      "org.apache.spark"  %% "spark-sql-kafka-0-10"     % "3.1.2",
      "org.apache.spark"  %% "spark-sql"     % "3.1.2",
      "org.apache.spark"  %% "spark-core"     % "3.1.2",
      "org.apache.spark" %% "spark-streaming-kafka-0-10" % "3.1.2",
      "org.apache.spark" %% "spark-streaming" % "3.1.2",
      "ch.qos.logback"    % "logback-classic"           % "1.2.7",
      "com.amazonaws" % "aws-java-sdk" % awsVersion,
      "javax.mail" % "mail" % "1.4.7",
      //"com.amazonaws" % "aws-java-sdk-ses" % "1.12.125",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.2.9"         % Test,
      "org.apache.spark"  %% "spark-sql-kafka-0-10"     % "3.1.2"         % Test,
      "org.apache.kafka"  % "kafka-clients"             % "2.8.0"


      //"org.apache.maven.plugins" % "maven-shade-plugin" % "3.2.4"

    )
  )

assembly/assemblyMergeStrategy := {
  case "reference.conf" => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}



assembly/assemblyJarName := "HTTPServer.jar"
