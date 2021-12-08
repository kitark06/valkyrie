package interfaces

import core.Valkyrie

abstract class StepBuilder {

  def notNull(fieldNames: String*): Valkyrie

  def isNull(fieldNames: String*): Valkyrie

  def isBlank(fieldNames: String*): Valkyrie

  def isBetweenRange(lowerInclusiveRange: String, upperInclusiveRange: String, fieldNames: String*): Valkyrie

  def isEqualTo(value: String, fieldName: String): Valkyrie

  def isNotEqualTo(value: String, fieldName: String): Valkyrie
}
