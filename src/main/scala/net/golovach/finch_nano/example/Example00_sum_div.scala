package net.golovach.finch_nano.example

import com.twitter.finagle.Http
import com.twitter.util.Await
import net.golovach.finch_nano.syntax._

object Example00_sum_div extends App {

  val div = get("div" :: path[Int] :: path[Int]) {
    (a: Int, b: Int) => Ok(s"${a / b}")
  }

  val sum = get("sum" :: path[Int] :: path[Int]) {
    (a: Int, b: Int) => Ok(s"${a + b}")
  }

  val service = (div :+: sum).toService

  Await.ready(Http.server.serve(":8080", service))
}
