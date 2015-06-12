package dashboard.server


//import com.fasterxml.jackson.annotation.JsonValue
//import org.joda.time.DateTime

import dashboard.model._
import shapeless.{HNil, HList}
import spray.http.HttpHeaders.{Host, `Access-Control-Allow-Origin`}
import spray.httpx.marshalling.Marshaller
import spray.routing._

import scala.concurrent.duration._
import akka.util.Timeout
import akka.actor._
import spray.can.Http
import spray.can.server.Stats
import spray.http._
import MediaTypes._
import spray.json._


/**
 * @author Nader Albert
 * @since  20/05/2015.
 */

case class DashboardRequest(method: HttpMethod, path: PathMatcher[HNil]) //path shouldn't be represented as a String

case class DashboardResponse(body: HttpEntity, code: StatusCode)

object DashboardHandler {

  import scala.languageFeature.implicitConversions._
}

trait DashboardHandler extends HttpServiceActor with ActorLogging {

  //self: Set =>

  implicit val timeout: Timeout = 1.second // for the actor 'asks'
  import context.dispatcher // ExecutionContext for the futures and scheduler

  def jsonValue(timeTable: TimeTableResult):JsValue = JsObject(Map.empty[String, JsValue] updated
    /*("bundleId", JsString(timeTable.bundleId.toString)) updated*/ ("time", JsString(timeTable.result)))

  implicit val timeTableWriter = JsonWriter.func2Writer(jsonValue)

  implicit val timeTableLoadingWriter = JsonWriter.func2Writer(TimeTableLoading.writer)
  implicit val timeTableReseedingStatusWriter = JsonWriter.func2Writer(ReseedingStatus.writer)

  def reactions: Set[(DashboardRequest,DashboardResponse)]

  /*
  override def receive: Receive = runRoute {

    path("dashboard" / "home"){
      complete{
        toHTTPResponse(DashboardResponse(HttpEntity("dashboard home"), StatusCodes.OK))
      }
    } ~ pathPrefix("dashboard" / "dashboard" / "timetable") {
          path("status") {
            get {
              parameters('result.as[String] /*, 'time ?*/).as(TimeTableResult) { timeTableResult =>
                complete {
                  timeTableResult match {
                    case TimeTableResult(result) if result.equals("successful") =>
                      toHTTPResponse(
                        DashboardResponse(
                          HttpEntity(
                            TimeTableReseed(100L, SuccessfulReseed(startedAt=DateTime.now, finishedAt = None, successfulTripCount=9, failedTripCount=0)).toJson.toString),
                          StatusCodes.OK))

                    case TimeTableResult(result) if result.equals("failing") =>
                      toHTTPResponse(
                        DashboardResponse(
                          HttpEntity(
                            TimeTableReseed(200L, FailingReseed(startedAt=DateTime.now, finishedAt = None, successfulTripCount=9, failedTripCount=10)).toJson.toString),
                          StatusCodes.OK))

                    case TimeTableResult(result) if result.equals("progress") =>
                      toHTTPResponse(
                        DashboardResponse(
                          HttpEntity(
                            TimeTableReseed(300L, InProgressReseed(startedAt=DateTime.now, finishedAt = None, successfulTripCount=2509, failedTripCount=10)).toJson.toString),
                          StatusCodes.OK))
                    case _ => toHTTPResponse(DashboardResponse(HttpEntity("invalid query parameter"), StatusCodes.NoContent))
                  }
                }
              }
            }
          }
    } ~ pathPrefix("tcdc" / "dashboard" / "system"){
          path("health"){
            get{
              complete{
                "system health !"
              }
            }
          } ~
          path("cpu" / IntNumber){ number => //path param case
            get{
              complete{
                "cpu status" + number
              }
            }
          } ~
          path("memory"){ //TESTING JSON
            get{
              complete {
                val source = """{ "some": "JSON parsed successfully" }"""
                val jsonAst = source.parseJson // or JsonParser(source)
                "memory status" + jsonAst
              }
            }
        }
      //TODO: HANDLE POST case with JSON // Use spray-json
    }
  }*/

  override def receive: Receive = runRoute {
    reactions.map(reaction =>  path(reactions.head._1.path){
        complete {
          toHTTPResponse(reactions.head._2)
        }
      }).fold(path("dashboard" / "home"){
          complete{
            toHTTPResponse(DashboardResponse(HttpEntity("dashboard home"), StatusCodes.OK))
          }
      }) ((accumulatedRoute, currentRoute) => accumulatedRoute ~ currentRoute)
  }

  def toHTTPResponse(dashboardResponse: DashboardResponse): HttpResponse =
    HttpResponse(dashboardResponse.code, dashboardResponse.body,
      List(`Access-Control-Allow-Origin`(SomeOrigins(List(HttpOrigin("http", Host("localhost", 8080)))))))

  implicit val timeTableResult: Marshaller[TimeTableResult] =
    Marshaller.delegate[TimeTableResult, String](ContentTypes.`text/plain`) { timeTable =>
      "result                : " + timeTable.result //+ '\n' +
      //"Total requests        : " + timeTable.time + '\n'
    }

  case class TimeTableResult(/*bundleId: Int,*/ result: String)


  /*def receive = {
    // when a new connection comes in we register ourselves as the connection handler
    case _: Http.Connected => sender ! Http.Register(self)

    case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
      sender ! index

    case HttpRequest(GET, Uri.Path(""), _, _, _) =>
      sender ! HttpResponse(entity = "Hi! I am he new TCDC dashboard!")

    /*case HttpRequest(GET, Uri.Path("/tcdc/dashboard/dtt"), _, _, _) => {
      val dailyTimeTableResource = context.actorOf(Props(new DailyTimeTable(sender)))

      dailyTimeTableResource ! DailyTimeTableHealth(DateTime.now)
    }
    */

    case HttpRequest(GET, Uri.Path("/tcdc/dashboard/dtt/"), _, _, _) => {
      val dailyTimeTableResource = context.actorOf(Props(new DailyTimeTable))

      dailyTimeTableResource ! DailyTimeTableHealth(DateTime.now)
    }

    case HttpRequest(GET, Uri.Path("/stream"), _, _, _) =>
      val peer = sender // since the Props creator is executed asyncly we need to save the sender ref
      context actorOf Props(new Streamer(peer, 25))

    case HttpRequest(GET, Uri.Path("/server-stats"), _, _, _) =>
      val client = sender
      context.actorFor("/user/IO-HTTP/listener-0") ? Http.GetStats onSuccess {
        case x: Stats => client ! statsPresentation(x)
      }

    case HttpRequest(GET, Uri.Path("/crash"), _, _, _) =>
      sender ! HttpResponse(entity = "About to throw an exception in the request handling actor, " +
        "which triggers an actor restart")
      sys.error("BOOM!")

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/timeout" =>
      log.info("Dropping request, triggering a timeout")

    case HttpRequest(GET, Uri.Path("/stop"), _, _, _) =>
      sender ! HttpResponse(entity = "Shutting down in 1 second ...")
      sender ! Http.Close
      context.system.scheduler.scheduleOnce(1.second) { context.system.shutdown() }

    /*case r@HttpRequest(POST, Uri.Path("/file-upload"), headers, entity: HttpEntity.NonEmpty, protocol) =>
      // emulate chunked behavior for POST requests to this path
      val parts = r.asPartStream()
      val client = sender
      val handler = context.actorOf(Props(new FileUploadHandler(client, parts.head.asInstanceOf[ChunkedRequestStart])))
      parts.tail.foreach(handler !)*/

    /*case s@ChunkedRequestStart(HttpRequest(POST, Uri.Path("/file-upload"), _, _, _)) =>
      val client = sender
      val handler = context.actorOf(Props(new FileUploadHandler(client, s)))
      sender ! RegisterChunkHandler(handler)*/

    case _: HttpRequest => sender ! HttpResponse(status = 404, entity = "Unknown resource!")

    case Timedout(HttpRequest(_, Uri.Path("/timeout/timeout"), _, _, _)) =>
      log.info("Dropping Timeout message")

    case Timedout(HttpRequest(method, uri, _, _, _)) =>
      sender ! HttpResponse(
        status = 500,
        entity = "The " + method + " request to '" + uri + "' has timed out..."
      )
}*/

  ////////////// helpers //////////////

  lazy val index = HttpResponse(
    entity = HttpEntity(`text/html`,
      /*<html>
        <body>
          <h1>Say hello to <i>spray-can</i>!</h1>
          <p>Defined resources:</p>
          <ul>
            <li><a href="/ping">/ping</a></li>
            <li><a href="/stream">/stream</a></li>
            <li><a href="/server-stats">/server-stats</a></li>
            <li><a href="/crash">/crash</a></li>
            <li><a href="/timeout">/timeout</a></li>
            <li><a href="/timeout/timeout">/timeout/timeout</a></li>
            <li><a href="/stop">/stop</a></li>
          </ul>
          <p>Test file upload</p>
          <form action ="/file-upload" enctype="multipart/form-data" method="post">
            <input type="file" name="datafile" multiple=""></input>
            <br/>
            <input type="submit">Submit</input>
          </form>
        </body>
      </html>.toString()*/ "noha"
    )
  )

  def statsPresentation(s: Stats) = HttpResponse(
    entity = HttpEntity(`text/html`,
      /*<html>
        <body>
          <h1>HttpServer Stats</h1>
          <table>
            <tr><td>uptime:</td><td>{s.uptime.formatHMS}</td></tr>
            <tr><td>totalRequests:</td><td>{s.totalRequests}</td></tr>
            <tr><td>openRequests:</td><td>{s.openRequests}</td></tr>
            <tr><td>maxOpenRequests:</td><td>{s.maxOpenRequests}</td></tr>
            <tr><td>totalConnections:</td><td>{s.totalConnections}</td></tr>
            <tr><td>openConnections:</td><td>{s.openConnections}</td></tr>
            <tr><td>maxOpenConnections:</td><td>{s.maxOpenConnections}</td></tr>
            <tr><td>requestTimeouts:</td><td>{s.requestTimeouts}</td></tr>
          </table>
        </body>
      </html>.toString()*/"rami"
    )
  )

  /*class Streamer(client: ActorRef, count: Int) extends Actor with ActorLogging {
    log.debug("Starting streaming response ...")

    // we use the successful sending of a chunk as trigger for scheduling the next chunk
    client ! ChunkedResponseStart(HttpResponse(entity = " " * 2048)).withAck(Ok(count))

    def receive = {
      case Ok(0) =>
        log.info("Finalizing response stream ...")
        client ! MessageChunk("\nStopped...")
        client ! ChunkedMessageEnd
        context.stop(self)

      case Ok(remaining) =>
        log.info("Sending response chunk ...")
        context.system.scheduler.scheduleOnce(100 millis span) {
          client ! MessageChunk(DateTime.now.toIsoDateTimeString + ", ").withAck(Ok(remaining - 1))
        }

      case x: Http.ConnectionClosed =>
        log.info("Canceling response stream due to {} ...", x)
        context.stop(self)
    }

    // simple case class whose instances we use as send confirmation message for streaming chunks
    case class Ok(remaining: Int)
  }*/

}

case class DailyTimeTableHealth(dateTime: DateTime)



