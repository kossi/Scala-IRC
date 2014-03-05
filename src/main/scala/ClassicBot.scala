package org.conbere.irc

import Messages._
import akka.actor._

trait ClassicBot extends Bot {
  val serverName:String
  val nickName:String
  val userName:String
  val password:String
  val realName:String

  val hostName = java.net.InetAddress.getLocalHost.getHostName

  def onConnect:Receive =  {
    case Connected if !password.isEmpty =>
      sender !
        (Pass(password) + Nick(nickName) + User(userName, hostName, serverName, realName))
    case Connected if password.isEmpty =>
      sender ! (Nick(nickName) + User(userName, hostName, serverName, realName))
  }
}

