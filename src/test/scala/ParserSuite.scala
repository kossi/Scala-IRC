import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.conbere.irc.PEGParser
import org.conbere.irc.Tokens._

import scala.util.Success

@RunWith(classOf[JUnitRunner])
class ParserSuite extends FunSuite {
  val messages =
    List( "PASS secretpasswordhere"
        , "NICK Wiz"
        , ":WiZ NICK Kilroy"
        , "USER guest tolmoon tolsun :Ronnie Reagan"
        , ":testnick USER guest tolmoon tolsun :Ronnie Reagan"
        , "SERVER test.oulu.fi 1 :[tolsun.oulu.fi] Experimental server"
        , ":tolsun.oulu.fi SERVER csd.bu.edu 5 :BU Central Server"
        , "OPER foo bar"
        , "QUIT :Gone to have lunch"
        , "SQUIT tolsun.oulu.fi :Bad Link ?"
        , ":Trillian SQUIT cm22.eng.umd.edu :Server out of control"
        , "JOIN #foobar"
        , "JOIN &foo fubar"
        , "JOIN #foo,&bar fubar"
        , "JOIN #foo,#bar fubar,foobar"
        , "JOIN #foo,#bar"
        , ":WiZ JOIN #Twilight_zone"
        , "PART #twilight_zone"
        , "PART #oz-ops,&group5"
        , "MODE #Finnish +im"
        , "MODE #Finnish +o Kilroy"
        , "MODE #Finnish +v Wiz"
        , "MODE #Fins -s"
        , "MODE #42 +k oulu"
        , "MODE #eu-opers +l 10"
        , "MODE &oulu +b"
        , "MODE &oulu +b *!*@*"
        , "MODE &oulu +b *!*@*.edu"
        , ":MODE WiZ -w"
        , ":Angel MODE Angel +i"
        , "MODE WiZ -o"
        , ":Wiz TOPIC #test :New topic"
        , "TOPIC #test :another topic"
        , "TOPIC #test"
        , "NAMES #twilight_zone,#42"
        , "NAMES"
        , "LIST"
        , "LIST #twilight_zone,#42"
        , ":Angel INVITE Wiz #Dust"
        , "INVITE Wiz #Twilight_Zone"
        , "KICK &Melbourne Matthew"
        , "KICK #Finnish John :Speaking English"
        , ":WiZ KICK #Finnish John"
        , ":Wiz VERSION *.se"
        , "VERSION tolsun.oulu.fi"
        , ":Wiz STATS c eff.org"
        , "STATS m"
        , "LINKS *.au"
        , ":WiZ LINKS *.bu.edu *.edu"
        , "TIME tolsun.oulu.fi"
        , "Angel TIME *.au"
        , "CONNECT tolsun.oulu.fi"
        , ":WiZ CONNECT eff.org 6667 csd.bu.edu"
        , "TRACE *.oulu.fi"
        , ":WiZ TRACE AngelDust"
        , "ADMIN tolsun.oulu.fi"
        , ":WiZ ADMIN *.edu"
        , "KILL David (csd.bu.edu <- tolsun.oulu.fi)"
        )


  test("can parse") {
    messages.foreach { m =>
      assert(PEGParser(m) match {
        case Success(_) => true
        case _ => false
      }, "Failed to parse: " + m)
    }
  }

  test("can parse messages from the server with a prefix") {
    val message = ":irc02.test.server.com 461 aconbot JOIN :Not enough parameters"
    assert(PEGParser(message) match {
      case Success(Message(Some(Prefix("irc02.test.server.com", None, None)),
        Command("461"),
        List("aconbot",
        "JOIN",
        "Not enough parameters"))) =>
        true
      case parse =>
        println(parse)
        false
    }, "Failed to parse: " + message)
  }

  test("can parse commands correctly") {
    val stringCommand = "JOIN"
    val numericCommand = "461"

    assert(new PEGParser(stringCommand).Command.run() match {
      case Success(Command("JOIN")) =>
        true
      case _ =>
        false
    }, "Failed to parse command: " + stringCommand)

    assert(new PEGParser(numericCommand).Command.run() match {
      case Success(Command("461")) =>
        true
      case _ =>
        false
    }, "Failed to parse command: " + numericCommand)
  }

  test("can parse prefixes correctly") {
    val server = "irc02.test.server.com"
    val nick = "username!username@756.455.45.45"

    assert(new PEGParser(server).Prefix.run() match {
      case Success(Prefix("irc02.test.server.com", None, None)) =>
        true
      case parse =>
        println(parse)
        false
    }, "Failed to parse server prefix: " + server)

    assert(new PEGParser(nick).Prefix.run() match {
      case Success(Prefix("username", Some("username"), Some("756.455.45.45"))) =>
        true
      case parse =>
        println(parse)
        false
    }, "Failed to parse nick prefix: " + nick)

  }

  test("can parse underscore usernames") {
    val prefix = "aconbere_!aconbere@172.16.18.94"
    assert(new PEGParser(prefix).Prefix.run() match {
      case Success(Prefix("aconbere_", Some("aconbere"), Some("172.16.18.94"))) =>
        true
      case parse =>
        println(parse)
        false
    }, "failed to parser message with underscore: " + prefix)

  }

  test("can parse usernames with tilde"){
    val prefix = "testi!~testi@localhost"


    assert(new PEGParser(prefix).Prefix.run() match {
      case Success(Prefix("testi", Some("~testi"), Some("localhost"))) =>
        true
      case parse =>
        println(parse)
        false
    }, "failed to parser message with tilde in username: " + prefix)
  }

}
