package com.itv.lifecycle

/** Trait for classes that provide services that require shutting down after use.
  *
  * Supports monadic operations so that Services can be used and combined like the following:
  * {{{
  *   for {
  *     executionContext <- FixedThreadPoolExecutorService(numThreads = 4)
  *     httpServer <- HttpServerService(port = 8080)
  *   } {
  *     respondToRequests(httpServer, executionContext)
  *   }
  * }}}
  *
  * @tparam T The public type this lifecycle provides
  */
trait Lifecycle[+T] {

  type ServiceInstance

  def start(): ServiceInstance

  def unwrap(instance: ServiceInstance): T

  def shutdown(instance: ServiceInstance): Unit

  final def flatMap[U](f: T => Lifecycle[U]): Lifecycle[U] = new FlatMapLifecycle(this, f)

  final def map[U](f: T => U): Lifecycle[U] = flatMap(t => NoOpLifecycle(f(t)))

  final def foreach(block: T => Unit): Unit = {
    val instance = start()
    try {
      block(unwrap(instance))
    } finally {
      shutdown(instance)
    }
  }

  final def runUntilJvmShutdown(): Unit = {
    val instance = start()
    sys.addShutdownHook(shutdown(instance))
  }
}

object Lifecycle {

  def using[T, S](lifecycle: Lifecycle[T])(block: T => S): S = {
    val instance = lifecycle.start()
    try {
      block(lifecycle.unwrap(instance))
    } finally {
      lifecycle.shutdown(instance)
    }
  }

  def sequence[T](lifecycles: List[Lifecycle[T]]): Lifecycle[List[T]] = {
    lifecycles.foldLeft[Lifecycle[List[T]]](NoOpLifecycle(List()))((acc, lifecycle) => acc.flatMap[List[T]] { list =>
      lifecycle.map(a => a :: list)
    }).map(_.reverse)
  }

}

class FlatMapLifecycle[A, B](a: Lifecycle[A], f: A => Lifecycle[B]) extends Lifecycle[B] {

  trait ServiceInstance {
    def unwrap: B

    def shutdown(): Unit
  }

  override def start(): ServiceInstance = {
    val ai = a.start()
    try {
      val b = f(a.unwrap(ai))
      val bi = b.start()
      new ServiceInstance {

        def unwrap: B = b.unwrap(bi)

        def shutdown(): Unit = {
          try {
            b.shutdown(bi)
          } finally {
            a.shutdown(ai)
          }
        }
      }
    } catch {
      case ex: Throwable =>
        a.shutdown(ai)
        throw ex
    }
  }

  override def shutdown(instance: ServiceInstance): Unit = instance.shutdown()

  override def unwrap(instance: ServiceInstance): B = instance.unwrap
}

trait VanillaLifecycle[T] extends Lifecycle[T] {

  final type ServiceInstance = T

  final override def unwrap(instance: ServiceInstance): T = instance
}

/**
 * Trivial lifecycle that provides a single value and does nothing on shutdown.
 * @param value The value this lifecycle should provide
 * @tparam T The type of the value this lifecycle provides
 */
case class NoOpLifecycle[T](value: T) extends VanillaLifecycle[T] {

  override def start(): T = value

  override def shutdown(instance: T): Unit = {}
}
