package korolev.garcon

sealed trait Demand[+Q, +R, +E]

object Demand {
  case class Start[+Q, +R](query: Q, prev: Option[R]) extends Demand[Q, R, Nothing]
  case class Eventually[+R](prev: Option[R]) extends Demand[Nothing, R, Nothing]
  case class Ready[+R](result: R) extends Demand[Nothing, R, Nothing]
  case class Error[E](error: E) extends Demand[Nothing, Nothing, E]
}