package pg

import slick.jdbc.H2Profile.api._


object Demo extends App with Tables {
  exec(populate())


  val newWay = messages
    .joinOn(_.room)
    .joinOn(_.as[MessageTable].to)
    .map(current => current.as[MessageTable].roomId -> current.as[UserTable].name)

  val oldWay = messages
    .join(rooms).on { case (msg, room) => room.id === msg.roomId }
    .join(users).on { case ((msg, room), user) => user.id === msg.toId }
    .map { case ((msg, room), user) => msg.roomId -> user.name }

  compare(sqlQuery(newWay), sqlQuery(oldWay))

  compare(exec(newWay.result), exec(oldWay.result))

}
