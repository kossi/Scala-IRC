import org.conbere.irc.PEGParser
import org.conbere.irc.perf.Parser

import com.google.caliper.SimpleBenchmark

class ParserBenchmark extends SimpleBenchmark {
  var lines: List[String] = _

  override def setUp(){
    val file = io.Source.fromInputStream(getClass.getResourceAsStream("/testi.log"), "UTF-8")
    lines = file.getLines().toList
    file.close()
  }

  def timeRegexpParser(n: Int) =
    for (i <- 0 to n)
      lines.foreach(Parser(_).get)

  def timePEGP2Parser(n: Int) =
    for (i <- 0 to n)
      lines.foreach(PEGParser(_).get)

}
