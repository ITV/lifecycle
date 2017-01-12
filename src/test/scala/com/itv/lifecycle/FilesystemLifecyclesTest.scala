package com.itv.lifecycle

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.UUID

import org.scalatest.FunSuite
import org.scalatest.Matchers._

import scala.io.Source

class FilesystemLifecyclesTest extends FunSuite {

  import FilesystemLifecycles._

  test("should create a file, and remove it afterwards") {
    val fileName = UUID.randomUUID().toString

    def fileShouldNotExist = new File("./" + fileName).exists() shouldBe false

    withClue("file should not exist before lifecycle usage") {
      fileShouldNotExist
    }

    val expectedContents = "Hello, world!"
    Lifecycle.using(CreateFileLifecycle(Paths.get("."), fileName, expectedContents)) { file =>
      withClue("file should exist during lifecycle usage") {
        file.exists() shouldBe true
      }
      Source.fromFile(file).mkString shouldBe expectedContents
    }

    withClue("file should not exist after lifecycle usage") {
      fileShouldNotExist
    }
  }

}
