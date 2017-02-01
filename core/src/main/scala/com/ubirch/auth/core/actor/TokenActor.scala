package com.ubirch.auth.core.actor

import com.ubirch.auth.core.manager.TokenManager
import com.ubirch.auth.model.{AfterLogin, Token}
import com.ubirch.util.model.JsonErrorResponse

import akka.actor.{Actor, ActorLogging}

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

/**
  * author: cvandrei
  * since: 2017-01-31
  */
class TokenActor extends Actor
  with ActorLogging {

  implicit val executionContext: ExecutionContextExecutor = context.dispatcher

  override def receive: Receive = {

    case afterLogin: AfterLogin =>

      val sender = context.sender()
      TokenManager.verifyCode(afterLogin).onComplete {

        case Success(tokenOpt: Option[Token]) =>
          tokenOpt match {
            case Some(token) => sender ! token
            case None => sender ! JsonErrorResponse(errorType = "VerificationError", errorMessage = "invalid code")
          }

        case Failure(t) =>
          log.error(t, s"code verification failed: afterLogin=$afterLogin")
          sender ! JsonErrorResponse(errorType = "ServerError", errorMessage = "code verification failed")

      }

    case _ =>
      log.error("unknown message")
      sender ! JsonErrorResponse(errorType = "UnknownMessage", errorMessage = "unable to handle message")

  }

}
