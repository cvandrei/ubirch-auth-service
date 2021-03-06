package com.ubirch.auth.core.actor

import com.ubirch.auth.config.Config
import com.ubirch.auth.core.manager.LogoutManager
import com.ubirch.util.model.JsonErrorResponse

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

/**
  * author: cvandrei
  * since: 2017-02-01
  */
class LogoutActor extends Actor
  with ActorLogging {

  implicit val executionContext: ExecutionContextExecutor = context.dispatcher

  override def receive: Receive = {

    case logout: Logout =>

      val sender = context.sender()
      LogoutManager.logout(logout.token).onComplete {

        case Success(status: Boolean) => sender ! status

        case Failure(t) =>
          log.error(t, s"failed to logout token: $logout")
          sender ! JsonErrorResponse(errorType = "ServerError", errorMessage = "logout failed")

      }

  }

  override def unhandled(message: Any): Unit = {
    log.error(s"received from ${context.sender().path} unknown message: ${message.toString} (${message.getClass})")
    context.sender ! JsonErrorResponse(errorType = "UnknownMessage", errorMessage = "unable to handle message")
  }

}

object LogoutActor {
  def props(): Props = new RoundRobinPool(Config.akkaNumberOfWorkers).props(Props[LogoutActor])
}

case class Logout(token: String)
