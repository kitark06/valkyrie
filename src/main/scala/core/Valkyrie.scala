package core

import interfaces.StepBuilder
import model.{Field, FieldValidationResult, ValidationType, ValidationRule, ValkyrieProcessor}
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

  val fieldValidationMap = new mutable.HashMap[String, ListBuffer[ValidationRule]]()
  val globalValidations = new ListBuffer[ValidationRule]()

  def passJudgement(field: Field, validations: Seq[ValidationRule]): Boolean = evaluateValidation(field, validations).validationFailures.isEmpty

  def evaluateValidation(field: Field, validations: Seq[ValidationRule]): FieldValidationResult = {
    val validationFailures =
      validations filterNot
        (ValidationRule => ValidationRule.validationType
        match {

          case ValidationType.NOT_NULL => field.fieldValue != null
          case ValidationType.IS_NULL => field.fieldValue == null

          case ValidationType.IS_BLANK => field.fieldValue.isEmpty
          case ValidationType.IS_NOT_BLANK => field.fieldValue.nonEmpty

          case ValidationType.GREATER_THAN_INCLUSIVE => field.fieldValue.compareTo(ValidationRule.parameter.head) <= 0
          case ValidationType.LESS_THAN_INCLUSIVE => field.fieldValue.compareTo(ValidationRule.parameter.head) >= 0
          case ValidationType.IS_EQUAL_TO => field.fieldValue.equals(ValidationRule.parameter.head)
          case ValidationType.IS_NOT_EQUAL_TO => !field.fieldValue.equals(ValidationRule.parameter.head)
        })

    println(s"${field.fieldName} , ${field.fieldValue} +  $validationFailures")
    FieldValidationResult(field, validationFailures)
  }

  private def recorder(validations: ValidationRule*)(fieldNames: String*): Valkyrie = {
    if (fieldNames.isEmpty || (fieldNames.size == 1 & fieldNames.head.trim.isEmpty))
      globalValidations ++= validations
    else
      fieldNames foreach (fieldName => fieldValidationMap.getOrElseUpdate(fieldName, new mutable.ListBuffer[ValidationRule]()) ++= validations)

    this
  }

  // Methods in Step builder
  def notNull(fieldNames: String*): Valkyrie = recorder(ValidationRule(ValidationType.NOT_NULL))(fieldNames: _*)

  def isNull(fieldNames: String*): Valkyrie = recorder(ValidationRule(ValidationType.IS_NULL))(fieldNames: _*)

  def isBlank(fieldNames: String*): Valkyrie = recorder(ValidationRule(ValidationType.IS_BLANK))(fieldNames: _*)

  def isNotBlank(fieldNames: String*): Valkyrie = recorder(ValidationRule(ValidationType.IS_NOT_BLANK))(fieldNames: _*)

  def isBetweenRange(lowerInclusiveRange: String, upperInclusiveRange: String, fieldNames: String*): Valkyrie =
    recorder(ValidationRule(ValidationType.LESS_THAN_INCLUSIVE, lowerInclusiveRange), ValidationRule(ValidationType.GREATER_THAN_INCLUSIVE, upperInclusiveRange))(fieldNames: _*)

  def isEqualTo(value: String, fieldName: String): Valkyrie = recorder(ValidationRule(ValidationType.IS_EQUAL_TO, value))(fieldName)

  def isNotEqualTo(value: String, fieldName: String): Valkyrie = recorder(ValidationRule(ValidationType.IS_NOT_EQUAL_TO, value))(fieldName)
  // Methods in Step builder


  def build = valkyrieProcessor match {
    case ValkyrieProcessor.JSON => new JsonProcessor(this)
    //    case ValkyrieProcessor.BYTE_ARRAY => new JsonProcessor(this)
  }
}