package net.golovach.finch_nano

import shapeless.ops.adjoin.Adjoin
import shapeless.{::, DepFn2, HNil}

trait PairAdjoin[A, B] extends DepFn2[A, B]

object PairAdjoin {
  type XAux[A, B, Out0] = PairAdjoin[A, B] {type Out = Out0}

  implicit def pairAdjoin[A, B, Out0](implicit
                                      adjoin: Adjoin.Aux[A :: B :: HNil, Out0]
                                     ): XAux[A, B, Out0] =
    new PairAdjoin[A, B] {
      type Out = Out0

      def apply(a: A, b: B): Out0 = adjoin(a :: b :: HNil)
    }
}
