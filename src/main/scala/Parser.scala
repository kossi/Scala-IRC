package org.conbere.irc
/*
<message>    ::= [':' <prefix> <SPACE> ] <command> <params> <crlf>
<prefix>     ::= <servername> | <nick> [ '!' <user> ] [ '@' <host> ]
<command>    ::= <letter> { <letter> } | <number> <number> <number>
<SPACE>      ::= ' ' { ' ' }
<params>     ::= <SPACE> [ ':' <trailing> | <middle> <params> ]
<middle>     ::= <Any *non-empty* sequence of octets not including SPACE or NUL or CR or LF, the first of which may not be ':'>
<trailing>   ::= <Any, possibly *empty*, sequence of octets not including NUL or CR or LF>
<crlf>       ::= CR LF

<target>     ::= <to> [ "," <target> ]
<to>         ::= <channel> | <user> '@' <servername> | <nick> | <mask>
<channel>    ::= ('#' | '&') <chstring>
<servername> ::= <host>
<host>       ::= see RFC 952 [DNS:4] for details on allowed hostnames
<nick>       ::= <letter> { <letter> | <number> | <special> }
<mask>       ::= ('#' | '$') <chstring>
<chstring>   ::= <any 8bit code except SPACE, BELL, NUL, CR, LF and comma (',')>

<user>       ::= <nonwhite> { <nonwhite> }
<letter>     ::= 'a' ... 'z' | 'A' ... 'Z'
<number>     ::= '0' ... '9'
<special>    ::= '-' | '[' | ']' | '\' | '`' | '^' | '{' | '}'
<nonwhite>   ::= <any 8bit code except SPACE (0x20), NUL (0x0), CR (0xd), and LF (0xa)>
*/

import scala.util.parsing.combinator.RegexParsers
import akka.util.{ ByteString, ByteStringBuilder }

sealed trait Token {
  import ControlChars._

  def outputString:String
  def toByteString() = {
    println("sending: " + outputString)
    (new ByteStringBuilder ++= ByteString(outputString) ++= CRLF).result
  }
}

sealed trait SimpleToken extends Token {
  val value:String
  def outputString = value
}

object Tokens {
  case class Channel(value:String) extends SimpleToken
  case class UserMask(value:String) extends SimpleToken
  case class Command(value:String) extends SimpleToken

  case class Prefix(target:String,
                    user:Option[String],
                    host:Option[String])
  extends Token {
    def outputString =
      List(target,
           user.getOrElse(""),
           host.getOrElse("")).mkString(" ")
  }

  case class Message(prefix:Option[Prefix],
                     command:Command,
                     params:List[String])
  extends Token {
    import ControlChars._
    // TODO: look into how params actually need to look
    // for the traling param
    def outputString = {
      val pre = prefix.map { p => p.outputString }.getOrElse("")

      pre + command.outputString + " " + params.mkString(" ")
    }
  }
}

object Parser extends RegexParsers {
  import Tokens._
  override def skipWhitespace = false

  def message:Parser[Message] =
    opt(":" ~> prefix <~ space) ~
    command ~
    params ^^ {
    case (prefix~command)~params =>
      Message(prefix, command, params)
  }

  def prefix:Parser[Prefix] =
    (serverName | nick ) ~ opt( '!' ~> user ) ~ opt( '@' ~> serverName ) ^^ {
    case t~u~s => Prefix(t, u, s)
  }

  def command:Parser[Command] =
    ("""[0-9]{3}""".r | word) ^^ (Command(_))

  def space = rep(' ')

  def params =
    space ~> opt(repsep(middle, ' ') ~ (space ~> opt(':' ~> trailing))) ^^ {
    case Some(result) =>
      result match {
        case ps~None => ps
        case ps~Some(tr) => ps :+ tr
      }
    case None =>
      List()
  }

  def middle = not(':') ~> """[^\s\r\n]+""".r
  def trailing = """[^\r\n]+""".r
  def crlf = """\r\n""".r

  def target:Parser[Any] = to ~ opt(',' ~ target)
  def to = channel | user ~ '@' ~ serverName | nick | mask
  def channel:Parser[Channel] =  """[#|&].+""".r ^^ (Channel(_))
  def serverName = host
  def host = """[a-zA-Z0-9.\-]+""".r

  def nick = """(\p{L}|[0-9]|[-\[\]\\`^\{\}])+""".r

  def mask:Parser[UserMask] =  """[#|$].+""".r ^^ (UserMask(_))
  def letter = """[a-zA-Z]""".r
  def user = nick
  def startsWithColon = """:.+""".r
  def word = """[a-zA-Z]*""".r
  def number = """[0-9]""".r
  def special = """[-\[\]\\`^\{\}]""".r

  def apply(input:String) = parseAll(message, input)
}