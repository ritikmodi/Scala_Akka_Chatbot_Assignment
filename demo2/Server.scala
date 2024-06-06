package demo2

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.{handleWebSocketMessages, path}
import akka.http.scaladsl.server.Route
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.io.StdIn

object Server {

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem[ServerMessage] = ActorSystem(ServerActor(), "ChatServer")
    implicit val executionContext = system.executionContext

    val route: Route =
      path("ws-chat") {
        handleWebSocketMessages(webSocketFlow)
      }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)
    println(s"Server started at http://localhost:8080/ws-chat \n Press STOP to stop...")
    StdIn.readLine()
    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }

  def webSocketFlow(implicit system: ActorSystem[ServerMessage]): Flow[Message, Message, Any] = {
    val clientActor = system.systemActorOf(ClientActor(), "clientActor")

    val incomingMessages: Sink[Message, Any] =
      Flow[Message].collect {
        case TextMessage.Strict(text) =>
          val Array(messagetype, msg) = text.split(":", 2)
          messagetype match {
            case "broadcast" => Broadcast(msg)
            case "private" =>
              val Array(receiver, msg1) = msg.split(":", 2)
              PrivateMessage(receiver, msg1)
          }
      }.to(Sink.onComplete( _ =>
      system ! ClientDisconnected("client")))


    val outgoingMessages: Source[Message, Any] =
      Source.actorRef[ClientMessage](bufferSize = 10, OverflowStrategy.fail).mapMaterializedValue {
        outgoingActor =>
          outgoingActor
      }.map {
        case MessageReceived(from, msg) => TextMessage(s"message $msg from $from")
        case ClientList(clients) => TextMessage(s"Clients : ${clients.mkString(", ")}")
      }

    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }
}
