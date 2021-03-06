package com.ubirch.auth.server.route

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.auth.config.Config
import com.ubirch.auth.core.actor.util.ActorNames
import com.ubirch.auth.core.actor.{GetProviderInfoList, ProviderInfoActor, ProviderInfoList}
import com.ubirch.auth.util.server.RouteConstants
import com.ubirch.util.http.response.ResponseUtil
import com.ubirch.util.rest.akka.directives.CORSDirective

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * author: cvandrei
  * since: 2017-01-26
  */
class ProviderRoute(implicit system: ActorSystem) extends ResponseUtil
  with CORSDirective
  with StrictLogging {

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val timeout: Timeout = Timeout(Config.actorTimeout seconds)

  private val providerInfoActor = system.actorOf(ProviderInfoActor.props(), ActorNames.PROVIDER_INFO)

  val route: Route = {

    pathPrefix(RouteConstants.providerInfo / RouteConstants.list) {

      path(Segment /Segment) { (context, appId) =>

        providerInfoList(context, appId)

      }

    }

  }

  private def providerInfoList(context: String, appId: String) = {

    respondWithCORS {

      get {
        onComplete(providerInfoActor ? GetProviderInfoList(context = context, appId = appId)) {

          case Failure(t) =>
            logger.error("provider info list call responded with an unhandled message (check ProviderRoute for bugs!!!)", t)
            complete(serverErrorResponse(errorType = "ServerError", errorMessage = "sorry, something went wrong on our end"))

          case Success(resp) =>

            resp match {
              case providerInfos: ProviderInfoList => complete(providerInfos.seq)
              case _ => complete(serverErrorResponse(errorType = "QueryError", errorMessage = "failed to query provider info list"))
            }

        }
      }

    }

  }

}
