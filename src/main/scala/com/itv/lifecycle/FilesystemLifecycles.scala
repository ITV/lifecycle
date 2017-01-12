package com.itv.lifecycle

import java.io.{File, FileOutputStream}
import java.nio.file.Path

object FilesystemLifecycles {

  object CreateFileLifecycle {

    def apply(rootPath: Path, fileName: String, contents: String): Lifecycle[File] =
      new VanillaLifecycle[File] {
        override def shutdown(instance: File) = instance.delete()

        override def start() = {
          val file = new File(rootPath.toFile, fileName)

          file.createNewFile()
          val fileOutputStream = new FileOutputStream(file)
          fileOutputStream.write(contents.getBytes("UTF-8"))
          fileOutputStream.close()
          file
        }
      }
  }

}
