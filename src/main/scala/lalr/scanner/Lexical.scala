package lalr.scanner

import lalr.Regex
import lalr.util.Terminal
import lalr.util.Fixity

case object Whitespace extends Terminal {
  def fixity = Fixity.default
}

case class Rule(lhs: Terminal, rhs: Regex) {
  def item = Item(lhs, rhs)
}

case class Lexical(rules: List[Rule]) {
  
}