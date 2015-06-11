package au.com.pwc.tcdc.administration.dashboard.server

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import au.com.pwc.tcdc.administration.dashboard.server.SimpleServer.WebSocketServer
import spray.can.Http
import spray.can.server.UHttp

/**
 * @author Nader Albert
 * @since  20/05/2015.
 */
class DashboardServer extends MySslConfiguration {

  implicit val system = ActorSystem()

  // the handler actor replies to incoming HttpRequests
  val handler = system.actorOf(Props[DashboardHandler], name = "handler")

  //IO(Http) ! Http.Bind(handler, interface = "localhost", port = 8090)

  val webSocketServer = system.actorOf(WebSocketServer.props(), "socket-handler")

  val uhttpManager = IO(UHttp)

  uhttpManager ! Http.Bind(handler, interface = "localhost", port = 8090)

  uhttpManager ! Http.Bind(webSocketServer, "localhost", 3000)
}

object Main extends App with MySslConfiguration{
  implicit val system = ActorSystem()

  // the handler actor replies to incoming HttpRequests
  val httpHandler = system.actorOf(Props[DashboardHandler], name = "http-handler")

  //val handler = system.actorOf(Props[DailyTimeTable], name = "handler")

 // IO(Http) ! Http.Bind(httpHandler, interface = "localhost", port = 8090)

  val webSocketServer = system.actorOf(WebSocketServer.props(), "socket-handler")

  //IO(UHttp) ! Http.Bind(webSocketServer, "localhost", 3000)

  val uhttpManager = IO(UHttp)

  uhttpManager ! Http.Bind(httpHandler, interface = "localhost", port = 8090)
  uhttpManager ! Http.Bind(webSocketServer, "localhost", 3000)
}

