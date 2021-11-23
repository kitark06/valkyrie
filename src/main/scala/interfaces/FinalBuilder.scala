package interfaces

import core.Valkyrie

trait FinalBuilder extends PreBuilder {
  def build : Valkyrie
}
