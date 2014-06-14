package org.conbere.irc

import Messages._
import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps

import com.typesafe.scalalogging.slf4j.LazyLogging
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import scala.io.StdIn
import scala.util.{Failure, Success}

class ExampleBot(
  val serverName:String,
  val nickName:String,
  val userName:String,
  val password:String,
  val realName:String,
  val rooms:List[Room]
) extends ClassicBot with LazyLogging {

  nick = nickName

  def before:Receive = {
    case PrivMsg(from, `nickName`, text) =>
        sender ! PrivMsg(from, text)
  }

  def invite:Receive = {
    case Invite(from, room) =>
      sender ! Join(List(Room(room, None)))
  }

  def receive = onConnect orElse altDefaultHandler orElse
                before orElse invite
}


object Main extends App with LazyLogging {

  val rooms = List(Room("#blaa", None, autoJoin = true))

  val serverName = "irc.quakenet.org"
  val ports: List[Int] = 6667 :: (6660 until 6666).toList

  val system = ActorSystem("Irc")

  val botProps =
    Props(classOf[ExampleBot], serverName, "testing", "testing testing", "", "Test Bot", rooms)

  val bot = system.actorOf(botProps)

  val client = system.actorOf(Client.props(serverName, ports, bot), name = "client")

  import system.dispatcher
  implicit val timeout = Timeout(30 seconds)
  def exit(): Unit =
    client ? Stop onComplete{
      case _ =>
        system.shutdown()
        system.awaitTermination(5 seconds)
        sys.exit()
    }

  sys.addShutdownHook(exit())

  StdIn.readLine(s"Exit: press enter ...${System.getProperty("line.separator")}")
  exit()
}
