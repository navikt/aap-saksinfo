package saksinfo.kafka

import kotlinx.coroutines.runBlocking
import no.nav.aap.kafka.streams.v2.KeyValue
import no.nav.aap.kafka.streams.v2.Topology
import saksinfo.arena.ArenaoppslagRestClient
import saksinfo.arena.FinnesVedtakKafkaDTO
import saksinfo.arena.Response

fun topology(arenaoppslagRestClient: ArenaoppslagRestClient): Topology = no.nav.aap.kafka.streams.v2.topology {

    val vedtaksTable = consume(Tables.vedtak)

    val skalJoineMedVedtaksTopic: (value: FinnesVedtakKafkaDTO) -> Boolean =
        { kafkaDTO -> kafkaDTO.req.sjekkKelvin }
    consume(Topics.sisteVedtak)
        .filter { kafkaDTO -> kafkaDTO.res == null }
        .branch(skalJoineMedVedtaksTopic) { chain ->
            chain.leftJoinWith(vedtaksTable)
                .mapKeyValue { personident, sisteVedtakKafkaDTO, vedtakKafkaDTO ->
                    val finnesIArena = when (sisteVedtakKafkaDTO.req.sjekkArena) {
                        true -> runBlocking { arenaoppslagRestClient.hentVedtak(personident) }.isNotEmpty()
                        false -> null
                    }
                    val svar = sisteVedtakKafkaDTO.copy(res = Response(vedtakKafkaDTO != null, finnesIArena))
                    KeyValue(personident, svar)
                }
                .filter { kafkaDTO -> kafkaDTO.res != null }
                .produce(Topics.sisteVedtak)
        }
        .default { chain ->
            chain.map { personident, kafkaDTO ->
                if (kafkaDTO.req.sjekkArena) {
                    val respons = runBlocking { arenaoppslagRestClient.hentVedtak(personident) }
                    kafkaDTO.copy(res = Response(null, respons.isNotEmpty()))
                } else kafkaDTO
            }
                .filter { kafkaDTO -> kafkaDTO.res != null }
                .produce(Topics.sisteVedtak)
        }
}