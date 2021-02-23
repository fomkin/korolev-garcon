# Garcon

This extension intended to help you load data to a state in [Korolev](https://github.com/fomkin/korolev) applications. It's especially useful when you have some data that loads in multiple parts of the app in a same way. Garcon allows to generalize data loading and remove this work from event handlers.

Lets see how it works. For example you have an app which manage user's friends:
```scala
case class User(deviceId: String, email: String)
case class ApplicationState(user: User, friends: List[User])
```
To make this app works, you should configure `StateLoader.forDeviceId`, which will load user and his friends from database. 

This obvious solution may lead to problems with user experience. What if DBMS on heavy load? What if request is not optimized well? What if we work with real word case where we need to make a hundreds of SQL-queries to show user a first screen? Until data is not loaded, user looks on a blank page. This is unacceptable.

## How to use

Garcon available in Maven Central. Garcon supports Scala 2.12 and 2.13 and Korolev 0.14.0 or higher. Add dependency to your project.

```scala
libraryDependencies += "org.fomkin" %% "korolev-garcon" % "0.3.0"
```

This library offers [Demand](https://github.com/fomkin/korolev-garcon/blob/master/src/main/scala/korolev/garcon/Demand.scala) data type, [Garcon](https://github.com/fomkin/korolev-garcon/blob/master/src/main/scala/korolev/garcon/Garcon.scala) type class, and extension for Korolev which allows to manage data asynchronously.

Let's come back to friends list. First let's replace data with demands.

```scala
import korolev.garcon._

case class User(deviceId: String, email: String)
case class FriendList(xs: List[User])
case class ApplicationState(
  user: Demand[String, User, String],
  friends: Demand[String, FriendList, String]
)
```

Next we should define a way to load data (pseudo-SQL).

```scala
implicit val userGarcon: Garcon[Future, String, User, Throwable] =
  (id, modify) => sql"select * from user where id = $id"
   .one[User]
   .run
   .flatMap(user => modify(Demand.Ready(user)))
   .recover(e => modify(Demand.Error(e)))

implicit val friendListGarcon: Garcon[Future, String, FriendList, Throwable] =
  (id, modify) => sql"select from friends left join user on friends.of = user.id where friends.of = $id"
   .list[User]
   .run
   .flatMap(users => modify(Demand.Ready(users)))
   .recover(e => modify(Demand.Error(e)))
```

Finally we add extension to Korolev's config. Make sure that implicit garcons are in the scope.

```scala
KorolevServiceConfig[Future, ApplicationState, Any](
  extensions = List(
    Garcon.extension[Future, ApplicationState, Throwable],
    ...
  ),
  ...
)
```
Define `StateLoader` and renderer.

```scala
KorolevServiceConfig[Future, ApplicationState, Any](
  ...
  stateLoader = StateLoader.forDeviceId { deviceId =>
    Future.successful(
      ApplicationState(
        user = Demand.Start(deviceId, prev = None),
        friends = Demand.Start(deviceId, prev = None)  
      )
    )
  },
  render = { state =>
    Html(
      body(
        state.user match {
          case Demand.Ready(user) => div(s"User: ${user.email}")
          case _ => div("Loading user")
        },
        state.friends match {
          case Demand.Ready(FriendList(xs)) => ul(xs.map(x => li(x.email)))
          case _ => div("Loading friend list")
        }
      )
    )
  }
)
```

That's all. The extension will watch a state. When one of demands became `Start` it will run corresponding   `Garcon` and reset demands to `Eventually`. After loading is complete demand will become `Ready`.
