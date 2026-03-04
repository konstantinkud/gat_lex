package simulations

import io.gatling.javaapi.core.*
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.Properties

object KafkaClient {
    val producer: KafkaProducer<String, String> by lazy {
        val props = Properties().apply {
            put("bootstrap.servers", "192.168.0.216:9092")
            put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
            put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
            put("acks", "1")
        }
        KafkaProducer(props)
    }
}

class KafkaSimulation : Simulation() {

    val httpProtocol = http.baseUrl("https://httpbin.org")

    val scn = scenario("Kafka Producer Scenario")
        // Kafka send — трекается вручную через время
        .exec { session ->
            val userId = session.userId()
            val message = """{"userId": $userId, "event": "load_test", "ts": ${System.currentTimeMillis()}}"""
            KafkaClient.producer.send(ProducerRecord("load-test-topic", userId.toString(), message))
            session
        }
        // Минимальный HTTP-запрос чтобы Gatling писал отчёты и метрики
        .exec(
            http("Kafka send marker")
                .get("/status/200")
                .check(status().`is`(200))
        )
        .pause(1)

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            KafkaClient.producer.flush()
            KafkaClient.producer.close()
        })

        setUp(
            scn.injectOpen(
                rampUsers(50).during(30)
            )
        ).protocols(httpProtocol)
    }
}