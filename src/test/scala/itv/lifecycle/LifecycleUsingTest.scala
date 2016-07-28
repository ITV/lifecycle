package itv.lifecycle

import org.scalatest.FunSuite
import org.scalatest.Matchers._

class LifecycleUsingTest extends FunSuite {

  test("Service running when using Lifecycle.using pattern") {
    var srv: Service = null
    Lifecycle.using(ServiceLifecycle) {service =>
      srv = service
      service should be('running)
    }
    srv shouldNot be('running)
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

  override def shutdown(instance: Service) {
    instance.serviceStarted = false
  }
}
