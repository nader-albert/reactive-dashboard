package example

import dashboard.model._
import dashboard.server._
import spray.http._
import spray.json.{JsonWriter, JsObject, JsValue}

import spray.json._

/**
 * @author nader albert
 * @since  11/06/2015.
 */
class MyDashboardHandler extends ResourceProvider{

  implicit val timeTableLoadingWriter = JsonWriter.func2Writer(TimeTableLoading.writer)

  override def reactions: Set[(ResourceRequest, (Map[String,String] => ResourceResponse) )] =
   Set(
     (ResourceRequest(HttpMethods.GET, "dashboard" / "home"), { param: Map[String, String] => {
      ResourceResponse(
        HttpEntity("Home Page!"),
        StatusCodes.OK)
     }})
   ,
     (ResourceRequest(HttpMethods.GET, "dashboard" / "timetable"), { param: Map[String, String] => {
       param.get("result").map {
         case "successful" =>
           ResourceResponse(
             HttpEntity(TimeTableReseed(200L, SuccessfulReseed(startedAt = DateTime.now, finishedAt = None,
               successfulTripCount = 4000, failedTripCount = 10)).toJson.toString),
             StatusCodes.OK)

         case "failing" => ResourceResponse(
           HttpEntity(TimeTableReseed(200L, FailingReseed(startedAt = DateTime.now, finishedAt = None,
             successfulTripCount = 9, failedTripCount = 10)).toJson.toString),
           StatusCodes.OK)

         case "progress" => ResourceResponse(
           HttpEntity(
             TimeTableReseed(300L, InProgressReseed(startedAt=DateTime.now, finishedAt = None,
               successfulTripCount=2509, failedTripCount=10)).toJson.toString),
           StatusCodes.OK)

         case _ => ResourceResponse(HttpEntity(""), StatusCodes.NotFound)

       } getOrElse ResourceResponse(HttpEntity("EMPTY !"), StatusCodes.NotFound)
     }})
  )
}

