package lrk.scanner

import java.io.Reader

import scala.collection.mutable
import scala.util.control.Breaks._

import lrk.Range
import lrk.Scanner
import lrk.Token
import lrk.util.Buffer
import lrk.Mode

object DFA {
  def translate(mode: Mode): Lexical = {
    val rules = mode.regexps.map {
      re => Rule(re.symbol, re.re)
    }
    Lexical(rules.toList)
  }

  def states(lexical: Lexical): (State, Seq[State]) = {
    var number = 0

    val todo = mutable.Queue[State]()
    val states = mutable.Buffer[State]()

    val init = new State(lexical.rules map (_.item), null)

    todo enqueue init

    while (!todo.isEmpty) {
      val state = todo.dequeue

      val that = states find (_.items == state.items)

      that match {
        case Some(that) =>
          if (state.prev != null) {
            for ((symbol, `state`) <- state.prev.transitions) {
              state.prev.transitions(symbol) = that
            }
          }

        case None =>
          number += 1
          state.number = number
          states += state
          todo ++= state.successors
      }
    }

    (init, states)
  }

  /**
   * Scan longest prefix of cs from a state.
   */
  def scan(in: Reader, scanner: Scanner) = new Iterator[Token] {
    val buf = new Array[Char](1)
    val result = new Buffer()

    var start = 0
    var letter: Char = _
    var atEof: Boolean = _

    step()

    def step() {
      atEof = (in.read(buf) < 0)
      letter = buf(0)
    }

    def hasNext = {
      !atEof
    }

    def next = {
      val init = scanner.state
      var state = init
      var accepting: Option[(State, Int)] = None

      breakable {
        while (hasNext) {
          assert(state != null)
          if (state.transitions contains letter) {
            result append letter
            state = state.transitions(letter)
            if (state.accepts) {
              accepting = Some((state, result.length))
            }
          } else {
            break
          }

          step()
        }
      }

      accepting match {
        case Some((state, length)) =>
          val text = result shift length
          val symbols = state.canAccept
          val range = Range(start, length)

          start += length
          Token(symbols.head, text, range)
        case None if hasNext =>
          sys.error("unexpected character " + Letter.fmt(letter))
        case None =>
          sys.error("unexpected end of input")
      }
    }
  }
}