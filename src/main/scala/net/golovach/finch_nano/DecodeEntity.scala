package net.golovach.finch_nano

import shapeless.{::, Generic, HList, HNil}

import scala.util.{Success, Try}

trait DecodeEntity[A] {
  def apply(s: String): Try[A]
}

object DecodeEntity {
  /**
    * Returns a [[DecodeEntity]] instance for a given type.
    */
  @inline def apply[A](implicit d: DecodeEntity[A]): DecodeEntity[A] = d

  implicit val decodeString: DecodeEntity[String] = instance(s => Success(s))

  implicit val decodeInt: DecodeEntity[Int] = instance(s => Try(s.toInt))

  implicit val decodeLong: DecodeEntity[Long] = instance(s => Try(s.toLong))

  implicit val decodeFloat: DecodeEntity[Float] = instance(s => Try(s.toFloat))

  implicit val decodeDouble: DecodeEntity[Double] = instance(s => Try(s.toDouble))

  implicit val decodeBoolean: DecodeEntity[Boolean] = instance(s => Try(s.toBoolean))

  /**
    * Creates an [[DecodeEntity]] instance from a given function `String => Try[A]`.
    */
  def instance[A](fn: String => Try[A]): DecodeEntity[A] = new DecodeEntity[A] {
    def apply(s: String): Try[A] = fn(s)
  }

  /**
    * Creates a [[DecodeEntity]] from [[shapeless.Generic]] for single value case classes.
    */
  implicit def decodeFromGeneric[A, H <: HList, E](implicit
                                                   gen: Generic.Aux[A, H],
                                                   ev: (E :: HNil) =:= H,
                                                   de: DecodeEntity[E]
                                                  ): DecodeEntity[A] = instance(s => de(s).map(b => gen.from(b :: HNil)))
}
