package net.golovach.finch_nano

import java.io.ByteArrayInputStream

import com.twitter.finagle.http.{Response, Status, Version}
import com.twitter.finagle.http.Status.Ok
import com.twitter.finagle.http.Version.Http11
import com.twitter.io.Reader.fromStream
import shapeless.{:+:, CNil, Coproduct, Inl, Inr}

/**
  * Represents a conversion from `A` to [[com.twitter.finagle.http.Response Response]].
  */
trait ToResponse[A] {
  def apply(a: A): Response
}

object ToResponse {

  def instance[A](fn: A => Response): ToResponse[A] = new ToResponse[A] {
    def apply(a: A): Response = fn(a)
  }

  implicit def responseToResponse: ToResponse[Response] = instance(identity)

  implicit def valueToResponse[A](implicit e: Encode[A],
                                 ): ToResponse[A] = instance { a =>
    val resp = Response(Http11, Ok, fromStream(new ByteArrayInputStream(e(a))))
    resp.contentType = "text/plain"
    resp
  }

  trait FromCoproduct[C <: Coproduct] extends ToResponse[C]

  object FromCoproduct {

    def instance[C <: Coproduct](fn: C => Response): FromCoproduct[C] =
      new FromCoproduct[C] {
        def apply(c: C): Response = fn(c)
      }

    implicit def cnilToResponse: FromCoproduct[CNil] =
      instance(_ => Response(Version.Http10, Status.NotFound))

    implicit def cconsToResponse[L, R <: Coproduct](implicit
                                                    trL: ToResponse[L],
                                                    fcR: FromCoproduct[R]
                                                   ): FromCoproduct[L :+: R] = instance {
      case Inl(h) => trL(h)
      case Inr(t) => fcR(t)
    }
  }

  implicit def coproductToResponse[C <: Coproduct](implicit fc: FromCoproduct[C]
                                                  ): ToResponse[C] = fc
}
