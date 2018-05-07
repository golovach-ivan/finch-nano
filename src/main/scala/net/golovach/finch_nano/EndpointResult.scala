package net.golovach.finch_nano

/**
  * A result returned from an Endpoint.
  * This models Option[(Input, Future[Output])] and represents two cases:
  * <ul>
  *   <li>Endpoint is matched (think of 200).</li>
  *   <li>Endpoint is not matched (think of 404, 405, etc).</li>
  * </ul>
  */
sealed trait EndpointResult[+A]

object EndpointResult {
  case class Matched[A](rem: Input, out: Output[A]) extends EndpointResult[A]
  trait NotMatched extends EndpointResult[Nothing]
  case object NotMatched extends NotMatched
}
