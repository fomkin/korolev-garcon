package korolev.garcon

import cats.effect.IO
import korolev.{Context, Qsid, Transition, effect}
import korolev.data.Bytes
import korolev.util.JsCode
import korolev.web.FormData

import scala.collection.mutable

class FakeAccess[S](initialState: S) extends korolev.Context.BaseAccess[IO, S, Any] {

  val states: mutable.Buffer[S] =
    mutable.Buffer[S](initialState)

  def state: IO[S] = IO.pure(states.last)
  def transition(f: Transition[S]): IO[Unit] =
    IO.delay(states.append(f(states.last))).map(_ => ())

  override def property(id: Context.ElementId): Context.PropertyHandler[IO] = ???
  override def focus(id: Context.ElementId): IO[Unit] = ???
  override def publish(message: Any): IO[Unit] = ???
  override def downloadFormData(id: Context.ElementId): IO[FormData] = ???
  override def downloadFiles(id: Context.ElementId): IO[List[(Context.FileHandler, Bytes)]] = ???
  override def downloadFilesAsStream(id: Context.ElementId): IO[List[(Context.FileHandler, effect.Stream[IO, Bytes])]] = ???
  override def downloadFileAsStream(handler: Context.FileHandler): IO[effect.Stream[IO, Bytes]] = ???
  override def listFiles(id: Context.ElementId): IO[List[Context.FileHandler]] = ???
  override def resetForm(id: Context.ElementId): IO[Unit] = ???
  override def syncTransition(f: Transition[S]): IO[Unit] = ???
  override def sessionId: IO[Qsid] = ???
  override def evalJs(code: JsCode): IO[String] = ???
  override def registerCallback(name: String)(f: String => IO[Unit]): IO[Unit] = ???
}
