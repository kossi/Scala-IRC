package org.conbere.irc.perf

import org.conbere.irc.PEGParser
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try
import java.util.concurrent.TimeUnit

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
  val avg = 0 to iter*100
  val rndFill = 100
  val rnd = Seq.fill(rndFill)(scala.util.Random.nextInt(1624))

  // PEG Parboiled 2

  println(s"*** PEGP2 - ${lines.size} msgs ${iter/1000}k iterations ***")
  val test = time{
    (1 to iter).foreach(_ => lines.par.foreach( m => PEGParser(m).get))
  }
  println(s"# Elapsed time: ${test._2.toMillis} ms,  msgs/sec: "+
    f"${(iter * lines.size)/test._2.toUnit(TimeUnit.SECONDS)}%.0f")

  println("# PING command avg parse time: "+
    f"${time{avg.foreach(_ => PEGParser(lines(4)).get)
    }._2.toUnit(TimeUnit.MILLISECONDS)/avg.size.toDouble}%.5f ms")

  println(s"# PRIVMSG length of ${lines(98).length} avg parse time: "+
    f"${time{avg.foreach(_ => PEGParser(lines(98)).get)
    }._2.toUnit(TimeUnit.MILLISECONDS)/avg.size.toDouble}%.5f ms")


  val r100time =  avg.map{i =>
    val line = lines(rnd(i - ((i/rndFill)*rndFill)))
    time(PEGParser(line).get)._2.toNanos
  }.sum

  println("# Random100 avg parse time: "+
    f"${Duration(r100time, "nanos").toUnit(TimeUnit.MILLISECONDS)/avg.size.toDouble}%.5f ms")


  // REGEXP

  println(s"*** REGEX - ${lines.size} msgs ${iter/1000}k iterations ***")
  val test2 = time{
    (1 to iter).foreach(_ => lines.par.foreach(m => Parser(m).get))
  }

  println(s"# Elapsed time: ${test2._2.toMillis} ms, msgs/sec: "+
    f"${(iter * lines.size)/test2._2.toUnit(TimeUnit.SECONDS)}%.0f")

  println("# PING command avg parse time: "+
    f"${time{avg.foreach(_ => Parser(lines(4)).get)
    }._2.toUnit(TimeUnit.MILLISECONDS)/avg.size.toDouble}%.5f ms")

  println(s"# PRIVMSG length of ${lines(98).length} avg parse time: "+
    f"${time{avg.foreach(_ => Parser(lines(98)).get)
    }._2.toUnit(TimeUnit.MILLISECONDS)/avg.size.toDouble}%.5f ms")


  val r100time2 =  avg.map{i =>
    val line = lines(rnd(i - ((i/rndFill)*rndFill)))
    time(Parser(line).get)._2.toNanos
  }.sum

  println("# Random100 avg parse time: "+
    f"${Duration(r100time2, "nanos").toUnit(TimeUnit.MILLISECONDS)/avg.size.toDouble}%.5f ms")

  val cmds = for(i <- 0 until rndFill) yield PEGParser(lines(rnd(i))).get.command
  println("# Random100 commands were: "+
    s"${cmds.groupBy(c => c.value).map(t => (t._1, t._2.length)).mkString(", ")}")


  sys.exit()

}
