package com.example
import com.example.logRegistry.ActionPerformed

//#json-formats
import spray.json.DefaultJsonProtocol

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._
  // JSon formatters for our custom messages
  implicit val logFormat = jsonFormat2(Log)
  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
//#json-formats
