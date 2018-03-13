package com.itv.lifecycle

import java.io.{File, FileOutputStream}
import java.nio.file.{Files, Path}
import java.util.function.Consumer

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

  object CreateDirectoryLifecycle {
    def apply(rootPath: Path, directoryName: String): Lifecycle[Path] =
      new VanillaLifecycle[Path] {
        override def start(): Path = {
          val file = new File(rootPath.toFile, directoryName)
          file.mkdir()
          file.toPath
        }

        override def shutdown(instance: Path): Unit = {
          Files.list(instance).forEach(new Consumer[Path] {
            override def accept(t: Path) =
              t.toFile.delete()
          })
          instance.toFile.delete()
        }
      }
  }

}
