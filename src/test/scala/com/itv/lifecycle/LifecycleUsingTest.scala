package com.itv.lifecycle

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class LifecycleUsingTest extends AnyFunSuite with Matchers {

  test("Service running when using Lifecycle.using pattern") {
    var srv: Service = null
    Lifecycle.using(ServiceLifecycle) {service =>
      srv = service
      service should be(Symbol("running"))
    }
    srv shouldNot be(Symbol("running"))
  }

}

class Service {
  var serviceStarted = false

  def isRunning = serviceStarted
}

object ServiceLifecycle extends VanillaLifecycle[Service] {

  override def start(): Service = {
    val service = new Service
    service.serviceStarted = true
    service
  }

  override def shutdown(instance: Service): Unit = {
    instance.serviceStarted = false
  }
}
