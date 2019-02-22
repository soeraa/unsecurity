package io.unsecurity

import cats.effect.{ExitCode, IO, IOApp}
import io.unsecurity.hlinx.HLinx._
import no.scalabin.http4s.directives.Directive
import org.http4s.Method

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp {
  val unsecurity2: Unsecurity[IO, String, String] = new Unsecurity[IO, String, String] {
    override def sc: SecurityContext[IO, String, String] = ???
  }
  import unsecurity2._

  val server: Server[IO] = Server[IO](
    port = 8088,
    host = "0.0.0.0"
  )

  val helloWorld: unsecurity2.Complete =
    unsecure(
      Endpoint(
        method = Method.GET,
        path = Root / "hello",
        produces = Produces.json[String]
      )
    ).run { _ =>
      Directive.success("Hello world")
    }

  val collidingHello =
    unsecure(
      Endpoint(
        Method.GET,
        Root / 'collideWithHello.as[String],
        Produces.json[String]
      )
    ).run {
      case ((collide), _) =>
        Directive.success(
          s"Hello, $collide"
        )
    }

  case class StringAndInt(s: String, i: Int)

  val twoParams =
    unsecure(
      Endpoint(
        Method.GET,
        Root / 'param1.as[String] / 'param2.as[Int],
        Produces.json[String]
      )
    ).resolve { case ((s, i), _) => StringAndInt(s, i) }
      .run { sai =>
        Directive.success(sai.toString)
      }

  override def run(args: List[String]): IO[ExitCode] = {
    import cats.implicits._

    val httpRoutes = toHttpRoutes(
      List(
        helloWorld,
        collidingHello,
        twoParams
      ))

    server
      .serve(httpRoutes)
      .compile
      .drain
      .as(ExitCode.Success)
  }
}