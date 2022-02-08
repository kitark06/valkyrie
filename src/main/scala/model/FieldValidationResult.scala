package model

case class FieldValidationResult( field: Field , validationFailures : Seq[ValidationRule])