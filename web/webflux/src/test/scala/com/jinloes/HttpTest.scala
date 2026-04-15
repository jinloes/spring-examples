package com.jinloes

import io.gatling.core.Predef._
import io.gatling.core.json.Json
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder.toActionBuilder

class HttpTest extends Simulation {
  before {
    println("***** My simulation is about to begin! *****")
  }

  after {
    println("***** My simulation has ended! ******")
  }

  val httpProtocol = http.baseURL("http://54.183.217.145:8080")
  val headers = Map("Content-Type" -> "application/json")
  val requestBody = Map("name" -> "name1", "address" -> "address 1")
  val scn = scenario("Scenario1")
    .exec(
      http("Create Address")
        .post("/addresses")
        .body(StringBody(Json.stringify(requestBody)))
        .headers(headers)
    )

  setUp(
    scn.inject(
      constantUsersPerSec(1000) during new DurationInteger(1).minutes
    )
  ).protocols(httpProtocol)
}