package core

import core.Valkyrie.Operations
import interfaces.{FinalBuilder, PreBuilder}
import model.{OperationType, Validation}
//import processor.{JsonInputProcessor, Operations}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Null check
 * Range check
 * Presence check (?)
 * Relative comparison check (a can't be greater than b) // custom comparator
 * Rule Optimizer
 * First process all presence checks (contains op)
 * Then run rules on group by fields
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

object Valkyrie {
  //  def usingClass[T] = new GenericCore

  def json: PreBuilder = new Operations

  class Operations extends PreBuilder with FinalBuilder {
    private[core] val fieldValidationMap = new mutable.HashMap[String, ListBuffer[Validation]]()
    private[core] val allFieldValidations = new mutable.HashSet[Validation]()

    def notNull(fieldNames: String*): FinalBuilder = {
      if (fieldNames.isEmpty || (fieldNames.size == 1 & fieldNames.head.trim.isEmpty))
        allFieldValidations.add(Validation(OperationType.NOT_NULL))
      else
        fieldNames.foreach(
          name => fieldValidationMap.getOrElseUpdate(name, new mutable.ListBuffer[Validation]()) += Validation(OperationType.NOT_NULL))

      this
    }

    def isNull(fieldNames: String*): FinalBuilder = {
      if (fieldNames.isEmpty || (fieldNames.size == 1 & fieldNames.head.trim.isEmpty))
        allFieldValidations.add(Validation(OperationType.IS_NULL))
      else
        fieldNames.foreach(
          name => fieldValidationMap.getOrElseUpdate(name, new mutable.ListBuffer[Validation]()) += Validation(OperationType.IS_NULL))

      this
    }

    def isBetweenRange(lowerInclusiveRange: String, upperInclusiveRange: String, fieldNames: String*): FinalBuilder = {
      if (fieldNames.isEmpty || (fieldNames.size == 1 & fieldNames.head.trim.isEmpty)) {
        allFieldValidations.add(Validation(OperationType.LESS_THAN_INCLUSIVE, lowerInclusiveRange))
        allFieldValidations.add(Validation(OperationType.GREATER_THAN_INCLUSIVE, upperInclusiveRange))
      }
      else
        fieldNames.foreach(name => {
          fieldValidationMap.getOrElseUpdate(name, new mutable.ListBuffer[Validation]()) += Validation(OperationType.LESS_THAN_INCLUSIVE, lowerInclusiveRange)
          fieldValidationMap.getOrElseUpdate(name, new mutable.ListBuffer[Validation]()) += Validation(OperationType.GREATER_THAN_INCLUSIVE, upperInclusiveRange)
        })

      this
    }

    /*def recorder = {

    }*/

    override def build: Valkyrie = {
      new Valkyrie(this)
    }
  }


}

class Valkyrie private (operations: Operations) {
  def evaluate(string: String): Boolean = {

    println(operations.allFieldValidations)
    println(operations.fieldValidationMap)
    true
  }
}

