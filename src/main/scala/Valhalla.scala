import core.Valkyrie
//import processor.{JsonInputProcessor, Operations}

object Valhalla {
  def main(args: Array[String]): Unit = {

    /*val valkyrie = core.Valkyrie
      .json
      .isNull()
      .notNull()
      .build
      .evaluate(" ")*/


    Valkyrie
      .json
      .isNull()
      .notNull()
      .build
      .evaluate(" ")

  }
}
