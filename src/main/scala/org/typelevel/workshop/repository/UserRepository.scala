package org.typelevel.workshop.repository

import cats.effect.IO
import cats.implicits._
import doobie.implicits._
import org.typelevel.workshop.model.User

object UserRepository {

  def findByUsername(username: String): IO[Option[User]] =
    sql"""
      SELECT u.id, u.username, u.email
      FROM user u
      WHERE u.username = $username
    """.query[User].option.transact(Database.xa)

  def findAll(): IO[List[User]] = {
    sql"""
      SELECT u.id, u.username, u.email FROM user u
    """.query[User].to[List].transact(Database.xa)
  }

  def addUser(username: String, email: String): IO[Option[User]] =
    sql"INSERT INTO user (username, email) VALUES ($username, $email)"
      .update
      .withUniqueGeneratedKeys[Int]("id")
      .attemptSql
      .map(_.toOption.map(id => User(id, username, email)))
      .transact(Database.xa)

  def updateUser(id: Int, username: String, email: String) : IO[Int] =
    sql"""
      UPDATE user SET username = $username, email = $email
      WHERE id = $id
    """
      .update
      .run
      .transact(Database.xa)

  def deleteUser(username: String): IO[Unit] = (for {
    userId <- sql"SELECT id FROM user WHERE username = $username".query[Int].unique
    _ <- sql"DELETE FROM project WHERE owner = $userId".update.run
    _ <- sql"DELETE FROM user WHERE id = $userId".update.run
  } yield ()).transact(Database.xa).attempt.void


}
