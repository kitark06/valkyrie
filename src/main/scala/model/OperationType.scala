package model

object OperationType extends Enumeration {
  val NOT_NULL,
  IS_NULL,
  IS_BLANK,
  GREATER_THAN_INCLUSIVE,
  LESS_THAN_INCLUSIVE
  = Value
}
