package itv.lifecycle

import java.util.concurrent.TimeUnit

import org.scalatest.FunSuite
import org.scalatest.Matchers._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Promise, Future}

class ExecutorLifecyclesTest extends FunSuite {

  test("Can provide ScheduledExecutor for running scheduled tasks") {
    var state = "A"

    def transitionState(fromToPair: (String, String)) = new Runnable {
      override def run(): Unit = {
        state should be(fromToPair._1)
        state = fromToPair._2
      }
    }

    for (executorService <- ExecutorLifecycles.singleThreadScheduledExecutor) {
      // Run in the distant future
      val future = executorService.schedule(transitionState("B" -> "C"), 100, TimeUnit.MILLISECONDS)

      // Run in the near future
      executorService.execute(transitionState("A" -> "B"))

      future.get()
      state should be("C")
    }
  }

  test("Can provide ExecutionContext for use with e.g. Scala Futures") {
    val waitForMe = Promise[String]()

    for (executionContext <- ExecutorLifecycles.singleThreadExecutionContext) {
      implicit def ec = executionContext

      val future = Future("A").map(_ + "B").flatMap(ab => waitForMe.future.map(ab + _))

      future.value should be(None)
      waitForMe.success("C")
      Await.result(future, FiniteDuration(100, TimeUnit.MILLISECONDS)) should be("ABC")
    }
  }
}
