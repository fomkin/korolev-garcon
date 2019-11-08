package korolev.garcon

import cats.effect.IO
import org.specs2._
import monocle.macros.GenPrism
import monocle.macros.GenLens
import korolev.catsEffectSupport.implicits._
import org.specs2.matcher.MatchResult

class GarconSpec extends Specification { def is = s2"""
  This is a specification to check that Garcon
  reset entries properly.

    reset direct case class field          $resetDirectCaseClassField
    reset field in sealed trait hierarchy  $resetFieldInSealedTraitHierarchy
    produce only one transition to reset   $produceOnlyOneTransitionToReset
    shows error                            $showsError
  """

  import GarconSpec._

  lazy val resetDirectCaseClassField: MatchResult[Any] = {
    val initialState =
      ApplicationState(
        Tab.Friends(Demand.Ready(List(jane))),
        Demand.Start(0, None)
      )
    val fakeAccess = new FakeAccess(initialState)
    extension
      .setup(fakeAccess)
      .flatMap(hs => hs.onState(initialState))
      .unsafeRunAsyncAndForget()

    fakeAccess.states.length shouldEqual 3
    fakeAccess.states(1).user shouldEqual Demand.Eventually(None)
    fakeAccess.states(2).user shouldEqual Demand.Ready(john)
  }

  lazy val resetFieldInSealedTraitHierarchy: MatchResult[Any] = {
    val initialState =
      ApplicationState(
        Tab.Friends(Demand.Start(List(1, 2), None)),
        Demand.Ready(john)
      )
    val fakeAccess = new FakeAccess(initialState)
    extension
      .setup(fakeAccess)
      .flatMap(hs => hs.onState(initialState))
      .unsafeRunAsyncAndForget()

    fakeAccess.states.length shouldEqual 3
    friendsLens.getOption(fakeAccess.states(1)) shouldEqual Some(Demand.Eventually(None))
    friendsLens.getOption(fakeAccess.states(2)) shouldEqual Some(Demand.Ready(List(jane, bob)))
  }

  lazy val produceOnlyOneTransitionToReset: MatchResult[Any] = {
    val initialState =
      ApplicationState(
        Tab.Friends(Demand.Start(List(1, 2), None)),
        Demand.Start(0, Some(jane))
      )
    val fakeAccess = new FakeAccess(initialState)
    extension
      .setup(fakeAccess)
      .flatMap(hs => hs.onState(initialState))
      .unsafeRunAsyncAndForget()

    fakeAccess.states.length shouldEqual 4

    val resetState = fakeAccess.states(1)
    friendsLens.getOption(resetState) shouldEqual Some(Demand.Eventually(None))
    resetState.user shouldEqual Demand.Eventually(Some(jane))

    val setUserState = fakeAccess.states(2)
    friendsLens.getOption(setUserState) shouldEqual Some(Demand.Eventually(None))
    setUserState.user shouldEqual Demand.Ready(john)

    val setFriendsState = fakeAccess.states(3)
    friendsLens.getOption(setFriendsState) shouldEqual Some(Demand.Ready(List(jane, bob)))
    setFriendsState.user shouldEqual Demand.Ready(john)
  }

  lazy val showsError: MatchResult[Any] = {
    val initialState =
      ApplicationState(
        Tab.Friends(Demand.Ready(List())),
        Demand.Start(99, None)
      )
    val fakeAccess = new FakeAccess(initialState)
    extension
      .setup(fakeAccess)
      .flatMap(hs => hs.onState(initialState))
      .unsafeRunAsyncAndForget()

    fakeAccess.states.length shouldEqual 3
    fakeAccess.states(1).user shouldEqual Demand.Eventually(None)
    fakeAccess.states(2).user shouldEqual Demand.Error("99 not exists")
  }

}

object GarconSpec {

  final case class User(id: Long, email: String)
  final case class UserProfile(id: Long, name: String, birthYear: Int)

  sealed trait Tab

  object Tab {
    final case class Friends(xs: Demand[List[Long], List[User], String]) extends Tab
    final case class Profile(profile: Demand[Long, UserProfile, String]) extends Tab
  }

  final case class ApplicationState(tab: Tab, user: Demand[Long, User, String])

  private[garcon] val friendsPrism = GenPrism[Tab, Tab.Friends]
  private[garcon] val profilePrism = GenPrism[Tab, Tab.Profile]
  private[garcon] val tabLens = GenLens[ApplicationState](_.tab)

  private[garcon] val userLens = GenLens[ApplicationState](_.user).asOptional
  private[garcon] val friendsLens = tabLens.composePrism(friendsPrism).composeLens(GenLens[Tab.Friends](_.xs))
  private[garcon] val profileLens = tabLens.composePrism(profilePrism).composeLens(GenLens[Tab.Profile](_.profile))

  private[garcon] val john = User(0, "john@example.com")
  private[garcon] val jane = User(1, "jane@example.com")
  private[garcon] val bob = User(2, "bob@example.com")
  private[garcon] val users = Map(
    0L -> john,
    1L -> jane,
    2L -> bob,
    3L -> User(3, "alice@example.com")
  )

  object UserGarcon extends Garcon[IO, Long, User, String] {
    def serve(query: Long, modify: Demand[Long, User, String] => IO[Unit]): IO[Unit] =
      query match {
        case 99L => modify(Demand.Error("99 not exists"))
        case _ => modify(Demand.Ready(users(query)))
      }
  }

  object UsersGarcon extends Garcon[IO, List[Long], List[User], String] {
    def serve(query: List[Long], modify: Demand[List[Long], List[User], String] => IO[Unit]): IO[Unit] =
      modify(Demand.Ready(query.map(users)))
  }

  private[garcon] val extension = Garcon.Extension[IO, ApplicationState, Any](
    Garcon.Entry[IO, ApplicationState, Long, User, String](UserGarcon, userLens.getOption, (s, r) => userLens.set(r)(s)),
    Garcon.Entry[IO, ApplicationState, List[Long], List[User], String](UsersGarcon, friendsLens.getOption, (s, r) => friendsLens.set(r)(s))
  )
}