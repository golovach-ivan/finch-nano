package net.golovach.finch_nano

import shapeless.ops.function.FnToProduct

trait Mapper[A] {
  type Out
  def apply(e: Endpoint[A]): Endpoint[Out]
}

object Mapper {
  type Aux[A, B] = Mapper[A] {type Out = B}

  implicit def mapperFromOutputHFunction[A, B, F, OB](f: F)(implicit
                                                            ftp: FnToProduct.Aux[F, A => OB],
                                                            ev: OB <:< Output[B]
  ): Mapper.Aux[A, B] = instance(_.mapOutput(value => ev(ftp(f)(value))))

  // todo: add exception handling here!
  def instance[A, B](f: Endpoint[A] => Endpoint[B]): Mapper.Aux[A, B] = new Mapper[A] {
    type Out = B

    def apply(e: Endpoint[A]): Endpoint[B] = f(e)
  }
}
