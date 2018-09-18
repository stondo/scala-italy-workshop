package org.typelevel.workshop.http

import cats.effect.IO
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.HttpService
import org.http4s.dsl.io._
import org.http4s.circe.CirceEntityCodec._
import org.typelevel.workshop.model.User
import org.typelevel.workshop.repository.UserRepository

object UserService {

  case class CreateUserRequest(name: String, email: String)

  val service: HttpService[IO] = HttpService[IO] {

    case GET -> Root / username =>
      UserRepository.findByUsername(username).flatMap {
        case Some(user) => Ok(user)
        case None => NotFound(s"No user found: $username".asJson)
      }

    case GET -> Root =>
      UserRepository.findAll().flatMap {
        case h::Nil => Ok(h)
        case h::t => Ok(h::t)
        case Nil => NotFound(s"No user found: ".asJson)
      }

    case req @ PUT -> Root => for {
      updatedUser <- req.as[User]
      affectedRows <- UserRepository.updateUser(updatedUser.id, updatedUser.username, updatedUser.email)
      result <- if (affectedRows > 0) Created(affectedRows)
                else BadRequest("Error updating project".asJson)

    } yield result

    case req @ POST -> Root => for {
      createUser <- req.as[CreateUserRequest]
      userOption <- UserRepository.addUser(createUser.name, createUser.email)
      result <- userOption match {
        case Some(user) => Created(user)
        case None => BadRequest("User already exists".asJson)
      }
    } yield result

    case _ @ DELETE -> Root / username =>
      UserRepository.deleteUser(username).flatMap(_ => NoContent())


  }
}
