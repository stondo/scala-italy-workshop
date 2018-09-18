package org.typelevel.workshop.repository

import cats.effect.IO
import cats.implicits._
import doobie.implicits._
import org.typelevel.workshop.model.{Project, User}

object ProjectRepository {

//  implicit val decoder = jsonOf[IO, Project]

  def findByName(name: String): IO[Option[Project]] =
    sql"""
      SELECT p.id, p.name, p.description, u.id, u.username, u.email
      FROM project p JOIN user u ON p.owner = u.id
      WHERE p.name = $name
    """.query[Project].option.transact(Database.xa)

  def findAll(): IO[List[Project]] = {
    sql"""
      SELECT p.id, p.name, p.description, u.id, u.username, u.email FROM project p JOIN user u ON p.owner = u.id
    """.query[Project].to[List].transact(Database.xa)
  }

  def findAllByUser(userId: Int): IO[List[Project]] = {
    sql"""
      SELECT p.id, p.name, p.description, u.id, u.username, u.email FROM project p JOIN user u ON p.owner = u.id WHERE p.owner = $userId
    """.query[Project].to[List].transact(Database.xa)
  }

  def addProject(name: String, description: String, ownerId: Int): IO[Option[Project]] =
    (for {
      user <- sql"""SELECT u.id, u.username, u.email FROM user u WHERE id = $ownerId""".query[User].option
      project <- sql"INSERT INTO project (name, description, owner) VALUES ($name, $description, $ownerId)"
        .update
        .withUniqueGeneratedKeys[Int]("id")
        .attemptSql
        .map(_.toOption.map(id => Project(id, name, description, user.get)))
    } yield project).transact(Database.xa)

  def updateProject(id: Int, name: String, description: String): IO[Int] =
    sql"""
      UPDATE project SET name = $name, description = $description
      WHERE id = $id
    """
      .update
      .run
      .transact(Database.xa)

  def deleteProject(name: String): IO[Unit] = (for {
    projectId <- sql"SELECT id FROM project WHERE name = $name".query[Int].unique
    _ <- sql"DELETE FROM project WHERE id = $projectId".update.run
  } yield ()).transact(Database.xa).attempt.void

}
