package processor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ArrayNode, ObjectNode, ValueNode}
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.{Configuration, JsonPath, Option}
import core.Valkyrie
import model.Validation

import scala.collection.JavaConversions._

class JsonProcessor(valkyrie: Valkyrie) {

  private val config: Configuration = Configuration.builder()
    .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
    .jsonProvider(new JacksonJsonNodeJsonProvider())
    .build()

  // TODO .. try to convert it into tail recursive
  // TODO .. Add optimizer so that in a single pass , we run all validations for a specific field [field specific + global]

  private def traverseJsonNode(node: AnyRef)(implicit validations: Seq[Validation]): Boolean = node match {
    case nodes: ArrayNode => nodes forall traverseJsonNode
    case node: ObjectNode => node.elements forall traverseJsonNode
    case node: ValueNode => valkyrie.passJudgement(node.asText)(validations)
    case default: JsonNode => throw new UnsupportedOperationException(s"NodeType ${default.getNodeType} is not supported yet.")
  }

  def evaluate(json: String): Boolean = {
    // TODO handle invalid json scenario
    val document = JsonPath.using(config).parse(json)
    val globalValidationResult: Boolean = traverseJsonNode(document.read("$"))(valkyrie.globalValidations)
    val perFieldValidationResult: Boolean = valkyrie.fieldValidationMap forall (x => traverseJsonNode(document.read(x._1))(x._2))
    perFieldValidationResult && globalValidationResult
  }

//  def getValue

}
