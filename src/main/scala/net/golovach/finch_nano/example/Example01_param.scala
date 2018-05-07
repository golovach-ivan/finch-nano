package net.golovach.finch_nano.example

import com.twitter.finagle.Http
import com.twitter.util.Await
import net.golovach.finch_nano.syntax._

object Example01_param extends App {

  val foo = get("foo" :: param[Int]("i") :: param[String]("s")) {
    (i: Int, s: String) => Ok(s"i = $i, s = $s")
  }

  val sum = get("sum" :: path[Int] :: path[Int]) {
    (a: Int, b: Int) => Ok(s"${a + b}")
  }

  val service = (foo :+: sum).toService

  Await.ready(Http.server.serve(":8080", service))
}
