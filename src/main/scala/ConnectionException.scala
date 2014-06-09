package org.conbere.irc

object ConnectionException{
  def create(msg: String) = new ConnectionException(msg)
  def create(msg: String, cause: Throwable) = new ConnectionException(msg).initCause(cause)
}
class ConnectionException(msg: String) extends RuntimeException(msg: String) {
}
