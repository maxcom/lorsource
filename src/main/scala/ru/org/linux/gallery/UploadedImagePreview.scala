/*
 * Copyright 1998-2023 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package ru.org.linux.gallery

import ru.org.linux.util.BadImageException
import ru.org.linux.util.image.ImageUtil
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

import org.apache.commons.io.FilenameUtils

class UploadedImagePreview(val mainFile: File) {
  private val path = mainFile.getParent
  private val name = FilenameUtils.removeExtension(mainFile.getName)
  val extension: String = FilenameUtils.getExtension(mainFile.getName)

  private val allSizes = Image.Sizes.map(size => size -> new File(path, name + s"-${size}px.jpg"))
  
  @throws[IOException]
  def moveTo(dir: File, name: String): Unit = {
    val target = Files.createDirectory(new File(dir, name).toPath).toFile

    Files.move(mainFile.toPath, new File(target, "original." + extension).toPath)

    allSizes foreach { case (size, file) =>
      Files.move(file.toPath, new File(target, size.toString + "px.jpg").toPath)
    }
  }

  @throws[IOException]
  @throws[BadImageException]
  private[gallery] def doResize(uploadedFile: File): Unit = {
    Files.move(uploadedFile.toPath, mainFile.toPath, StandardCopyOption.REPLACE_EXISTING)

    var error: Boolean = true

    try {
      allSizes foreach { case (size, file) =>
        ImageUtil.resizeImage(mainFile.getAbsolutePath, file.getAbsolutePath, size)
      }

      error = false
    } finally {
      if (error) {
        allSizes foreach { case (_, file) => Files.deleteIfExists(file.toPath) }
        Files.deleteIfExists(mainFile.toPath)
      }
    }
  }
}

object UploadedImagePreview {
  def create(prefix: String, extension: String, previewPath: File, uploadedData: File): UploadedImagePreview = {
    val mainFile = File.createTempFile(prefix, "." + extension, previewPath)

    val r = new UploadedImagePreview(mainFile)
    r.doResize(uploadedData)
    r
  }

  def reuse(previewPath: File, filename: String): UploadedImagePreview = {
    val mainFile = new File(previewPath, filename)

    new UploadedImagePreview(mainFile)
  }
}