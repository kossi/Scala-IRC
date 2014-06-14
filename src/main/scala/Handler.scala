package org.conbere.irc

import akka.actor._
import akka.io.Tcp
import akka.util.{ByteStringBuilder, ByteString}
import java.net.InetSocketAddress

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.conbere.irc.ControlChars._
import org.conbere.irc.Messages.Quit
import akka.contrib.throttle.Throttler._
import scala.annotation.tailrec



object Handler {
  def props(remote: InetSocketAddress, connection: ActorRef, responder: ActorRef, throttlerProps: Props,
            charset: String) = Props(classOf[Handler], remote, connection, responder, throttlerProps, charset)
  val CR_LF = "\r\n"
}

class Handler(remote: InetSocketAddress, connection: ActorRef, responder:ActorRef,
               throttlerProps: Props,charset: String) extends Actor with LazyLogging {
  val hostName = java.net.InetAddress.getLocalHost.getHostName

  val throttler = context.actorOf(throttlerProps)


  import Tcp._
  import org.conbere.irc.Connected

  context.setReceiveTimeout(5 minutes)

  context.watch(connection)
  context.watch(throttler)

  var buffer: Option[String] = None


  def parseMessage(str:String) =
    PEGParser(str) match {
      case Success(message) =>
        logger.debug(s"Received: $message")
        Some(message)
      case _ =>
        logger.error(s"Could not parse: $str")
        None
    }

  def out(command: ByteString): ByteString =
    (new ByteStringBuilder ++= command ++= CRLF).result()

  override def preStart(){
    logger.info(s"Connection handler started for $remote")
    throttler ! SetTarget(Some(connection))
    responder ! Connected
  }

  @tailrec
  private def respondToInput(in: String){
    buffer match{
      case Some(bufferedInput) =>
        buffer = None
        respondToInput(bufferedInput + in)
      case None =>
        for(message <- parseMessage(in)) {
          responder ! message
        }
    }
  }


  def receive = {
    case response: Response =>
      logger.debug("send: "+response.toString)
      throttler !  Write(out(response.byteString))
    case CommandFailed(w: Write) =>
      logger.info(s"write failed: ${w.data.decodeString(charset)}")
    case Received(data) =>
      val input = data.decodeString(charset)
      val ins = input.split(Handler.CR_LF)

      handleInput(0)

      @tailrec def handleInput(index: Int){
        if(index < ins.length-1){
          respondToInput(ins(index))
          handleInput(index+1)
        }
        else{
          input takeRight 2 match{
            case Handler.CR_LF => respondToInput(ins(index))
            case _ => buffer = Some(ins(index))
          }
        }
      }
    case ReceiveTimeout =>
      context.setReceiveTimeout(Duration.Undefined)
      context.parent ! Reconnect
    case Terminated(`connection`) =>
      context.setReceiveTimeout(Duration.Undefined)
      context.parent ! Reconnect
    case Stop =>
      val reply = sender()
      connection ! Write(out(Quit("End is coming...").byteString))
      throttler ! SetTarget(None)
      context.setReceiveTimeout(20 seconds)
      context.children.foreach(context.unwatch)
      context.become({
          case closed: ConnectionClosed =>
            reply forward Stopped
          case ReceiveTimeout =>
            logger.warn("Disconnect timed out")
            context stop throttler
            context stop connection
            reply forward Stopped
          case _ => logger.info("Closing connection")

      }, discardOld = true)
    case closed: ConnectionClosed =>
      context.setReceiveTimeout(Duration.Undefined)
      logger.debug(s"$remote Handler closed")
      context.parent ! Reconnect
  }

  override def postStop(){
    context.setReceiveTimeout(Duration.Undefined)
    context.children.foreach(context.stop)
  }
}
