package interfaces

import core.Valkyrie.Operations

abstract class StepBuilder[T] {

  def notNull(fieldNames: String*): Operations[T]

  def isNull(fieldNames: String*): Operations[T]

  def isBlank(fieldNames: String*): Operations[T]

  def isBetweenRange(lowerInclusiveRange: String, upperInclusiveRange: String, fieldNames: String*): Operations[T]

}
