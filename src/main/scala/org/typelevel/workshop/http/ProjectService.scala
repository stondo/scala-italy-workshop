package org.typelevel.workshop.http

import cats.effect.IO
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.HttpService
import org.http4s.dsl.io._
import org.http4s.circe.CirceEntityCodec._
import org.typelevel.workshop.repository.ProjectRepository


object ProjectService {

  case class ProjectWithoutOwner(id: Int, name: String, description: String)

  case class CreateProjectRequest(name: String, description: String, ownerId: Int)

  val service: HttpService[IO] = HttpService[IO] {

    case GET -> Root / name =>
      ProjectRepository.findByName(name).flatMap {
        case Some(project) => Ok(project)
        case None => NotFound(s"No project found: $name".asJson)
      }

    case GET -> Root =>
      ProjectRepository.findAll().flatMap {
        case h::Nil => Ok(h)
        case h::t => Ok(h::t)
        case Nil => NotFound(s"No project found: ".asJson)
      }

    case GET -> Root / "byuser" /userId =>
      ProjectRepository.findAllByUser(userId.toInt).flatMap {
        case h::Nil => Ok(h)
        case h::t => Ok(h::t)
        case Nil => NotFound(s"No project found: $userId".asJson)
      }

    case req @ POST -> Root => for {
      createProjectReq <- req.as[CreateProjectRequest]
      projectOption <- ProjectRepository.addProject(createProjectReq.name, createProjectReq.description, createProjectReq.ownerId)
      result <- projectOption match {
        case Some(project) => Created(project)
        case None => BadRequest("Project already exists or wrong owner id".asJson)
      }
    } yield result

    case req @ PUT -> Root => for {
      updateNameDesc <- req.as[ProjectWithoutOwner]
      affectedRows <- ProjectRepository.updateProject(updateNameDesc.id, updateNameDesc.name, updateNameDesc.description)
      result <- if (affectedRows > 0) Created(affectedRows)
                else BadRequest("Error updating project".asJson)
    } yield result

    case _ @ DELETE -> Root / name =>
      ProjectRepository.deleteProject(name).flatMap(_ => NoContent())

  }
}
