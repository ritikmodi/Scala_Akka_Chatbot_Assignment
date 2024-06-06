package demo2

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors


sealed trait ClientMessage
case class MessageReceived(from: String, message: String) extends ClientMessage
case class ClientList(clients: List[String]) extends ClientMessage

object ClientActor {
  def apply(): Behavior[ClientMessage] = Behaviors.receiveMessage {
    case MessageReceived(from, message) =>
      println(s"message $message from $from")
      Behaviors.same

    case ClientList(clients) =>
      println(s"Connected Clients : ${clients.mkString(", ")}")
      Behaviors.same
  }
}
