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

    case req @ PUT -> Root => for {
      updateNameDesc <- req.as[ProjectWithoutOwner]
      affectedRows <- ProjectRepository.updateNameAndDesc(updateNameDesc.id, updateNameDesc.name, updateNameDesc.description)
      result <- if (affectedRows > 0) Created(affectedRows)
                else BadRequest("Error updating project".asJson)
    } yield result

    case req @ DELETE -> Root / name =>
      ProjectRepository.deleteProject(name).flatMap(_ => NoContent())

  }
}
