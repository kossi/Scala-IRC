package org.conbere.irc

import java.net.InetSocketAddress
import com.typesafe.scalalogging.log4j.Logging

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


  def props(serverName: String, port: Int, responder: ActorRef) =
    Props(classOf[Client], serverName, List(port), responder, defaultThrottlerProps, "UTF-8", 5, 30 seconds)
  def props(serverName: String, ports: List[Int], responder: ActorRef) =
    Props(classOf[Client], serverName, ports, responder, defaultThrottlerProps, "UTF-8", 5, 30 seconds)
  def props(serverName: String, ports: List[Int], responder: ActorRef, throttlerProps: Props) =
    Props(classOf[Client], serverName, ports, responder, defaultThrottlerProps, "UTF-8", 5, 30 seconds)
  def props(serverName: String, ports: List[Int], responder: ActorRef, throttlerProps: Props, charset: String,
             maxTries: Int, retryTick: FiniteDuration) = Props(classOf[Client], serverName, ports, responder,
             throttlerProps, charset, maxTries, retryTick)
}

class Client(serverName:String, ports:List[Int], responder:ActorRef, throttlerProps: Props,
              charset: String, maxTries: Int, retryTick: FiniteDuration) extends Actor with Logging {
  var retry = 1
  var handler: ActorRef = _

  import context.system

  override def preStart() {
    self ! Connect(ports.head)
  }

  def receive = {
    case Connect(port) =>
      val address = new InetSocketAddress(serverName, port)
      logger.info(s"Connecting to: $address")
      IO(Tcp) ! Tcp.Connect(address)
    case Tcp.CommandFailed(c: Tcp.Connect) =>
      logger.warn("Command failed")
      self ! Reconnect
    case c @ Tcp.Connected(remote, local) =>
      val connection = sender()
      handler = context.actorOf(Handler.props(remote, connection, responder, throttlerProps, charset))
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
          if(context.children.size == 0) sender() ! Stopped
          handler forward Stop
        case response: Response =>
          handler forward response
        case Stopped =>
          sender() ! Stopped
        case _ =>

      },discardOld = false)
    case Reconnect =>
      if(retry < maxTries && context.children.size == 0){
        context.system.scheduler.scheduleOnce(retryTick){
          logger.info(s"retrying vol...$retry")
          retry += 1
          self ! Connect(Random.shuffle(ports).head)
        }
      }
      else{
        throw new RuntimeException("Could not connect and max tries exceeded. " +
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
