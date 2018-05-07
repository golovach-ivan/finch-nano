package net.golovach.finch_nano.example

import com.twitter.finagle.Http
import com.twitter.util.Await
import net.golovach.finch_nano.syntax._

object Example10_foo_xxx extends App {

  val foo = get("foo" :: param[Int]("i") :: path[Int] :: param[String]("s")) {
    (i: Int, k: Int, s: String) => Ok(s"i = $i, k = $k, s = $s")
  }

  Await.ready(Http.server.serve(":8080", foo.toService))
}
