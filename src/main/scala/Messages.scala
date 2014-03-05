package org.conbere.irc

import Tokens._

object Messages {
  object Pong {
    def apply(to:String) =
      Message(None, Command("PONG"), List(to))

    def unapply(msg:Message) = {
      msg match {
        case Message(_,Command("PONG"), List(from)) =>
          Some(from)
        case _ =>
          None
      }
    }
  }

  object Ping {
    def apply(to:String) =
      Message(None, Command("PING"), List(to))

    def unapply(msg:Message) = {
      msg match {
        case Message(_,Command("PING"), List(from)) =>
          Some(from)
        case _ =>
          None
      }
    }
  }

  object PrivMsg {
    def apply(to:String, text:String) =
        Message(None, Command("PRIVMSG"), List(to, text))

    def unapply(msg:Message) = {
      msg match {
        case Message(Some(Prefix(from, _, _)), Command("PRIVMSG"), List(to, text)) =>
          Some((from, to, text))
        case _ =>
          None
      }
    }
  }

  object PrivateMsg {
    def apply(to:String, text:String) =
      Message(None, Command("PRIVMSG"), List(to, text))

    def unapply(msg:Message) = {
      msg match {
        case Message(Some(Prefix(from, user, host)), Command("PRIVMSG"), List(to, text)) =>
          Some((from, to, text, user, host))
        case _ =>
          None
      }
    }
  }

  object Mode {
    def apply() =
      Message(None, Command("MODE"), List())

    def unapply(msg:Message) = {
      msg match {
        case Message(_, Command("MODE"), params) =>
          Some(params)
        case _ =>
          None
      }
    }
  }

  object UserMode {
    def apply() =
      Message(None, Command("MODE"), List())

    def unapply(msg:Message) = {
      msg match {
        case Message(_, Command("MODE"), List(to, params)) =>
          Some(to, params)
        case _ =>
          None
      }
    }
  }
  object Quit{
    def apply(text: String) =
      Message(None, Command("QUIT"), List(s":$text"))
    def unapply(msg: Message) = {
      msg match{
        case Message(_, Command("QUIT"), List(from, text)) =>
          Some(from, text)
        case _ =>
          None
      }
    }
  }

  object NickInUse{
    def apply() =
      Message(None, Command("433"), List())
    def unapply(msg: Message) = {
      msg match{
        case Message(_, Command("433"), List(_, nick, text)) =>
          Some(nick, text)
        case _ =>
          None
      }
    }
  }

  object TopicReply{
    def apply() =
      Message(None, Command("332"), List())
    def unapply(msg: Message) = {
      msg match{
        case Message(_, Command("332"), List(_, room, text)) =>
          Some(room, text)
        case _ =>
          None
      }
    }
  }

  object NoTopicReply{
    def apply() =
      Message(None, Command("331"), List())
    def unapply(msg: Message) = {
      msg match{
        case Message(_, Command("331"), List(_, room, _)) =>
          Some(room)
        case _ =>
          None
      }
    }
  }

  object NamesReply{
    def apply() =
      Message(None, Command("353"), List())
    def unapply(msg: Message) = {
      msg match{
        case Message(_, Command("353"), List(_, _ , room, names)) =>
          Some(room, names.split(" "))
        case _ =>
          None
      }
    }
  }

  object EndOfNamesReply{
    def apply() =
      Message(None, Command("366"), List())
    def unapply(msg: Message) = {
      msg match{
        case Message(_, Command("366"), List(_, room, _)) =>
          Some(room)
        case _ =>
          None
      }
    }
  }

  object EndOfMotdReply{
    def apply() =
      Message(None, Command("376"), List())
    def unapply(msg: Message) = {
      msg match{
        case Message(_, Command("376"), List(_,text)) =>
          Some(text)
        case _ =>
          None
      }
    }
  }
  object NoMotdError{
    def apply() =
      Message(None, Command("422"), List())
    def unapply(msg: Message) = {
      msg match{
        case Message(_, Command("422"), List(_,text)) =>
          Some(text)
        case _ =>
          None
      }
    }
  }

  object Invite{
    def apply(to:String, room:String) =
      Message(None, Command("INVITE"), List(to, room))
    def unapply(msg: Message) = {
      msg match{
        case Message(Some(Prefix(from, _, _)), Command("INVITE"), List(_, room)) =>
          Some(from, room)
        case _ =>
          None
      }
    }

  }

  object Kick{
    def apply(room: String, target:String, text:String) =
      Message(None, Command("KICK"), List(room, target, text))

    def unapply(msg:Message) = {
      msg match {
        case Message(Some(Prefix(from, _, _)), Command("KICK"), List(room,nick, text)) =>
          Some((from, room, nick, text))
        case _ =>
          None
      }
    }
  }

  object Pass {
    def apply(password:String) =
      Message(None, Command("PASS"), List(password))

    def unapply(msg:Message) = {
      msg match {
        case Message(_, Command("PASS"), List(password)) =>
          Some(password)
        case _ =>
          None
      }
    }
  }

  object Nick {
    def apply(nick:String) =
      Message(None, Command("NICK"), List(nick))

    def unapply(msg:Message) = {
      msg match {
        case Message(_, Command("NICK"), List(nick)) =>
          Some(nick)
        case _ =>
          None
      }
    }
  }

  object User {
    def apply(userName:String, hostName:String, domainName:String, realName:String) =
      Message(None,
              Command("USER"),
              List(userName,
                   hostName, 
                   domainName,
                   realName))

    def unapply(msg:Message) = {
      msg match {
        case Message(_, Command("USER"), List(userName, hostName, domainName, realName)) =>
          Some((userName, hostName, domainName, realName))
        case _ =>
          None
      }
    }
  }

  object Join {
    def apply(rooms:List[Room]) = {
      // concat all the rooms with keys together and their keys
      val (roomsK:String, keys:String) = rooms.foldLeft(("", "")) {
        case (("", keys), Room(room, Some(key),_)) =>
          (room, key)
        case ((rooms, keys), Room(room, Some(key),_)) =>
          (rooms + "," + room, keys + "," + key)
        case (acc, _) =>
          acc
      }

      // concat all the rooms without keys together
      val roomsN = rooms.foldLeft(roomsK) {
        case ("", Room(room, None,_)) =>
          room
        case (acc, Room(room, None,_)) =>
          acc + "," + room
        case (acc, _) =>
          acc
      }

      val result = if (keys.isEmpty) {
        List(roomsN)
      } else {
        List(roomsN, keys)
      }

      Message(None, Command("JOIN"), result)
    }

    def unapply(msg:Message) = {
      msg match {
        case Message(_, Command("JOIN"), List(rooms, keys)) =>
          Some((rooms, keys))
        case _ =>
          None
      }
    }
  }

  object Part {
    def apply(channels:List[String]) =
      Message(None, Command("PART"), List(channels.mkString(",")))

    def unapply(msg:Message) = {
      msg match {
        case Message(_,Command("PART"), List(channels)) =>
          Some(channels)
        case _ =>
          None
      }
    }
  }

  object UserJoin {
    def apply(room: String) =
      Message(None, Command("JOIN"), List(room))
    def unapply(msg:Message) = {
      msg match {
        case Message(Some(Prefix(from, user, host)),Command("JOIN"), List(room)) =>
          Some(room, from, user, host)
        case _ =>
          None
      }
    }
  }
  object UserPart {
    def apply(room: String, text: String) =
      Message(None, Command("PART"), List(room, text))
    def unapply(msg:Message) = {
      msg match {
        case Message(Some(Prefix(from, user, host)),Command("PART"), List(room, text)) =>
          Some(room, text, from, user, host)
        case _ =>
          None
      }
    }
  }
}
