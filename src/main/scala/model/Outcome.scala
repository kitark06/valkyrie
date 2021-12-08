package model

case class Outcome[T](isValid: Boolean, errors: AnyRef, payload: T)
