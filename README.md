# Kafka-Spark-with-Akka-Actors
In this project I essentially made my pipleline such that the logFileGenerator creates log files (on the EC2 instance), then these are sent to the s3 Bucket.
The s3 bucket has a watcher that triggers a lambda function (my python script) which is just a glorified POST. I have an Akka HTTP Server that would have been
running already and then accpets this request which contains the name of the file recently generated and spawns an Actor (kafkaProducer) to process it. The actor then pulls the 
file from the s3 bucket and streams the information from the file into a kafka topic called "logMessages". The server on start up creates an Actor that runs
indefinitely called kafkaConsumer that constantly polls for messages on the topic. Once the messages arrive, it extracts the WARNS and ERRORS from these logs
and then sends it into another kafka topic called "extractedMessages". My Spark program which would already be running polls for messages from this topic
and then Aggregates the information and sends it via the Amazon SES Services to (well just me) but theoretically to all stakeholders.

How to run:
First, we start with the Spark program
Just move over the jar file assembled from my SparkProcessor scala program (explained how to move files in documentation). Install Spark as explained in documentation. Then finally run
YOUR_SPARK_HOME/bin/spark-submit \
  --class "SimpleApp" \
  --master local[4] \
  target/scala-2.12/Spark.jar

Then on a seperate terminal connected to the EC2, lets start the server up. Start up a kafka cluster on amazon aws with appropriate security access group. 
Move over the assemvled jar from assembling my akkaRESTServer program into preferably a seperate directory on your EC2. Now install kafka like explained in
documentation (wget the download, tar and then run the servers on different terminals). 
Execute the jar normally (java -jar HTTPServer.jar)

Finally, send the logGenerator generated from assembling my LogUploadProject scala program into the EC2 instance. 
Execute the jar (java -jar GeneratedFatJar.jar)

This will now generate log files and send to my s3, upon which my s3 activates the watcher which thereby starts the lambda that posts to the server we started, writes into 
the kafka topics via our Akka actors and finally our Spark program reads the stream of data, aggregates it and sends it to Stakeholders
  
