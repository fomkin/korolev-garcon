package korolev.garcon

import cats.effect.IO
import korolev.effect.io.LazyBytes
import korolev.util.JsCode
import korolev.{Context, FormData, Qsid, Transition}

import scala.collection.mutable

class FakeAccess[S](initialState: S) extends korolev.Context.BaseAccess[IO, S, Any] {

  val states: mutable.Buffer[S] =
    mutable.Buffer[S](initialState)

  def state: IO[S] = IO.pure(states.last)
  def transition(f: Transition[S]): IO[Unit] =
    IO.delay(states.append(f(states.last))).map(_ => ())

  def property(id: Context.ElementId[IO]): Context.PropertyHandler[IO] = ???
  def focus(id: Context.ElementId[IO]): IO[Unit] = ???
  def publish(message: Any): IO[Unit] = ???
  def resetForm(id: Context.ElementId[IO]): IO[Unit] = ???
  def sessionId: IO[Qsid] = ???
  def downloadFormData(id: Context.ElementId[IO]): IO[FormData] = ???
  def downloadFiles(id: Context.ElementId[IO]): IO[List[(Context.FileHandler[IO], Array[Byte])]] = ???
  def downloadFilesAsStream(id: Context.ElementId[IO]): IO[List[(Context.FileHandler[IO], LazyBytes[IO])]] = ???
  def downloadFileAsStream(handler: Context.FileHandler[IO]): IO[LazyBytes[IO]] = ???
  def listFiles(id: Context.ElementId[IO]): IO[List[Context.FileHandler[IO]]] = ???
  def syncTransition(f: Transition[S]): IO[Unit] = ???
  def evalJs(code: JsCode): IO[String] = ???
}
