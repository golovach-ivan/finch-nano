package net.golovach.finch_nano.example

import com.twitter.finagle.Http
import com.twitter.util.Await
import net.golovach.finch_nano.syntax._

object Example00_sum_div_x  extends App {

  private val int = path[Int]
  private val int2 = int :: int

  val div = get("div" :: int2) {
    (a: Int, b: Int) => Ok(s"${a / b}")
  }

  val sum = get("sum" :: int2) {
    (a: Int, b: Int) => Ok(s"${a + b}")
  }

  val service = (div :+: sum).toService

  Await.ready(Http.server.serve(":8080", service))
}
