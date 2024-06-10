package ChatBot

import akka.actor.{Actor, Props}

case class MessageReceived(from: String, message: String)
case class ClientList(clients: List[String])
case class ClientNotification(message: String)

class ClientActor(name: String) extends Actor {
  override def receive: Receive = {
    case MessageReceived(from, message) =>
      println(s"$from: $message")

    case ClientList(clients) =>
      println(s"Connected clients: ${clients.mkString(", ")}")

    case ClientNotification(message) =>
      println(message)

    case msg =>
      println(s"Unknown message: $msg")
  }
}

object ClientActor {
  def props(name: String): Props = Props(new ClientActor(name))
}

