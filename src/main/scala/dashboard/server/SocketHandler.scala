package dashboard.server


import akka.actor._
import dashboard.config.SSLConfiguration
import spray.can.Http
import spray.can.websocket.{FrameCommandFailed, WebSocketServerWorker}
import spray.can.websocket.frame.{TextFrame, BinaryFrame}
import spray.http.HttpRequest
import spray.routing.HttpServiceActor

/**
 * @author nader albert
 * @since  3/06/2015.
 */

  object SimpleServer extends App with SSLConfiguration {

  final case class Push(msg: String)

  object WebSocketServer {
    def props() = Props(classOf[WebSocketServer])
  }

  class WebSocketServer extends Actor with ActorLogging {
    def receive = {
      // when a new connection comes in we register a WebSocketConnection actor as the per connection handler
      case Http.Connected(remoteAddress, localAddress) =>
        println("new socket connection received ! " + "remote address is: " + remoteAddress + "local address is: " + localAddress + "sender is: " + sender)
        val serverConnection = sender
        val conn = context.actorOf(SocketHandler.props(serverConnection))
        serverConnection ! Http.Register(conn)
    }
  }

  object SocketHandler {
    def props(serverConnection: ActorRef) = Props(classOf[SocketHandler], serverConnection)
  }

  class SocketHandler(val serverConnection: ActorRef) extends HttpServiceActor with WebSocketServerWorker {

    override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

    def businessLogic: Receive = {
      // just bounce frames back for Autobahn testsuite
      case x@(_: BinaryFrame | _: TextFrame) =>
        sender ! x

      case Push(msg) => send(TextFrame(msg))

      case x: FrameCommandFailed =>
        log.error("frame command failed", x)

      case x: HttpRequest => // do something
    }

    def businessLogicNoUpgrade: Receive = {
      implicit val refFactory: ActorRefFactory = context
      runRoute {
        getFromResourceDirectory("webapp")
      }
    }
  }
}

