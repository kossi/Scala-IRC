package org.conbere.irc

import akka.actor._
import Messages._
import scala.util.Random

trait Bot extends Actor{
  // a list of channels to join
  val rooms:List[Room]

  var nick: String = _

  // some default behaviors that most bots will want to implement
  val defaultHandler:Receive = {
    case Ping(from) =>
      sender ! Pong(from)
    case Mode(params) if !rooms.isEmpty =>
      sender ! Join(rooms)
  }

  // For QNet and IRCnet
  val altDefaultHandler:Receive = {
    case EndOfMotdReply(text) if !rooms.isEmpty =>
      sender ! Join(rooms)
    case NoMotdError(text) if !rooms.isEmpty =>
      sender ! Join(rooms)
    case Ping(from) =>
      sender ! Pong(from)
    case NickInUse(_, text)=>
      println(text)
      nick = nick+Random.nextInt(999)
      sender ! Nick(nick)
  }
}
