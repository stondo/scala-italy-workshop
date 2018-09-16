package org.typelevel.workshop.repository

import cats.effect.IO
import cats.implicits._
import doobie.implicits._
import io.circe.generic.auto._
import org.http4s.circe._
import org.typelevel.workshop.model.Project

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

  def updateNameAndDesc(id: Int, name: String, description: String): IO[Int] =
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
