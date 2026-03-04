package simulations

import io.gatling.javaapi.core.*
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.*

class BasicSimulation : Simulation() {

    val httpProtocol = http
        .baseUrl("https://httpbin.org")
        .acceptHeader("application/json")

    val scn = scenario("Basic Scenario")
        .exec(
            http("GET /get")
                .get("/get")
                .check(status().`is`(200))
        )

    init {
        setUp(
            scn.injectOpen(
                rampUsers(50).during(30)
            )
        ).protocols(httpProtocol)
    }
}