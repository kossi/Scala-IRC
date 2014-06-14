package org.conbere.irc.perf

import org.conbere.irc.PEGParser
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

object Main extends App{

  def time[R](block: => R): (R, FiniteDuration) = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    (result, Duration(t1 - t0, "nanos"))
  }

  val file = io.Source.fromInputStream(getClass.getResourceAsStream("/testi.log"), "UTF-8")

  val lines = file.getLines().toList
  file.close()

  val sampleMessage = ":protectglobal.uk.quakenet.org 001 testing673 :Welcome " +
    "to the QuakeNet IRC Network, testing673"

  (1 to 100).foreach{_ =>
    PEGParser(sampleMessage).get
    Parser(sampleMessage).get
  }


  val iter = if(args.isEmpty) 1000 else Try(args(0).toInt) getOrElse 1000

  println(s"*** PEGP2 - ${lines.size} msgs ${iter/1000}k iterations ***")
  val test = time{
    (1 to iter).foreach(_ => lines.par.foreach( m => PEGParser(m).get))
  }
  println(s"# Elapsed time: ${test._2.toMillis} ms,  msgs/sec: "+
    s"${(iter * lines.size)/test._2.toSeconds}")
  println(s"*** REGEX - ${lines.size} msgs ${iter/1000}k iterations ***")
  val test2 = time{
    (1 to iter).foreach(_ => lines.par.foreach(m => Parser(m).get))
  }

  println(s"# Elapsed time: ${test2._2.toMillis} ms, msgs/sec: "+
    s"${(iter * lines.size)/test2._2.toSeconds}")


  sys.exit()

}