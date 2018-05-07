package net.golovach.finch_nano

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.util.Future
import net.golovach.finch_nano.EndpointResult.{Matched, NotMatched}
import shapeless.{::, HList, HNil}

/**
  * Wraps a given list of [[net.golovach.finch_nano.Endpoint XEndpoint]]s and their content-types with a Finagle [[com.twitter.finagle.Service Service]].
  * Endpoint-to-Service converter.
  * Represents a conversion from ??? to ???.
  *
  * Guarantees to:
  * - handle Finch's own errors (i.e., [[Error]] and [[Error]]) as 400s
  * - respond with 404 when an endpoint is not matched
  *
  * @tparam ES endpoints product
  */
trait ToService[ES <: HList] {
  def apply(endpoints: ES): Service[Request, Response]
}

object ToService {

  // todo: remove Error / Errors
  private val respond400OnErrors: PartialFunction[Throwable, Output[Nothing]] = {
    //    case e: io.finch.Error => XOutput.XFailure(e, Status.BadRequest)
    //    case es: io.finch.Errors => XOutput.XFailure(es, Status.BadRequest)
    case es: Exception => Output.Failure(es, Status.BadRequest)
  }

  implicit val hnilTS: ToService[HNil] =
    (_: HNil) => (req: Request) =>
      Future(Response(req.version, Status.NotFound))

  implicit def hlistTS[A, EH <: Endpoint[A], ET <: HList](implicit
                                                          ntrA: ToResponse[A],
                                                          tsT: ToService[ET]
                                                          ): ToService[Endpoint[A] :: ET] = new ToService[Endpoint[A] :: ET] {
    def apply(es: Endpoint[A] :: ET): Service[Request, Response] =
      new Service[Request, Response] {
        val underlying = es.head.handle(respond400OnErrors)

        def apply(req: Request): Future[Response] =
          underlying(Input(req, req.path.split("/").toList.drop(1))) match {
            case Matched(rem, out) if rem.route.isEmpty =>
              Future.value(out.toResponse(ntrA))
            case _: NotMatched => tsT(es.tail)(req)
          }
      }
  }
}
