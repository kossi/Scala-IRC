package org.conbere.irc.model

import akka.util.ByteString

object ControlChars {
  val CR_LF = "\r\n"
  val CRLF = ByteString(CR_LF)
  val SP = ByteString(" ")
}
