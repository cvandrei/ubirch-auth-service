package com.ubirch.auth.util.db.config.defaults

import java.net.URI

import com.ubirch.auth.model.db.ContextProviderConfig

/**
  * author: cvandrei
  * since: 2017-03-14
  */
object OidcContextProvider {

  final val activeContexts: Seq[String] = Seq(
    ContextDefinitions.ctxTrackleDev,
    ContextDefinitions.ctxUbirchAdminUIDev,
    ContextDefinitions.ctxTrackleAdminUIDev
  )
  final val contextProviderList: Seq[ContextProviderConfig] = Seq(
    trackleDevGoogle, trackleDevYahoo,
    ubirchAdminUIDevYahoo,
    ContextDefinitions.trackleAdminUIDevYahoo
  )
  private lazy val trackleDevGoogle: ContextProviderConfig = ContextProviderConfig(
    context = ContextDefinitions.ctxTrackleDev,
    provider = OidcProviders.providerIdGoogle,
    clientId = "370115332091-kqf5hu698s4sodrvv03ka3bule530rp5.apps.googleusercontent.com",
    clientSecret = "M86oj4LxV-CcEDd3ougKSbsV",
    callbackUrl = new URI("https://localhost:10000/oidc-callback-google")
  )
  private lazy val trackleDevYahoo: ContextProviderConfig = ContextProviderConfig(
    context = ContextDefinitions.ctxTrackleDev,
    provider = OidcProviders.providerIdYahoo,
    clientId = "dj0yJmk9eWdKUGRJM01KclhqJmQ9WVdrOVlrMUVSRTF3TlRBbWNHbzlNQS0tJnM9Y29uc3VtZXJzZWNyZXQmeD1mMA--",
    clientSecret = "069dd28f2144ed043dcb70e27f99e424369c3040",
    callbackUrl = new URI("https://example.com/oidc-callback-yahoo")
  )
  private lazy val ubirchAdminUIDevYahoo: ContextProviderConfig = ContextProviderConfig(
    context = ContextDefinitions.ctxUbirchAdminUIDev,
    provider = OidcProviders.providerIdGoogle,
    clientId = "370115332091-q7dccmh0leq20rqgs2550vp0u67pj42p.apps.googleusercontent.com",
    clientSecret = "BFmveucyPZ9Ijt31UvYocrj4",
    callbackUrl = new URI("http://localhost:9000/auth?providerId=google")
  )

}

object ContextDefinitions {

  /*
   * context = trackle-admin-ui-dev
   *********************************************************************/
  val ctxTrackleAdminUIDev = "trackle-admin-ui-dev"

  /*
   * context = trackle-dev
   *********************************************************************/
  final val trackleAdminUIDevYahoo: ContextProviderConfig = ContextProviderConfig(
    context = ctxTrackleAdminUIDev,
    provider = OidcProviders.providerIdGoogle,
    clientId = "370115332091-dqhiaemv68bjvtnp84beg26plrpkmc8t.apps.googleusercontent.com",
    clientSecret = "n3fPuja818436VmggJZSht6-",
    callbackUrl = new URI("http://localhost:9100/auth?providerId=google")
  )
  final val ctxTrackleDev = "trackle-dev"

  /*
   * context = ubirch-admin-ui-dev
   *********************************************************************/
  final val ctxUbirchAdminUIDev = "ubirch-admin-ui-dev"

}