package korolev.garcon

import cats.effect.IO
import korolev.{Context, LazyBytes, QualifiedSessionId, Transition}

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
  def downloadFormData(id: Context.ElementId[IO]): Context.FormDataDownloader[IO, S] = ???
  def downloadFiles(id: Context.ElementId[IO]): IO[List[Context.File[Array[Byte]]]] = ???
  def downloadFilesAsStream(id: Context.ElementId[IO]): IO[List[Context.File[LazyBytes[IO]]]] = ???
  def resetForm(id: Context.ElementId[IO]): IO[Unit] = ???
  def sessionId: IO[QualifiedSessionId] = ???
  def evalJs(code: String): IO[String] = ???
}
