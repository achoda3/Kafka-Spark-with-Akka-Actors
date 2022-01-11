import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark._
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Seconds, StreamingContext}
import com.typesafe.config.ConfigFactory
import javax.mail.{Message, Session}
import javax.mail.internet.{InternetAddress, MimeMessage}

// This class reads from the Kafka topic constantly while it runs
// It basically Streams in the data, and starts aggregating all the
// WARNS and ERRORS . Finally uses aws ses to give the stakeholders the aggregate
object Spark {
  def main(args: Array[String]): Unit = {
    val logger = CreateLogger(this.getClass)
    val config = ConfigFactory.load()
    val sparkConf = new SparkConf()
    sparkConf.setAppName("WordCountingApp")
    val ssc = new StreamingContext(sparkConf, Seconds(1))
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "localhost:9092,anotherhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "use_a_separate_group_id_for_each_stream",
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )
    val topics = Array("extractedMessages")
    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )

    val tuples = stream.map(record => (record.key, record.value))
    val justVal = tuples.map(x => (x._2,1))
    val sum = justVal.reduceByKey((i1, i2) => i1 + i2)
    val formatted = sum.map(_.productIterator.mkString(":"))

    // Replace sender@example.com with your "From" address.
    // This address must be verified.
    val FROM = "aryannjch@gmail.com"
    val FROMNAME = "Aryann Chodankar"

    // Replace recipient@example.com with a "To" address. If your account
    // is still in the sandbox, this address must be verified.
    val TO = "aryannjcho@gmail.com"

    // Replace smtp_username with your Amazon SES SMTP user name.

    val  SMTP_USERNAME = config.getString("my-app.SMTP-user")

    // Replace smtp_password with your Amazon SES SMTP password.
    val  SMTP_PASSWORD = config.getString("my-app.SMTP-password")

    // The name of the Configuration Set to use for this message.
    // If you comment out or remove this variable, you will also need to
    // comment out or remove the header below.
    val  CONFIGSET = "ConfigSet"

    // Amazon SES SMTP host name. This example uses the US West (Oregon) region.
    // See https://docs.aws.amazon.com/ses/latest/DeveloperGuide/regions.html#region-endpoints
    // for more information.
    val  HOST = "email-smtp.us-west-2.amazonaws.com"

    // The port you will connect to on the Amazon SES SMTP endpoint.
    val PORT = "587"

    val  SUBJECT = "Amazon SES test (SMTP interface accessed using Java)"

    val BODY = String.join(
      System.getProperty("line.separator"),
      "<h1>Amazon SES SMTP Email Test</h1>",
      "<p>This email was sent with Amazon SES using the ",
      "<a href='https://github.com/javaee/javamail'>Javamail Package</a>",
      " for <a href='https://www.java.com'>Java</a>."
    )
    formatted.foreachRDD { rdd =>
      rdd.foreachPartition { partition =>
        // Create a Properties object to contain connection configuration information.
        val props = System.getProperties()
        props.put("mail.transport.protocol", "smtp")
        props.put("mail.smtp.port", PORT)
        props.put("mail.smtp.starttls.enable", "true")
        props.put("mail.smtp.auth", "true")

        // Create a Session object to represent a mail session with the specified properties.
        val session = Session.getDefaultInstance(props)

        // Create a message with the specified information.
        val msg = new MimeMessage(session)
        msg.setFrom(new InternetAddress(FROM,FROMNAME))
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(TO))
        msg.setSubject(SUBJECT)
        val builder = StringBuilder.newBuilder
        builder.append(BODY)
        partition.foreach(record => builder.append(record))
        msg.setContent(builder.toString(),"text/html")

        // Add a configuration set header. Comment or delete the
        // next line if you are not using a configuration set
        msg.setHeader("X-SES-CONFIGURATION-SET", CONFIGSET)

        // Create a transport.
        val transport = session.getTransport()
        // Send the message.
        try
        {
          System.out.println("Sending...")

          // Connect to Amazon SES using the SMTP username and password you specified above.
          transport.connect(HOST, SMTP_USERNAME, SMTP_PASSWORD)

          // Send the email.
          transport.sendMessage(msg, msg.getAllRecipients())
          logger.info("Email sent!")
        }
        catch {
          case e:Exception =>
            e.printStackTrace()
            logger.info("The email was not sent.")
            logger.info("Error message: " + e.getMessage())
        }
        finally
        {
          // Close and terminate the connection.
          transport.close()
        }
      }
    }

  }
}
