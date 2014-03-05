package org.conbere.irc

case class Room(name:String, key:Option[String], autoJoin: Boolean = false) {
  val hasKey = !key.isEmpty
}
