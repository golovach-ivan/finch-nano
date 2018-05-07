package net.golovach.finch_nano

import scala.util.Try

/**
  * Decodes an HTTP path (eg: /foo/bar/baz) represented as UTF-8 `String` into
  * an arbitrary type `A`.
  */
trait DecodePath[A] {
  def apply(s: String): Option[A]
}

object DecodePath {
  implicit val decodePath: DecodePath[String] = (s: String) => Some.apply(s)
  implicit val decodeInt: DecodePath[Int] = (s: String) => Try(s.toInt).toOption
}
