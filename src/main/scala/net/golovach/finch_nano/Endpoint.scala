package net.golovach.finch_nano

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Method, Request, Response}
import io.netty.handler.codec.http.QueryStringDecoder
import net.golovach.finch_nano.EndpointResult.{Matched, NotMatched}
import net.golovach.finch_nano.Output.{Failure, Payload}
import shapeless.ops.adjoin.Adjoin
import shapeless.{:+:, CNil, HNil, Inl, Inr}

import scala.reflect.ClassTag

trait Endpoint[A] {
  self =>

  def apply(input: Input): EndpointResult[A]

  def map[B](fn: A => B): Endpoint[B] = (input: Input) =>
    self(input) match {
      case Matched(rem, Payload(v, s, h)) => Matched(rem, Payload(fn(v), s, h))
      case Matched(rem, Failure(e, s, h)) => Matched(rem, Failure(e, s, h))
      case skipped: NotMatched => skipped
    }

  def mapOutput[B](fn: A => Output[B]): Endpoint[B] = (input: Input) =>
    self(input) match {
      case Matched(rem, Payload(v, _, _)) => Matched(rem, fn(v)) // todo: compose headers?
      case Matched(rem, Failure(e, s, h)) => Matched(rem, Failure(e, s, h))
      case skipped: NotMatched => skipped
    }

  def handle[B >: A](pf: PartialFunction[Throwable, Output[B]]): Endpoint[B] = (input: Input) =>
    self(input) match {
      case x@Matched(_, Payload(_, _, _)) => x
      case Matched(rem, Failure(e, s, h)) => Matched(rem, pf(e))
      case skipped: NotMatched => skipped
    }

  def ::[B](other: Endpoint[B])(implicit pa: PairAdjoin[B, A]): Endpoint[pa.Out] =
    new Endpoint[pa.Out] {
      private[this] val inner: Endpoint[pa.Out] =
        (input: Input) => other(input) match {
          case fst@Matched(_, _) => self(fst.rem) match {
            case snd@Matched(_, _) =>
              val composedOut = (fst.out, snd.out) match {
                case (Payload(v0, _, h0), Payload(v1, s, h1)) =>
                  Payload(pa(v0, v1), s, h0 ++ h1)
                case (x@Failure(_, _, _), _) => x
                case (_, x@Failure(_, _, _)) => x
              }
              Matched(snd.rem, composedOut)
            case skipped: NotMatched => skipped
          }
          case skipped: NotMatched => skipped
        }

      def apply(input: Input): EndpointResult[pa.Out] = inner(input)
    }

  /**
    * Composes this endpoint with another in such a way that co-products are flattened.
    */
  def :+:[B](that: Endpoint[B])(implicit a: Adjoin[B :+: A :+: CNil]): Endpoint[a.Out] = {
    val left: Endpoint[a.Out] = that.map(x => a(Inl[B, A :+: CNil](x)))
    val right: Endpoint[a.Out] = self.map(x => a(Inr[B, A :+: CNil](Inl[A, CNil](x))))
    (input: Input) =>
      (left(input), right(input)) match {
        case (a@Matched(_, _), b@Matched(_, _)) =>
          if (a.rem.route.length <= b.rem.route.length) a else b
        case (a@Matched(_, _), _: NotMatched) => a
        case (_: NotMatched, b@Matched(_, _)) => b
        case (a: NotMatched, b: NotMatched) => a
      }
  }

  /**
    * Converts this endpoint to a Finagle service `Request => Future[Response]` that serves custom
    * content-type `CT`.
    *
    * Consider using [[Bootstrap]] instead.
    */
  def toService(implicit tr: ToResponse[A],
               ): Service[Request, Response] = Bootstrap.serve(this).toService
}

class ExtractPath[A](implicit d: DecodePath[A], ct: ClassTag[A]) extends Endpoint[A] {
  def apply(input: Input): EndpointResult[A] = input.route match {
    case s +: rest => d(QueryStringDecoder.decodeComponent(s)) match {
      case Some(a) => Matched(Input(input.request, rest), Payload(a))
      case _ => NotMatched
    }
    case _ => NotMatched
  }
}

class MatchPath(s: String) extends Endpoint[HNil] {
  def apply(input: Input): EndpointResult[HNil] = input.route match {
    case `s` +: rest => Matched(Input(input.request, rest), Payload(HNil))
    case _ => NotMatched
  }
}

class ExtractParam[A](name: String, d: DecodeEntity[A], tag: ClassTag[A]) extends Endpoint[A] {
  override def apply(input: Input): EndpointResult[A] =
    input.request.params.get(name) match {
      case None => NotMatched
      case Some(value) =>
        d(value) match {
          case util.Success(r) => Matched(input, Payload(r)) // todo: ???
          case util.Failure(e: Exception) => Matched(input, Failure(e)) // todo
        }
    }
}

class EndpointMapper[A](m: Method, e: Endpoint[A]) extends Endpoint[A] {

  def apply(mapper: Mapper[A]): Endpoint[mapper.Out] =
    mapper(this)

  def apply(input: Input): EndpointResult[A] =
    if (input.request.method == m) e(input) else NotMatched
}