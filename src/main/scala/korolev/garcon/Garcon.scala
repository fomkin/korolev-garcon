package korolev.garcon

import scala.language.experimental.macros
import scala.language.higherKinds

trait Garcon[F[_], Q, +R, +E] {
  def serve(query: Q, modify: Demand[Q, R, E] => F[Unit]): F[Unit]
}

object Garcon {

  import korolev.{Async, Extension}
  import korolev.Async._
  import Entry.Request

  /**
   * Configure garcon in manual way.
   */
  def extension[F[_] : Async, S, M](entries: Entry[F, S, _, _, _]*): Extension[F, S, M] =
    korolev.Extension.pure[F, S, M] { access =>
      korolev.Extension.Handlers(
        onState = { state =>
          val rqs = entries.toList.flatMap(_.request(state, access.transition))
          if (rqs.nonEmpty) {
            for {
              _ <- access.transition(s => rqs.foldLeft(s)((a, r) => r.reset(a)))
              _ <- Async[F].sequence(rqs.map(_.serve()))
            } yield ()
          } else {
            Async[F].unit
          }
        }
      )
    }

  /**
   * Configure garcon automatically
   * @tparam F
   * @tparam S
   * @tparam M
   * @return
   */
  def extension[F[_], S, M]: Extension[F, S, M] =
    macro DerivationMacros.extension[F, S, M]

  final case class Entry[F[_]: Async, S, Q, R, E](garcon: Garcon[F, Q, R, E],
                                                  view: S => Option[Demand[Q, R, E]],
                                                  modify: (S, Demand[Q, R, E]) => S) {

    def request(state: S, transition: (S => S) => F[Unit]): Option[Request[F, S]] = view(state) match {
      case Some(Demand.Start(q, p)) =>
        Some(
          Request(
            reset = modify(_, Demand.Eventually(p)),
            serve = () => garcon.serve(q, r => transition(s => modify(s, r)))
          )
        )
      case _ => None
    }
  }

  object Entry {
    final case class Request[F[_], S](reset: S => S,
                                      serve: () => F[Unit])
  }
}