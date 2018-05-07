package net.golovach.finch_nano

import com.twitter.finagle.http.{Method, Status}
import net.golovach.finch_nano.Output.{Failure, Payload}
import shapeless.HNil

import scala.language.implicitConversions
import scala.reflect.ClassTag

package object syntax {
  // === methods ===
  def get[A](e: Endpoint[A]): EndpointMapper[A] = new EndpointMapper[A](Method.Get, e)
  def post[A](e: Endpoint[A]): EndpointMapper[A] = new EndpointMapper[A](Method.Post, e)

  // === params/paths ===
  def path[A: DecodePath : ClassTag]: Endpoint[A] = new ExtractPath[A]
  implicit def stringToPath(s: String): Endpoint[HNil] = new MatchPath(s)
  def param[A](name: String)(implicit d: DecodeEntity[A], tag: ClassTag[A]): Endpoint[A] = new ExtractParam[A](name, d, tag)

  // === outputs ===
  def Ok[A](value: A): Output[A] = Payload(value, Status.Ok)
  def NotFound(cause: Exception): Output[Nothing] = Failure(cause, Status.NotFound)
}
