package interfaces

trait PreBuilder {

  def isNull(fieldNames: String*) : FinalBuilder

  def notNull(fieldNames: String*): FinalBuilder

  def isBetweenRange(lowerInclusiveRange: String, upperInclusiveRange: String, fieldNames: String*) : FinalBuilder


}
