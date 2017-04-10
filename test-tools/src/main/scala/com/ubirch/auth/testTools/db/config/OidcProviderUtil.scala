package com.ubirch.auth.testTools.db.config

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.auth.model.db.OidcProviderConfig
import com.ubirch.auth.model.db.redis.RedisKeys
import com.ubirch.auth.testTools.db.config.defaults.OidcProviders
import com.ubirch.util.futures.FutureUtil
import com.ubirch.util.json.MyJsonProtocol

import org.json4s.native.Serialization.write

import redis.RedisClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * author: cvandrei
  * since: 2017-03-14
  */
object OidcProviderUtil extends StrictLogging
  with MyJsonProtocol {

  // TODO scaladoc
  final def initProviders(activateProviders: Boolean = true,
                          sleepAfter: Long = 500
                         )
                         (implicit redis: RedisClient): Future[Map[String, OidcProviderConfig]] = {

    logger.info("====== create: providers")
    val providersStored = OidcProviders.providers map { providerConf =>

      storeProvider(providerConf, activateProviders) map { stored =>

        if (stored) {
          logger.info(s"created provider base conf: provider=${providerConf.id}")
        } else {
          logger.error(s"failed to create provider base conf: provider=${providerConf.id}")
        }
        providerConf

      }

    }

    Thread.sleep(sleepAfter)

    FutureUtil.unfoldInnerFutures(providersStored) map { seq =>
      seq.map(provider => provider.id -> provider).toMap
    }

  }

  // TODO scaladoc
  def storeProvider(provider: OidcProviderConfig,
                    activateProvider: Boolean = true
                   )
                   (implicit redis: RedisClient): Future[Boolean] = {

    val id = provider.id
    val json = write(provider)

    redis.set(RedisKeys.providerKey(id), json) flatMap { stored =>

      if (activateProvider) {

        redis.lpush[String](RedisKeys.OIDC_PROVIDER_LIST, id) map { activated =>
          stored && activated > 0
        }

      } else {
        Future(stored)
      }

    }

  }

  def deleteProvider(providerId: String,
                     sleepAfter: Long = 100
                    )
                    (implicit redis: RedisClient): Future[Boolean] = {

    val result = redis.del(RedisKeys.providerKey(providerId)) map(_ > 0)

    Thread.sleep(sleepAfter)
    result

  }

  def disableProvider(providerId: String,
                      sleepAfter: Long = 100
                     )
                     (implicit redis: RedisClient): Future[Boolean] = {

    val result = redis.lrem(RedisKeys.OIDC_PROVIDER_LIST, 1, providerId) map(_ > 0)

    Thread.sleep(sleepAfter)
    result

  }

}
