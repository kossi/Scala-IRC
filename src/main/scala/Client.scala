package org.conbere.irc

import java.net.InetSocketAddress

import com.typesafe.scalalogging.slf4j.LazyLogging
import model.Response

import scala.concurrent.duration._

import akka.actor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.Random


import akka.io.{Tcp, IO}
import akka.contrib.throttle.TimerBasedThrottler
import akka.contrib.throttle.Throttler._

sealed trait IrcEvent
case object Connected extends IrcEvent
case class Connect(port: Int) extends IrcEvent
case object Reconnect extends IrcEvent
case object Stop extends IrcEvent
case object Stopped extends IrcEvent


object Client {
  val defaultThrottlerProps =  Props(classOf[TimerBasedThrottler], 4 msgsPer 1.second)

  case class ClientSettings(
    serverName: String, ports: List[Int], responder: ActorRef,
    throttlerProps: Props = defaultThrottlerProps, charset: String = "UTF-8",
    maxTries: Int = 5, retryTick: FiniteDuration = 30 seconds, quitMsg: String = "bye..."
                             )


  def props(serverName: String, port: Int, responder: ActorRef) =
    Props(classOf[Client], ClientSettings(serverName, List(port), responder))
  def props(serverName: String, ports: List[Int], responder: ActorRef) =
    Props(classOf[Client], ClientSettings(serverName, ports, responder))
  def props(clientSettings: ClientSettings) =
    Props(classOf[Client], clientSettings)
}

class Client(settings: Client.ClientSettings) extends Actor with LazyLogging {
  var retry: Int = 1
  var handler: ActorRef = _

  import context.system

  override def preStart() {
    self ! Connect(settings.ports.head)
  }

  def receive = {
    case Connect(port) =>
      val address = new InetSocketAddress(settings.serverName, port)
      logger.info(s"Connecting to: $address")
      IO(Tcp) ! Tcp.Connect(address)
    case Tcp.CommandFailed(c: Tcp.Connect) =>
      logger.warn("Command failed")
      self ! Reconnect
    case c @ Tcp.Connected(remote, local) =>
      val connection = sender()
      handler = context.actorOf(Handler.props(remote, connection, settings.responder,
        settings.throttlerProps, settings.charset, settings.quitMsg))
      connection ! Tcp.Register(handler)
      context watch handler
      logger.info(s"Connected remote: $remote local port: ${local.getPort}")
      retry = 1
      context.become({
        case Tcp.CommandFailed(c: Tcp.Connect) =>
          logger.warn("Command failed")
          self ! Reconnect
        case Reconnect =>
          context.children.foreach{c =>
            context.unwatch(c)
            context.stop(c)
          }
          context.unbecome()
          self ! Reconnect
        case Stop =>
          if(context.children.isEmpty) sender() ! Stopped
          handler forward Stop
        case response: Response =>
          handler forward response
        case Stopped =>
          sender() ! Stopped
        case _ =>

      },discardOld = false)
    case Reconnect =>
      if(retry < settings.maxTries){
        if(context.children.nonEmpty){
          logger.info(s"Children still alive. Waiting 20s...")
          context.system.scheduler.scheduleOnce(20 seconds){
            retry += 1
            self ! Connect(Random.shuffle(settings.ports).head)
          }
        }
        context.system.scheduler.scheduleOnce(settings.retryTick){
          logger.info(s"retrying vol...$retry")
          retry += 1
          self ! Connect(Random.shuffle(settings.ports).head)
        }
      }
      else{
         throw ConnectionException.create("Could not connect and max tries exceeded. " +
          "Throwing an exception and restarting...")
      }
    case Stop =>
      sender () ! Stopped
    case _ =>
  }

  override def postStop(){
    context.children.foreach(context.stop)
  }

}
