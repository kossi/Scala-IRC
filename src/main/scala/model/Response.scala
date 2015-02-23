package org.conbere.irc.model

import akka.util.ByteString

trait Response {
  def +(r:Response):ResponseCollection
  val byteString:ByteString
}
