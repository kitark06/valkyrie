package model

case class Outcome[T](isValid: Boolean, errors: Seq[FieldValidationResult], payload: T)