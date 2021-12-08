package core

import interfaces.StepBuilder
import model.{OperationType, Validation, ValkyrieProcessor}
import processor.JsonProcessor

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


/**
 * date comparison
 *
 * Relative comparison check (a can't be greater than b) // custom comparator
 * collections min elements
 *
 * validate against object
 *
 * Rule Optimizer
 * First process all presence checks (contains op)
 * Then run rules on group by fields
 *
 * Must haves (actions when they don't)
 * Could haves (separate actions when they don't)
 * Flink - failure rate actions (email, alert, crash)
 * Flink - failure rate over time / count / both
 * Flink - determine output sink based on outcome
 */

// TODO: add logging

object Valkyrie {

  //  def usingClass[T] = new GenericCore
  //  def byteArray[T] = new ByteArrayProcessor
  //  def json: StepBuilder[JsonProcessor] /*with InputProcessor[String,AnyRef]*/ = new JsonProcessor()

  def json: StepBuilder = new Valkyrie(ValkyrieProcessor.JSON)

  def byteArray: StepBuilder = new Valkyrie(ValkyrieProcessor.BYTE_ARRAY)

}

class Valkyrie private(valkyrieProcessor: ValkyrieProcessor.Value) extends StepBuilder {

  val fieldValidationMap = new mutable.HashMap[String, ListBuffer[Validation]]()
  val globalValidations = new ListBuffer[Validation]()

  def passJudgement(nodeValue: String)(validations: Seq[Validation]): Boolean = {
    println(s"Judging value :: $nodeValue against ->  ${validations.mkString(",")}")
    validations forall
      (validation => validation.operationType
      match {
        case OperationType.NOT_NULL => nodeValue.nonEmpty
        case OperationType.IS_NULL => nodeValue.equalsIgnoreCase("null")
        case OperationType.IS_BLANK => nodeValue.isEmpty
        case OperationType.GREATER_THAN_INCLUSIVE => nodeValue.compareTo(validation.parameter.head) <= 0
        case OperationType.LESS_THAN_INCLUSIVE => nodeValue.compareTo(validation.parameter.head) >= 0
        case OperationType.IS_EQUAL_TO => nodeValue.equals(validation.parameter.head)
        case OperationType.IS_NOT_EQUAL_TO => !nodeValue.equals(validation.parameter.head)
      })
  }

  private def recorder(validations: Validation*)(fieldNames: String*): Valkyrie = {
    if (fieldNames.isEmpty || (fieldNames.size == 1 & fieldNames.head.trim.isEmpty))
      globalValidations ++= validations
    else
      fieldNames foreach (fieldName => fieldValidationMap.getOrElseUpdate(fieldName, new mutable.ListBuffer[Validation]()) ++= validations)

    this
  }

  // Methods in Step builder
  def notNull(fieldNames: String*): Valkyrie = recorder(Validation(OperationType.NOT_NULL))(fieldNames: _*)

  def isNull(fieldNames: String*): Valkyrie = recorder(Validation(OperationType.IS_NULL))(fieldNames: _*)

  def isBlank(fieldNames: String*): Valkyrie = recorder(Validation(OperationType.IS_BLANK))(fieldNames: _*)

  def isBetweenRange(lowerInclusiveRange: String, upperInclusiveRange: String, fieldNames: String*): Valkyrie =
    recorder(Validation(OperationType.LESS_THAN_INCLUSIVE, lowerInclusiveRange), Validation(OperationType.GREATER_THAN_INCLUSIVE, upperInclusiveRange))(fieldNames: _*)

  def isEqualTo(value: String, fieldName: String): Valkyrie = recorder(Validation(OperationType.IS_EQUAL_TO, value))(fieldName)

  def isNotEqualTo(value: String, fieldName: String): Valkyrie = recorder(Validation(OperationType.IS_NOT_EQUAL_TO, value))(fieldName)
  // Methods in Step builder


  def build = valkyrieProcessor match {
    case ValkyrieProcessor.JSON => new JsonProcessor(this)
//    case ValkyrieProcessor.BYTE_ARRAY => new JsonProcessor(this)
  }
}


