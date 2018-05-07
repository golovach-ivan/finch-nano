package net.golovach.finch_nano

import java.nio.charset.StandardCharsets.UTF_8

/**
  * Encodes an HTTP payload (represented as an arbitrary type `A`).
  */
trait Encode[A] {
  def apply(a: A): Array[Byte]
}

object Encode {
  def instance[A](fn: A => Array[Byte]): Encode[A] = (a: A) => fn(a)

  implicit val encodeStringAsTextPlain: Encode[String] =
    instance[String](s => s.getBytes(UTF_8))
}
