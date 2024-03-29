package saksinfo

import no.nav.aap.kafka.streams.v2.config.StreamsConfig
import no.nav.aap.ktor.client.AzureConfig
import java.net.URL

data class Config(
    val kafka: StreamsConfig,
    val azure: AzureConfig,
    val arenaoppslag: ArenaoppslagConfig,
    val oauth: OauthConfig
)

data class OauthConfig(
    val azure: IssuerConfig
)

data class IssuerConfig(
    val issuer: String,
    val audience: String,
    val jwksUrl: URL
)

data class ArenaoppslagConfig(
    val proxyBaseUrl:String,
    val scope: String
)
