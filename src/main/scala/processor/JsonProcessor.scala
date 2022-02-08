package processor

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.databind.node._
import com.jayway.jsonpath.internal.JsonContext
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.{Configuration, DocumentContext, JsonPath}
import core.Valkyrie
import model.{Field, FieldValidationResult, Outcome, ValidationRule}

import java.lang.reflect
import java.util.concurrent.atomic.AtomicInteger
import scala.:+
import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class JsonProcessor(valkyrie: Valkyrie) {

  private val config: Configuration = Configuration.builder()
    .options(com.jayway.jsonpath.Option.DEFAULT_PATH_LEAF_TO_NULL)
    .jsonProvider(new JacksonJsonNodeJsonProvider())
    .build()

  val objectNodeChildrenReflectionField: java.lang.reflect.Field = classOf[ObjectNode].getDeclaredField("_children")
  objectNodeChildrenReflectionField.setAccessible(true)

  val arrayNodeChildrenReflectionField: java.lang.reflect.Field = classOf[ArrayNode].getDeclaredField("_children")
  arrayNodeChildrenReflectionField.setAccessible(true)

  // TODO .. if global validations present, latch per field validations to global .. Add optimizer so that in a single pass , we run all validations for a specific field [field specific + global]
  // TODO .. try to convert it into tail recursive
  def nodeToFields(name: String, inputJsonNode: AnyRef, accumulator: mutable.LinearSeq[Field]): Seq[Field] = {
    inputJsonNode match {
      case arrayNode: ArrayNode => {
        val counter = new AtomicInteger(-1)
        arrayNodeChildrenReflectionField
          .get(arrayNode)
          .asInstanceOf[java.util.ArrayList[JsonNode]]
          .flatMap(x => nodeToFields(s"$name[${counter.incrementAndGet()}]", x, accumulator))
      }
      case objNode: ObjectNode => {
        objectNodeChildrenReflectionField
          .get(objNode).asInstanceOf[java.util.LinkedHashMap[String, JsonNode]]
          .flatMap(x => nodeToFields(name + "." + x._1, x._2, accumulator))
          .toSeq
      }
      case nullNode: NullNode => accumulator :+ Field(name, null)
      case valueNode: ValueNode => accumulator :+ Field(name, valueNode.asText())
      case default: JsonNode => throw new UnsupportedOperationException(s"NodeType ${default.getNodeType} is not supported yet.")
    }
  }

  def getIfValid(json: String): Option[DocumentContext] = {

    // TODO handle invalid json scenario
    val document = JsonPath.using(config).parse(json)
    val children = document.json().asInstanceOf[AnyRef]
    val allNodes = nodeToFields("$", children, mutable.LinearSeq.empty[Field])

    val globalValidationResult: Boolean = allNodes.forall(field => valkyrie.passJudgement(field, valkyrie.globalValidations))

    val perFieldValidationResult: Boolean =
      valkyrie
        .fieldValidationMap
        .forall(x => {
          val nodesInJsonPath = nodeToFields(x._1, document.read[JsonNode](x._1).asInstanceOf[AnyRef], mutable.LinearSeq.empty[Field]) //  getLeafValues(mutable.Queue(document.read(x._1)), accumulator)
          nodesInJsonPath.forall(field => valkyrie.passJudgement(field, x._2))
        })

    if (perFieldValidationResult && globalValidationResult) Option(document) else Option.empty
  }

  def evaluate(json: String): Boolean = getIfValid(json).isDefined


  /* def traceEvaluate(json: String): Outcome[String] = {

     val document = JsonPath.using(config).parse(json)

     val cartographer =
       JsonPath
         .using(config.addOptions(com.jayway.jsonpath.Option.AS_PATH_LIST))
         .parse(json)
         .read("$..*").asInstanceOf[JsonNode]
         .elements()

     def getPathAndValueIfLeafNode(path: String): Option[Field] =
       document.read(path).asInstanceOf[AnyRef] match {
         case nullNode: NullNode => println(Field(path, null)); Option(Field(path, null))
         case valueNode: ValueNode => Option(Field(path, valueNode.asText))
         case _ => Option.empty
       }

     val globalValidationOutcome: Seq[FieldValidationResult] =
       cartographer
         .map(_.asText)
         .map(getPathAndValueIfLeafNode)
         .filter(_.isDefined)
         .map(_.get)
         .map(valkyrie.evaluateValidation(_, valkyrie.globalValidations))
         .toSeq

     val globalValidationResult: Boolean = globalValidationOutcome.forall(x => x.validationFailures.isEmpty)


     val perFieldValidationOutcome: Seq[FieldValidationResult] = valkyrie.fieldValidationMap.map(x => valkyrie.evaluateValidation(Field(x._1, getVal(document, x)), x._2)).toSeq
     val perFieldValidationResult: Boolean = perFieldValidationOutcome.forall(x => x.validationFailures.isEmpty)

     val errors = globalValidationOutcome ++ perFieldValidationOutcome filter (_.validationFailures.nonEmpty)

     Outcome(globalValidationResult && perFieldValidationResult, errors, json)
   }

   private def getVal(document: DocumentContext, x: (String, ListBuffer[ValidationRule])) = {
     val a = document.read(x._1)
     println(a)
     a
   }*/
}
//  def getValue