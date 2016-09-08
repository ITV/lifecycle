package com.itv.lifecycle

import java.util.concurrent.{ScheduledExecutorService, Executors, ExecutorService}

import scala.concurrent.ExecutionContext

object ExecutorLifecycles {

  val singleThreadScheduledExecutor: Lifecycle[ScheduledExecutorService] = lifecycleFrom(Executors.newSingleThreadScheduledExecutor())

  val singleThreadExecutionContext: Lifecycle[ExecutionContext] = singleThreadScheduledExecutor.map(ExecutionContext.fromExecutor)

  private def lifecycleFrom[T <: ExecutorService](createAndStart: => T) = new VanillaLifecycle[T] {

    override def start() = createAndStart

    override def shutdown(executorService: T): Unit = executorService.shutdown()
  }
}
