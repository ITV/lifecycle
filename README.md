Lifecycle
=====

A pattern for safe usage of resources.
Supports monadic operations so use can be composed.

A resource is anything that requires:

* Some intialization operations
* And/or some tear-down operations

to be performed before/after use. Examples include: files, HTTP servers, HTTP clients, actor systems, network connections, ...

It's a nice `try` / `finally` wrapper!

Lifecycle was born of frustration, aiming to help your codebase tidy up all used resources.

(Contrived) Example
=====

Meet `bob`, `fred`, and `barry`.

They each have a secret, but they'll only reveal it to you if you say hello first.
After you're done using them, you should say goodbye - it'd be pretty rude not to.

```scala
scala> case class Person(name: String, private val secret: String) {
     |     private var readyToTellSecret = false
     | 
     |     def hello(): Unit = {
     |         readyToTellSecret = true
     |         println("Said hello to " + name)
     |     }
     | 
     |     def revealSecret(): String = {
     |         require(readyToTellSecret, s"it's rude to ask $name a secret before you've said hello")
     |         secret
     |     }
     | 
     |     def goodbye(): Unit = {
     |         readyToTellSecret = false
     |         println("Said goodbye to " + name)
     |     }
     | }
defined class Person
```

```scala
scala> val bob = Person("Bob", secret = "I hate Barry")
bob: Person = Person(Bob,I hate Barry)

scala> val fred = Person("Fred", secret = "I hate Barry")
fred: Person = Person(Fred,I hate Barry)

scala> val barry = Person("Barry", secret = "I'm lonely")
barry: Person = Person(Barry,I'm lonely)
```

`bob`, `fred` and `barry` aren't going to win any prizes for lack of side-effects, but you've got to expect the unexpected when dealing with people.

You're gonna have a bad time if you ask `bob` his secret before saying hello:

```scala
scala> bob.revealSecret()
java.lang.IllegalArgumentException: requirement failed: it's rude to ask Bob a secret before you've said hello
  at scala.Predef$.require(Predef.scala:224)
  at Person.revealSecret(<console>:21)
  ... 350 elided
```

We always need to guarantee we interact with a `Person` in this manner:
```scala
scala> bob.hello()
Said hello to Bob

scala> bob.revealSecret()
res2: String = I hate Barry

scala> bob.goodbye()
Said goodbye to Bob
```

Defining a `Lifecycle`
----

Here's a `Lifecycle` that performs all the pleaseantries before/after using a `Person`.

```scala
scala> import itv.lifecycle._
import itv.lifecycle._

scala> def personInteraction(person: Person): Lifecycle[Person] =
     |     new VanillaLifecycle[Person] {
     |         override def start(): Person = {
     |             person.hello()
     |             person
     |         }
     | 
     |         override def shutdown(instance: Person): Unit =
     |             person.goodbye()
     |     }
personInteraction: (person: Person)itv.lifecycle.Lifecycle[Person]
```

`start` produces an initialized instance of a `Person`. `shutdown` should always be performed after using a `Person`: even if the interaction caused an exception to be thrown.

Note: in this example we perform the `start` operations on a pre-instantiated `Person`: but it's perfectly valid to instantiate the resource within the `start` method of a `Lifecycle`.

Interacting with a single person
----

We want to grab the secret of an individual `Person`, and print it to stdout.


```scala
scala> def announceSecret(person: Person) = {
     |     val secret: String = Lifecycle.using(personInteraction(person)) { greetedPerson =>
     |         println("Asking secret")
     |         greetedPerson.revealSecret()
     |     }
     | 
     |     println(s"${person.name}'s secret is '$secret'")
     | }
announceSecret: (person: Person)Unit

scala> announceSecret(bob)
Said hello to Bob
Asking secret
Said goodbye to Bob
Bob's secret is 'I hate Barry'

scala> announceSecret(fred)
Said hello to Fred
Asking secret
Said goodbye to Fred
Fred's secret is 'I hate Barry'

scala> announceSecret(barry)
Said hello to Barry
Asking secret
Said goodbye to Barry
Barry's secret is 'I'm lonely'
```

We have used `Lifecycle.using` to interact safely with a given `Lifecycle[Person]`:

```scala
def using[T, S](lifecycle: Lifecycle[T])(block: T => S): S
```

The `using` method:
* Gets an instance of `T` by using the `start` method of the given `Lifecycle`
* Uses a block of code you provide to produce an `S`
* Guarantees the `shutdown` method of the given `Lifecycle` is called with the `T` instance: even if the block of code you provided threw an exception
* Returns the `S` your block of code produced

The code block we called `Lifecycle.using` with is pretty tame: it's unlikely to throw an exception.

What if there was a strong chance our code block *will* fail in an unexpected manner? We should still be courteous and ensure we say goodbye to the `Person` we're interacting with.

Let's write a method called `judgeThenAnnounce`. This method will also interact with an individual `Person`.
It will judge their secret: and throw an exception if they're being unkind. Otherwise it will return the secret without exception.

```scala
scala> def judgeThenAnnounce(person: Person) = {
     |     val secret: String = Lifecycle.using(personInteraction(person)) { greetedPerson =>
     |         println("Asking secret")
     |         val revealedSecret: String = greetedPerson.revealSecret()
     | 
     |         if (revealedSecret contains "hate")
     |             throw new IllegalStateException(s"I'm not going to repeat what ${greetedPerson.name} just said to me.")
     |         else
     |             revealedSecret
     |     }
     | 
     |     println(s"${person.name}'s secret is '$secret'")
     | }
judgeThenAnnounce: (person: Person)Unit
```
```scala
scala> judgeThenAnnounce(bob)
Said hello to Bob
Asking secret
Said goodbye to Bob
java.lang.IllegalStateException: I'm not going to repeat what Bob just said to me.
  at $anonfun$1.apply(<console>:24)
  at $anonfun$1.apply(<console>:19)
  at itv.lifecycle.Lifecycle$.using(Lifecycle.scala:51)
  at .judgeThenAnnounce(<console>:19)
  ... 714 elided
```
```scala
scala> judgeThenAnnounce(fred)
Said hello to Fred
Asking secret
Said goodbye to Fred
java.lang.IllegalStateException: I'm not going to repeat what Fred just said to me.
  at $anonfun$1.apply(<console>:24)
  at $anonfun$1.apply(<console>:19)
  at itv.lifecycle.Lifecycle$.using(Lifecycle.scala:51)
  at .judgeThenAnnounce(<console>:19)
  ... 726 elided
```
```scala
scala> judgeThenAnnounce(barry)
Said hello to Barry
Asking secret
Said goodbye to Barry
Barry's secret is 'I'm lonely'
```

Looks good eh? Even though our code block blew up a couple of times due to the extreme views of `bob` and `fred`, we were courteous to each `Person`: always saying hello and goodbye.

Interacting with multiple people
-----

Let's extend this example further, and interact with multiple greeted `People` at the same time.

We will say a `Person` is friends with another `Person` if they both share the same secret.

```scala
scala> def areFriends(personA: Person, personB: Person): Boolean = {
     |     val interrogation = for {
     |         a <- personInteraction(personA)
     |         b <- personInteraction(personB)
     |     }
     |         yield (a.revealSecret(), b.revealSecret())
     | 
     |     Lifecycle.using(interrogation) {
     |         case (secretA, secretB) =>
     |             secretA == secretB
     |     }
     | }
areFriends: (personA: Person, personB: Person)Boolean

scala> areFriends(bob, fred)
Said hello to Bob
Said hello to Fred
Said goodbye to Fred
Said goodbye to Bob
res10: Boolean = true

scala> areFriends(bob, barry)
Said hello to Bob
Said hello to Barry
Said goodbye to Barry
Said goodbye to Bob
res11: Boolean = false

scala> areFriends(fred, barry)
Said hello to Fred
Said hello to Barry
Said goodbye to Barry
Said goodbye to Fred
res12: Boolean = false

scala> areFriends(barry, barry)
Said hello to Barry
Said hello to Barry
Said goodbye to Barry
Said goodbye to Barry
res13: Boolean = true
```

We can `map` and  `flatMap` a `Lifeycle` just like any other container. We get the same resource safety guarantees:

```scala
scala> personInteraction(bob).map(_.revealSecret().toUpperCase).foreach(println)
Said hello to Bob
I HATE BARRY
Said goodbye to Bob
```

Note: in this example we're only interacting with `Person` instances, this is not a limitation, you can combine `Lifecycle`'s of different intance types in the exact same manner.


Long running Lifecycles
----

Our typical usage is for our entire program to be defined within a single `Lifeycle`.

```scala
scala> trait HttpServer {
     |     def stop(): Unit = println("stopped")
     | }
defined trait HttpServer

scala> val httpServerLifecycle: Lifecycle[HttpServer] = 
     |     new VanillaLifecycle[HttpServer] {
     |         override def start: HttpServer = {
     |             val server: HttpServer = new HttpServer {}
     |             /**
     |             Construct some HTTP server, bind it to a port and establish request routes
     |             **/
     |             println("started")
     |             server
     |         }
     |         
     |         override def shutdown(instance: HttpServer) =
     |             instance.stop()
     |     }
httpServerLifecycle: itv.lifecycle.Lifecycle[HttpServer] = $anon$1@4d863dcd
```

`Lifecycle` has a method that will help with this: `runUntilJvmShutdown`.

```scala
httpServerLifecycle.runUntilJvmShutdown
```

This will start an instance using the `Lifecycle`, and register the `shutdown` method to be run on the instance when the JVM exits.
