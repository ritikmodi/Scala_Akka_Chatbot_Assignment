package ChatBot

import akka.actor.{ActorSystem, Props, ActorRef}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.util.{Failure, Success}
import akka.http.scaladsl.server.Directives._

object Server {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("ChatServer")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    val serverActor = system.actorOf(ServerActor.props, "serverActor")

    val route = path("ws-chat" / Segment) { name =>
      handleWebSocketMessages(webSocketFlow(serverActor, name))
    }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)
    println(s"Server online at http://localhost:8080/ws-chat/{name}\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }

  def webSocketFlow(serverActor: ActorRef, name: String): Flow[Message, Message, Any] = {
    val incomingMessages: Sink[Message, Any] =
      Flow[Message].collect {
        case TextMessage.Strict(text) =>
          val Array(command, payload) = text.split(":", 2)
          command match {
            case "broadcast" => BroadcastMessage(payload, name)
            case "private" =>
              val Array(receiver, msg) = payload.split(":", 2)
              PrivateMessage(name, receiver, msg)
          }
      }.to(Sink.actorRef(serverActor, ClientDisconnected(name)))

    val outgoingMessages: Source[Message, ActorRef] =
      Source.actorRef[Any](10, OverflowStrategy.fail)
        .mapMaterializedValue { outActor =>
          serverActor ! ClientConnected(name, outActor)
          outActor
        }
        .collect {
          case MessageReceived(from, msg) => TextMessage(s"$from: $msg")
          case ClientList(clients) => TextMessage(s"Clients: ${clients.mkString(", ")}")
          case ClientNotification(notification) => TextMessage(notification)
        }

    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }
}
