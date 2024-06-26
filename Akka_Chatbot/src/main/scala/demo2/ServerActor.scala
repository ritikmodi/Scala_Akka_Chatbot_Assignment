package demo2

import akka.actor.{Actor, ActorRef, Props, Terminated}
import scala.collection.mutable

case class ClientConnected(name: String, clientActor: ActorRef)
case class BroadcastMessage(message: String, from: String)
case class PrivateMessage(from: String, to: String, message: String)
case class ClientDisconnected(name: String)

class ServerActor extends Actor {
  private val clients = mutable.Map.empty[String, ActorRef]

  override def receive: Receive = {
    case ClientConnected(name, clientActor) =>
      context.watch(clientActor)
      clients += (name -> clientActor)
      sendClientList()
      notifyClients(name, s"Client $name connected")
      println(s"Client $name connected")

    case BroadcastMessage(message, from) =>
      clients.values.foreach(_ ! MessageReceived(from, message))
      println(s"Broadcast message from $from: $message")

    case PrivateMessage(from, to, message) =>
      clients.get(to) match {
        case Some(client) =>
          client ! MessageReceived(from, message)
          println(s"Private message from $from to $to: $message")
        case None =>
          println(s"Client $to not found")
      }

    case ClientDisconnected(name) =>
      clients -= name
      sendClientList()
      notifyClients(name, s"Client $name disconnected")
      println(s"Client $name disconnected")

  }

  private def sendClientList(): Unit = {
    val clientList = clients.keys.toList
    clients.values.foreach(_ ! ClientList(clientList))
  }

  private def notifyClients(sender: String, notification: String): Unit = {
    for(name <- clients.keys) {
      if (sender != name) {
        clients.values.foreach(_ ! ClientNotification(notification))
      }
    }
  }
}

object ServerActor {
  def props: Props = Props[ServerActor]()
}
