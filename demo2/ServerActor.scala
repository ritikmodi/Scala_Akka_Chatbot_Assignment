package demo2

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

sealed trait ServerMessage
case class ClientConnected(name: String, clientActor: ActorRef[ClientMessage]) extends ServerMessage
case class Broadcast(message: String) extends ServerMessage
case class PrivateMessage(receiver: String, message: String) extends ServerMessage
case class ClientDisconnected(name: String) extends ServerMessage

object ServerActor {
  def apply(): Behavior[ServerMessage] = Behaviors.setup { context =>

    var clients = Map.empty[String, ActorRef[ClientMessage]]

    Behaviors.receiveMessage {
      case ClientConnected(name, clientActor) =>
        context.log.info(s"Client $name connected")
        clients += name -> clientActor
        clients.values.foreach(_ ! ClientList(clients.keys.toList))
        Behaviors.same

      case Broadcast(message) =>
        context.log.info(s"Broadcasting message $message to all")
        clients.values.foreach(_ ! MessageReceived("Server", message))
        Behaviors.same

      case PrivateMessage(receiver, message) =>
        context.log.info(s"Sending private message $message to $receiver")
        clients.get(receiver) match {
          case Some(client) => client ! MessageReceived("Server", message)
          case None => context.log.info(s"Client $receiver Not Found")
        }
        Behaviors.same

      case ClientDisconnected(name) =>
        context.log.info(s"Client $name disconnected...")
        clients -= name
        clients.values.foreach(_ ! ClientList(clients.keys.toList))
        Behaviors.same
    }
  }
}