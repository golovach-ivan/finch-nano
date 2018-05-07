package net.golovach.finch_nano

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import shapeless.{::, HList, HNil}

/*
 * Bootstraps a Finagle HTTP service out of the collection of Finch endpoints.
 */
class Bootstrap[ES <: HList](val endpoints: ES) {
  self =>

  // todo: remove/inline?
  class XServe[CT] {
    def apply[E](e: Endpoint[E]): Bootstrap[Endpoint[E] :: ES] =
      new Bootstrap[Endpoint[E] :: ES](e :: self.endpoints)
  }

  def serve[CT]: XServe[CT] = new XServe[CT]

  def toService(implicit ts: ToService[ES]): Service[Request, Response] = ts(endpoints)
}

object Bootstrap extends Bootstrap[HNil](endpoints = HNil)
