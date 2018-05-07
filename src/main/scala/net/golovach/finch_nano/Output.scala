package net.golovach.finch_nano

import com.twitter.finagle.http.{Response, Status}

/**
  * An output of [[Endpoint]].
  * <pre>Output = status × headers</pre>
  */
sealed trait Output[+A] {
  def status: Status

  def headers: Map[String, String]
}

object Output {
  /**
    * A successful [[Output]] that wraps a payload `value`.
    * <pre>Payload = (status × headers) × value</pre>
    */
  case class Payload[A](value: A,
                        status: Status = Status.Ok,
                        headers: Map[String, String] = Map.empty
                        ) extends Output[A]

  /**
    * A failure [[Output]] that captures an [[Exception]].
    * <pre>Failure = (status × headers) × exception</pre>
    */
  case class Failure(cause: Exception,
                     status: Status = Status.BadRequest,
                     headers: Map[String, String] = Map.empty
                     ) extends Output[Nothing]

  implicit class OutputOps[A](val o: Output[A]) {

    def toResponse[CT](implicit tr: ToResponse[A],
                      ): Response = {
      val response = o match {
        case p: Payload[A] => tr(p.value)
        case f: Failure => ??? // todo:
      }

      response.status = o.status
      o.headers.foreach { case (k, v) => response.headerMap.set(k, v) }
      response
    }
  }

  // todo: use StandardCharsets.UTF_8 in all places!
}
