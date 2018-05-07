package net.golovach.finch_nano

import com.twitter.finagle.http.Request

case class Input(request: Request, route: Seq[String])
