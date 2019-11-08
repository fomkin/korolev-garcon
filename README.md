# Garcon

This extension intended to help you load data to a state in [Korolev](https://github.com/fomkin/korolev) applications. It's especially useful when you have some data that loads in multiple parts of the app in a same way. Garcon allows you to generalize data loading and remove this work from event handlers.

Lets see how it works. For example you have an app which manage user's friends:
```scala
case class User(deviceId: String, email: String)
case class ApplicationState(user: User, friends: List[User])
```
To make this app works, you should configure `StateLoader.forDeviceId`, which will load user and his friends from database. 

This obvious solution may lead to problems with user experience. What if DBMS on heavy load? What if request is not optimized well? What if we work with real word case where we need to make a hundreds of SQL-queries to show user a first screen? Until data is not loaded, user looks on a blank page. This is unacceptable.

Lets see what Garcon offers:
```scala
import korolev.garcon._

case class User(deviceId: String, email: String)
case class FriendList(xs: List[User])
case class ApplicationState(
  user: Demand[String, User, String],
  friends: Demand[String, FriendList, String])
```