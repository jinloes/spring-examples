package com.jinloes

import io.gatling.core.Predef._
import io.gatling.core.json.Json
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder.toActionBuilder
import java.util.UUID

class AlarmTest extends Simulation {
  before {
    println("***** My simulation is about to begin! *****")
  }

  after {
    println("***** My simulation has ended! ******")
  }

  val httpProtocol = http.baseURL("http://glx-eventservice-env.tjgjnifait.us-west-1.elasticbeanstalk.com:5000")
  val headers = Map("Content-Type" -> "application/json")
  val alarmData = Map("ipProtocol" -> "UDP", "local_port" -> 123)
  val requestBody = Map("vCenterUuid" -> "3d7c1571-5103-4777-a165-c853590e7ee9",
    "vmUuid" -> "df2ec31b-fd9e-482d-8c1b-945f9be7790c", "type" -> "outboundConnection",
    "timestamp" -> "2017-10-27T15:07:12Z", "alarmData" -> alarmData)
  val scn = scenario("Scenario1")
    .exec(
      http("Create Alarm")
        .post("/alarms/nothing")
        .body(StringBody(Json.stringify(requestBody)))
        .headers(headers)
    )

  setUp(
    scn.inject(
      rampUsersPerSec(1.0).to(1000.0)
        .during(new DurationInteger(5).seconds),
      constantUsersPerSec(1000.0).during(new DurationInteger(30).seconds),
      rampUsersPerSec(5000.0).to(1.0)
        .during(new DurationInteger(5).seconds)
    )
  ).protocols(httpProtocol)
}

class AlarmThroughputTest extends Simulation {
  before {
    println("***** My simulation is about to begin! *****")
  }

  after {
    println("***** My simulation has ended! ******")
  }

  val httpProtocol = http.baseURL("http://glx-eventservice-env.tjgjnifait.us-west-1.elasticbeanstalk.com:5000")
    .connectionHeader("keep-alive")
    .shareConnections
  val headers = Map("Content-Type" -> "application/json")
  val createAlarm = exec(
    http("Create Alarm")
      .post("/alarms/nothing")
      .body(StringBody(
        s"""
           | {
           |   "vCenterUuid": "${UUID.randomUUID().toString}",
           |   "vmUuid": "${UUID.randomUUID().toString}",
           |   "type": "outboundConnection",
           |   "timestamp": "2017-10-27T15:07:12Z",
           |   "alarmData": {
           |      "ipProtocol": "UDP",
           |      "local_post": 123
           |    }
           | }
        """.stripMargin))
      .headers(headers)
  )
  val scn = scenario("Constant Throughput")
    //.repeat(1) {
    .exec(createAlarm)
  //}

  setUp(
    scn.inject(
      //rampUsers(50).over(new DurationInteger(1).seconds)
      constantUsersPerSec(2500).during(new DurationInteger(30).seconds),
      //rampUsersPerSec(500).to(2000).during(new DurationInteger(90).seconds)
    )
  ).protocols(httpProtocol)
}

class AlarmKafkaThroughputTest extends Simulation {
  before {
    println("***** My simulation is about to begin! *****")
  }

  after {
    println("***** My simulation has ended! ******")
  }

  val httpProtocol = http.baseURL("http://glx-eventservice-env.tjgjnifait.us-west-1.elasticbeanstalk.com")
    .connectionHeader("keep-alive")
    .shareConnections
  val headers = Map("Content-Type" -> "application/json")
  val createAlarm = exec(
    http("Create Alarm")
      .post("/alarms/kafka")
      .body(StringBody(
        s"""
           | {
           |   "vCenterUuid": "${UUID.randomUUID().toString}",
           |   "vmUuid": "${UUID.randomUUID().toString}",
           |   "type": "outboundConnection",
           |   "timestamp": "2017-10-27T15:07:12Z",
           |   "alarmData": {
           |      "ipProtocol": "UDP",
           |      "local_post": 123
           |    }
           | }
        """.stripMargin))
      .headers(headers)
  )
  val scn = scenario("Constant Throughput")
    .repeat(300) {
      exec(createAlarm)
    }

  setUp(
    scn.inject(
      atOnceUsers(500)
      //rampUsers(50).over(new DurationInteger(1).seconds)
      //constantUsersPerSec(1000).during(new DurationInteger(60).seconds),
      //rampUsersPerSec(500).to(2000).during(new DurationInteger(90).seconds)
    )
  ).protocols(httpProtocol)
}

class AlarmKafkaDirectThroughputTest extends Simulation {
  before {
    println("***** My simulation is about to begin! *****")
  }

  after {
    println("***** My simulation has ended! ******")
  }

  val httpProtocol = http.baseURL("http://glx-eventservice-env.tjgjnifait.us-west-1.elasticbeanstalk.com:5000")
    .connectionHeader("keep-alive")
    .shareConnections
  val headers = Map("Content-Type" -> "application/json")
  val createAlarm = exec(
    http("Create Alarm")
      .post("/alarms/kafka")
      .body(StringBody(
        s"""
           | {
           |   "vCenterUuid": "${UUID.randomUUID().toString}",
           |   "vmUuid": "${UUID.randomUUID().toString}",
           |   "type": "outboundConnection",
           |   "timestamp": "2017-10-27T15:07:12Z",
           |   "alarmData": {
           |      "ipProtocol": "UDP",
           |      "local_post": 123
           |    }
           | }
        """.stripMargin))
      .headers(headers)
  )
  val scn = scenario("Constant Throughput")
    .repeat(300) {
      exec(createAlarm)
    }

  setUp(
    scn.inject(
      atOnceUsers(500)
      //rampUsers(50).over(new DurationInteger(1).seconds)
      //constantUsersPerSec(1000).during(new DurationInteger(60).seconds),
      //rampUsersPerSec(500).to(2000).during(new DurationInteger(90).seconds)
    )
  ).protocols(httpProtocol)
}

class AlarmPostgresThroughputTest extends Simulation {
  before {
    println("***** My simulation is about to begin! *****")
  }

  after {
    println("***** My simulation has ended! ******")
  }

  val httpProtocol = http.baseURL("http://glx-eventservice-env.tjgjnifait.us-west-1.elasticbeanstalk.com")
    .connectionHeader("keep-alive")
    .shareConnections
  val headers = Map("Content-Type" -> "application/json")
  val createAlarm = exec(
    http("Create Alarm")
      .post("/alarms")
      .body(StringBody(
        s"""
           | {
           |   "vCenterUuid": "${UUID.randomUUID().toString}",
           |   "vmUuid": "${UUID.randomUUID().toString}",
           |   "type": "outboundConnection",
           |   "timestamp": "2017-10-27T15:07:12Z",
           |   "alarmData": {
           |      "ipProtocol": "UDP",
           |      "local_post": 123
           |    }
           | }
        """.stripMargin))
      .headers(headers)
  )
  val scn = scenario("Constant Throughput")
    .repeat(300) {
    exec(createAlarm)
  }

  setUp(
    scn.inject(
      atOnceUsers(500)
      //rampUsers(50).over(new DurationInteger(1).seconds)
      //constantUsersPerSec(2500).during(new DurationInteger(30).seconds),
      //rampUsersPerSec(500).to(2000).during(new DurationInteger(90).seconds)
    )
  ).protocols(httpProtocol)
}

class AlarmPostgresDirectThroughputTest extends Simulation {
  before {
    println("***** My simulation is about to begin! *****")
  }

  after {
    println("***** My simulation has ended! ******")
  }

  val httpProtocol = http.baseURL("http://glx-eventservice-env.tjgjnifait.us-west-1.elasticbeanstalk.com:5000")
    .connectionHeader("keep-alive")
    .shareConnections
  val headers = Map("Content-Type" -> "application/json")
  val createAlarm = exec(
    http("Create Alarm")
      .post("/alarms")
      .body(StringBody(
        s"""
           | {
           |   "vCenterUuid": "${UUID.randomUUID().toString}",
           |   "vmUuid": "${UUID.randomUUID().toString}",
           |   "type": "outboundConnection",
           |   "timestamp": "2017-10-27T15:07:12Z",
           |   "alarmData": {
           |      "ipProtocol": "UDP",
           |      "local_post": 123
           |    }
           | }
        """.stripMargin))
      .headers(headers)
  )
  val scn = scenario("Constant Throughput")
    .repeat(300) {
      exec(createAlarm)
    }

  setUp(
    scn.inject(
      atOnceUsers(500)
      //rampUsers(50).over(new DurationInteger(1).seconds)
      //constantUsersPerSec(2500).during(new DurationInteger(30).seconds),
      //rampUsersPerSec(500).to(2000).during(new DurationInteger(90).seconds)
    )
  ).protocols(httpProtocol)
}

class NonEbsAlarmKafkaThroughputTest extends Simulation {
  before {
    println("***** My simulation is about to begin! *****")
  }

  after {
    println("***** My simulation has ended! ******")
  }

  val httpProtocol = http.baseURL("http://54.153.4.67:5000")
    .connectionHeader("keep-alive")
    .shareConnections
  val headers = Map("Content-Type" -> "application/json")
  val createAlarm = exec(
    http("Create Alarm")
      .post("/alarms/kafka")
      .body(StringBody(
        s"""
           | {
           |   "vCenterUuid": "${UUID.randomUUID().toString}",
           |   "vmUuid": "${UUID.randomUUID().toString}",
           |   "type" -> "outboundConnection",
           |   "timestamp" -> "2017-10-27T15:07:12Z",
           |   "alarmData": {
           |      "ipProtocol": "UDP",
           |      local_post": 123
           |    }
           | }
        """.stripMargin))
      .headers(headers)
  )
  val scn = scenario("Constant Throughput")
    .repeat(300) {
    exec(createAlarm)
  }

  setUp(
    scn.inject(
      atOnceUsers(500)
      //rampUsers(50).over(new DurationInteger(1).seconds)
      //constantUsersPerSec(5000).during(new DurationInteger(60).seconds),
      //rampUsersPerSec(500).to(2000).during(new DurationInteger(90).seconds)
    )
  ).protocols(httpProtocol)
}