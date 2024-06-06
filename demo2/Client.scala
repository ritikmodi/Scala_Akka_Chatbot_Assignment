package demo2

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.io.StdIn
import scala.concurrent.duration._

object Client {

  def main(args: Array[String]): Unit = {

    implicit val Clientsystem: ActorSystem[ClientMessage] = ActorSystem(ClientActor(), "ChatClient")
    val Serversystem: ActorSystem[ServerMessage] = ActorSystem(ServerActor(), "ChatServer")
    implicit val executionContext = Clientsystem.executionContext
    implicit val materializer: Materializer = Materializer(Clientsystem)

    val serverUri = "ws://localhost:8080/ws-chat"

    val clientActor = Clientsystem.systemActorOf(ClientActor(), "clientActor")

    val incomingMessages: Sink[Message, Any] =
      Flow[Message].collect {
        case TextMessage.Strict(text) =>
          text
      }.to(Sink.onComplete( _ =>
        Clientsystem ! MessageReceived("Server", "Connection Closed")))

    val outgoingMessages: Source[Message, Any] =
      Source.actorRef[String](bufferSize = 10, OverflowStrategy.fail).mapMaterializedValue {
        outgoingActor =>
          println("Enter client name : ")
          val clientName = StdIn.readLine()
          val updatedClientName = clientName + "test"
          Serversystem ! ClientConnected(updatedClientName, clientActor)
          Clientsystem.scheduler.scheduleAtFixedRate(2.seconds, 2.seconds) { () =>
            println("Choose 1. Broadcast 2. Private 3. Exit")
            StdIn.readLine() match {
              case "1" =>
                println("Enter broadcast message : ")
                val message = StdIn.readLine()
                outgoingActor ! s"broadcast:$message"

              case "2" =>
                println("Enter receiver : ")
                val receiver = StdIn.readLine()
                println("Enter private message : ")
                val message = StdIn.readLine()
                outgoingActor ! s"private:$receiver:$message"

              case "3" =>
                Clientsystem.terminate()
                return

              case _ =>
                println("Invalid option")
            }
          }
          outgoingActor
      }.map { msg =>
        TextMessage(msg)
      }

    val webSocketFlow = Flow.fromSinkAndSource(incomingMessages, outgoingMessages)

    val (upgradeResponse, closed) =
      Http().singleWebSocketRequest(WebSocketRequest(serverUri), webSocketFlow)

    val connected = upgradeResponse.map { upgrade =>
      if (upgrade.response.status == akka.http.scaladsl.model.StatusCodes.SwitchingProtocols) {
        println("Connected to Server")
        Behaviors.empty
      }
      else {
        throw new RuntimeException(s"Connection failed : ${upgrade.response.status}")
      }
    }

    connected.onComplete(_ => println("Type messages to send to the Server, Type 'exit' to exit"))

    StdIn.readLine()
    Clientsystem.terminate()
  }
}
