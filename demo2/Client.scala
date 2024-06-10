package ChatBot

import akka.actor.{ActorSystem, Props, ActorRef}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.util.{Failure, Success}
import scala.concurrent.duration._

object Client {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("ChatClient")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    println("Enter your name:")
    val name = StdIn.readLine()

    val clientActor = system.actorOf(ClientActor.props(name), s"clientActor_$name")

    val serverUri = s"ws://localhost:8080/ws-chat/$name"
    val outgoingMessages: Source[Message, ActorRef] = Source.actorRef[String](10, OverflowStrategy.fail)
      .mapMaterializedValue { outActor =>
        system.scheduler.scheduleAtFixedRate(2.seconds, 2.seconds)(() => {
          println("Choose an option: [1] Broadcast [2] Private Message [3] Exit")
          StdIn.readLine() match {
            case "1" =>
              println("Enter broadcast message:")
              val message = StdIn.readLine()
              outActor ! s"broadcast:$message"

            case "2" =>
              println("Enter recipient:")
              val recipient = StdIn.readLine()
              println("Enter private message:")
              val message = StdIn.readLine()
              outActor ! s"private:$recipient:$message"

            case "3" =>
              outActor ! "ClientDisconnected"
              system.terminate()
              return

            case _ =>
              println("Invalid option, try again.")
          }
        })(ec)
        outActor
      }
      .map { msg => TextMessage(msg) }

    val incomingMessages: Sink[Message, Any] = Sink.foreach {
      case TextMessage.Strict(text) => clientActor ! parseIncomingMessage(text)
      case _ =>
    }

    val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(serverUri))

    val (upgradeResponse, closed) =
      outgoingMessages
        .viaMat(webSocketFlow)(Keep.right)
        .toMat(incomingMessages)(Keep.both)
        .run()

    upgradeResponse.onComplete {
      case Success(upgrade) if upgrade.response.status == akka.http.scaladsl.model.StatusCodes.SwitchingProtocols =>
        println("Connected to chat server")

      case Success(upgrade) =>
        println(s"Connection failed: ${upgrade.response.status}")

      case Failure(exception) =>
        println(s"Connection failed: $exception")
    }

    //    closed.foreach(_ => system.terminate())
  }

  def parseIncomingMessage(text: String): Any = {
    if (text.startsWith("Clients: ")) {
      ClientList(text.stripPrefix("Clients: ").split(", ").toList)
    } else if (text.contains(": ")) {
      val Array(from, msg) = text.split(": ", 2)
      MessageReceived(from, msg)
    } else {
      ClientNotification(text)
    }
  }
}
