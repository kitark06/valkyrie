import com.fasterxml.jackson.databind.ObjectMapper
import core.Valkyrie

import scala.io.Source
//import processor.{JsonInputProcessor, Operations}

object Valhalla {
  def main(args: Array[String]): Unit = {


    val simpleValidJson = getJsonContents("src/test/resources/valid/Simple.json")

    assert(Valkyrie.json.notNull().build.evaluate(simpleValidJson))

//    assert(Valkyrie.json.notNull()
//      .build.evaluate(simpleValidJson))

//    assert(Valkyrie.json.isNull("/work/date").build.evaluate(simpleValidJson))

//    val parsedInput = new ObjectMapper().path(simpleValidJson)
//    println(parsedInput.at("/work/date"))

  }

  def getJsonContents(filePath: String): String = {
    val file = Source.fromFile(filePath)
    val result = file.mkString
    file.close()
    result
  }
}
