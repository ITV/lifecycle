package com.itv.lifecycle

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable.ListBuffer

class LifecycleTest extends AnyFunSuite with Matchers {

  test("Lifecycles can be reused") {
    val events = ListBuffer[Event]()
    val Lifecycle = new StubLifecycle("A", events)

    for (aName <- Lifecycle) {
      aName should be("A1")
    }
    for (aName <- Lifecycle) {
      aName should be("A2")
    }
    events should be(ListBuffer(
      Started("A1"),
      Shutdown("A1"),
      Started("A2"),
      Shutdown("A2")
    ))
  }

  test("Lifecycle values can be mapped") {
    val events = ListBuffer[Event]()
    val originalLifecycle = new StubLifecycle("A", events)
    val newLifecycle = for (originalValue <- originalLifecycle) yield "NEW_" + originalValue

    for (newValue <- newLifecycle) {
      newValue should be("NEW_A1")
    }
    events should be(ListBuffer(
      Started("A1"),
      Shutdown("A1")
    ))
  }

  test("FlatMap: Normal sequence of events is start A, start B, (do something), shutdown B, shutdown A") {
    val events = ListBuffer[Event]()
    for {
      aName <- new StubLifecycle("A", events)
      bName <- new StubLifecycle(aName + "_B", events)
    } {
      aName should be("A1")
      bName should be("A1_B1")
      events should be(ListBuffer(
        Started("A1"),
        Started("A1_B1")
      ))
    }
    events should be(ListBuffer(
      Started("A1"),
      Started("A1_B1"),
      Shutdown("A1_B1"),
      Shutdown("A1")
    ))
  }

  test("FlatMap: A is shutdown if B does not start successfully") {
    val events = ListBuffer[Event]()

    val thrown = intercept[Exception] {
      for (aName <- new StubLifecycle("A", events); bName <- new StubLifecycle(aName + "_B", events) {
        override def start(): String = throw new Exception("Oops")
      }) {
        fail("Starting B Lifecycle should have thrown")
      }
    }
    thrown.getMessage should be("Oops")

    events should be(ListBuffer(
      Started("A1"),
      Shutdown("A1")
    ))
  }

  test("Lifecycles can be sequenced") {
    val lifecycles = List(
      NoOpLifecycle(1),
      NoOpLifecycle(2)
    )

    val resultLifecycle = Lifecycle.sequence(lifecycles)

    Lifecycle.using(resultLifecycle) { list =>
      list should be (List(1,2))
    }
  }

  sealed trait Event
  case class Started(name: String) extends Event
  case class Shutdown(name: String) extends Event

  class StubLifecycle(prefix: String, events: ListBuffer[Event]) extends VanillaLifecycle[String] {
    private var counter: Int = 0

    override def start(): String = {
      counter += 1
      val name = prefix + counter
      events += Started(name)
      name
    }

    override def shutdown(name: String): Unit = {
      events += Shutdown(name)
    }
  }
}
