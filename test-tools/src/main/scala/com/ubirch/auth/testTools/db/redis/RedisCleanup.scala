package com.ubirch.auth.testTools.db.redis

import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.auth.config.ConfigKeys
import com.ubirch.util.redis.RedisClientUtil

import akka.actor.ActorSystem
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * author: cvandrei
  * since: 2017-03-14
  */
trait RedisCleanup extends StrictLogging {

  final def deleteAll(prefix: String = "", sleepAfter: Long = 500): Unit = {

    implicit val system = ActorSystem()
    implicit val timeout = Timeout(15 seconds)

    val finalPrefix = if (prefix == "") {
      "*"
    } else {
      s"$prefix.*"
    }

    logger.info(s"====== delete: prefix=$finalPrefix")
    val redis = RedisClientUtil.newInstance(ConfigKeys.CONFIG_PREFIX)(system)
    redis.keys(finalPrefix) map { keysList =>
      keysList foreach { key =>
        logger.info(s"delete: key=$key")
        redis.del(key)
      }
    }
    Thread.sleep(sleepAfter)

    system.terminate()

  }

}
