package com.ubirch.auth.core

/**
  * author: cvandrei
  * since: 2017-02-09
  */
object OidcUtil {

  def stateToHashedKey(provider: String, state: String): String = s"state:$provider:$state"

}
