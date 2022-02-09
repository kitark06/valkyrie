package processor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node._
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.{Configuration, DocumentContext, JsonPath}
import core.Valkyrie
import model.{Field, FieldValidationResult, Outcome}

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.JavaConversions._
import scala.collection.mutable

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
  def nodeToFields(name: String, inputJsonNode: JsonNode, accumulator: mutable.LinearSeq[Field]): Seq[Field] = {
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

  def evaluate(json: String): Boolean = getIfValid(json).isValid

  def getIfValid(json: String): Outcome[DocumentContext] = {

    // TODO handle invalid json scenario
    val document: DocumentContext = JsonPath.using(config).parse(json)
    val children = document.json[JsonNode]()

    val globalValidationResult: Seq[FieldValidationResult] =
      if (valkyrie.globalValidations.nonEmpty) {
        val allNodes = nodeToFields("$", children, mutable.LinearSeq.empty[Field])
        allNodes.map(field => valkyrie.evaluateValidation(field, valkyrie.globalValidations))
      }
      else mutable.Seq.empty


    val perFieldValidationResult: Seq[FieldValidationResult] =
      valkyrie.fieldValidationMap.flatMap(x => {
        val fields = nodeToFields(x._1, document.read[JsonNode](x._1), mutable.LinearSeq.empty[Field])
        fields.map(field => valkyrie.evaluateValidation(field, x._2))
      }).toSeq

    val isGlobalValidationSuccessful = globalValidationResult.forall(_.validationFailures.isEmpty)
    val isPerFieldValidationSuccessful: Boolean = perFieldValidationResult.forall(_.validationFailures.isEmpty)

    val errors = globalValidationResult ++ perFieldValidationResult filter (_.validationFailures.nonEmpty)
    val payload = if (errors.isEmpty) Option(document) else Option.empty

    Outcome(isGlobalValidationSuccessful && isPerFieldValidationSuccessful, errors, payload)
  }
}