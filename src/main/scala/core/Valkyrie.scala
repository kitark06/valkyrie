package core

import interfaces.StepBuilder
import model.{OperationType, Validation}
import processor.JsonProcessor

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


/**
 * Null check
 * Range check
 * Presence check (?)
 *
 * field equal to // not equal to value
 * date comparison
 * isBlank
 *
 * Relative comparison check (a can't be greater than b) // custom comparator
 * collections min elements
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

/**
 * valk
 * .pseudoBuild // select between json and object validations
 * .notnull("a","b")
 * .isNull("c","d")
 * .isBetween("e","1","2") // isGreaterThan , isLessThan
 * .build
 *
 * Build will trigger rule optimizer .. How will you handle for all field validations
 *
 * extractors and evaluators abstract json vs object handling and prevent code duplication ..
 *
 * .filter( valk.eval(_) )
 *
 * input [JsonString/Object] --> valkRuleEngine will evaluate the input --> will emit boolean value
 * short ckt on first breach of validation
 * .
 */

// TODO: add logging

object Valkyrie {
  //  def usingClass[T] = new GenericCore

  def json: StepBuilder[JsonProcessor] = new JsonProcessor

  def passJudgement(nodeValue: String)(validations: Seq[Validation]): Boolean = {
    validations forall
      (validation => validation.operationType match {
        case OperationType.NOT_NULL => nodeValue.nonEmpty
        case OperationType.IS_NULL => nodeValue == null
        case OperationType.IS_BLANK => nodeValue.isEmpty
        case OperationType.GREATER_THAN_INCLUSIVE => nodeValue.compareTo(validation.parameter.head) <= 0
        case OperationType.LESS_THAN_INCLUSIVE => nodeValue.compareTo(validation.parameter.head) >= 0
      })
  }

  abstract class Operations[T] extends StepBuilder[T] {
    val fieldValidationMap = new mutable.HashMap[String, ListBuffer[Validation]]()
    val globalValidations = new ListBuffer[Validation]()


    override def notNull(fieldNames: String*): Operations[T] = {
      recorder(Validation(OperationType.NOT_NULL))(fieldNames: _*)
      this
    }

    override def isNull(fieldNames: String*): Operations[T] = {
      recorder(Validation(OperationType.IS_NULL))(fieldNames: _*)
      this
    }

    override def isBlank(fieldNames: String*): Operations[T] = {
      recorder(Validation(OperationType.IS_BLANK))(fieldNames: _*)
      this
    }

    override def isBetweenRange(lowerInclusiveRange: String, upperInclusiveRange: String, fieldNames: String*): Operations[T] = {
      recorder(Validation(OperationType.LESS_THAN_INCLUSIVE, lowerInclusiveRange),
        Validation(OperationType.GREATER_THAN_INCLUSIVE, upperInclusiveRange))(fieldNames: _*)
      this
    }

    private def recorder(validations: Validation*)(fieldNames: String*): Unit = {
      if (fieldNames.isEmpty || (fieldNames.size == 1 & fieldNames.head.trim.isEmpty))
        globalValidations ++= validations
      else
        fieldNames foreach (fieldName => fieldValidationMap.getOrElseUpdate(fieldName, new mutable.ListBuffer[Validation]()) ++= validations)
    }

    def build: T
  }
}

