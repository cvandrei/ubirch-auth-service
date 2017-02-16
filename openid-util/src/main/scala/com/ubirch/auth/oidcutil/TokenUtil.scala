package com.ubirch.auth.oidcutil

import java.io.IOException
import java.net.{URI, URL}

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.RemoteJWKSet
import com.nimbusds.jose.proc.{JWSVerificationKeySelector, SimpleSecurityContext}
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import com.nimbusds.jwt.{JWT, JWTClaimsSet}
import com.nimbusds.oauth2.sdk.auth.{ClientSecretPost, Secret}
import com.nimbusds.oauth2.sdk.http.HTTPResponse
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.{AuthorizationCode, AuthorizationCodeGrant, ParseException, SerializeException, TokenErrorResponse, TokenRequest}
import com.nimbusds.openid.connect.sdk.{OIDCTokenResponse, OIDCTokenResponseParser}
import com.typesafe.scalalogging.slf4j.StrictLogging

import com.ubirch.auth.config.Config

/**
  * author: cvandrei
  * since: 2017-02-14
  */
object TokenUtil extends StrictLogging {

  def requestToken(provider: String, authCode: String): Option[TokenUserId] = {

    sendTokenRequest(provider, authCode) match {

      case None => None

      case Some(tokenHTTPResp) =>

        try {

          OIDCTokenResponseParser.parse(tokenHTTPResp) match {

            case error: TokenErrorResponse =>
              logger.error(s"oidc code verification failed (provider replied with an error): ${tokenHTTPResp.getStatusCode} - ${tokenHTTPResp.getContent} - ${error.toJSONObject.toJSONString}")
              None

            case accessTokenResponse: OIDCTokenResponse =>

              val accessToken = accessTokenResponse.getOIDCTokens.getAccessToken
              val idToken = accessTokenResponse.getOIDCTokens.getIDToken

              verifyIdToken(provider, idToken) match {

                case None =>
                  logger.error(s"failed to extract userId from JWT: provider=$provider")
                  logger.debug(s"accessToken=$accessToken, userId=$None, idToken=${idToken.getParsedString}")
                  None

                case Some(claims) =>
                  val userId = claims.getSubject
                  logger.debug(s"accessToken=$accessToken, userId=$userId, idToken=${idToken.getParsedString}")
                  Some(TokenUserId(accessToken.getValue, userId))

              }

          }

        } catch {
          case e: ParseException =>
            logger.error(s"oidc code verification failed (failed to parse response from provider)", e)
            None
        }

    }

  }

  private def sendTokenRequest(provider: String, authCode: String): Option[HTTPResponse] = {

    try {

      val tokenReq = tokenRequest(provider, authCode)
      Some(tokenReq.toHTTPRequest.send())

    } catch {

      case se: SerializeException =>
        logger.error(s"failed to send oidc code verification request (SerializeException)", se)
        None

      case e: IOException =>
        logger.error(s"failed to send oidc code verification request (SerializeException)", e)
        None
    }

  }

  private def tokenRequest(provider: String, authCode: String): TokenRequest = {

    val redirectUri = new URI(Config.oidcCallbackUrl(provider))
    val grant = new AuthorizationCodeGrant(new AuthorizationCode(authCode), redirectUri)

    val tokenEndpoint = new URI(Config.oidcTokenEndpoint(provider))
    logger.debug(s"token endpoint: provider=$provider, url=$tokenEndpoint")

    val clientId = new ClientID(Config.oidcClientId(provider))
    val secret = new Secret(Config.oidcClientSecret(provider))
    val auth = new ClientSecretPost(clientId, secret)

    new TokenRequest(tokenEndpoint, auth, grant)

  }

  private def verifyIdToken(provider: String, idToken: JWT): Option[JWTClaimsSet] = {

    // TODO handle exceptions
    jwtProcessor(provider, idToken) match {

      case None =>
        logger.error(s"failed to load jwtProcessor: provider=$provider")
        None

      case Some(jwtProcessor) =>
        val ctx: SimpleSecurityContext = new SimpleSecurityContext()
        Some(jwtProcessor.process(idToken, ctx))

    }

  }

  private def jwtProcessor(provider: String, idToken: JWT): Option[DefaultJWTProcessor[SimpleSecurityContext]] = {

    // TODO handle exceptions
    val jwtProcessor = new DefaultJWTProcessor[SimpleSecurityContext]()
    val keySource = new RemoteJWKSet[SimpleSecurityContext](new URL(Config.oidcJwksUri(provider)))
    val algorithm = idToken.getHeader.getAlgorithm

    if (Config.oidcTokenSigningAlgorithms(provider) contains algorithm.getName) {

      val requirement = algorithm.getRequirement
      val jwsAlg: JWSAlgorithm = new JWSAlgorithm(algorithm.getName, requirement)
      val keySelector: JWSVerificationKeySelector[SimpleSecurityContext] =
        new JWSVerificationKeySelector[SimpleSecurityContext](jwsAlg, keySource)
      jwtProcessor.setJWSKeySelector(keySelector)

      Some(jwtProcessor)

    } else {

      logger.error(s"signing algorithm does not match those allowed by our configuration: provider=$provider, algorithm=$algorithm")
      None

    }

  }

}

case class TokenUserId(token: String, userId: String)
