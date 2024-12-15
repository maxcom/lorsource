/*
 * Copyright 1998-2024 Linux.org.ru
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

import com.google.common.base.Preconditions
import com.typesafe.scalalogging.StrictLogging
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.validation.Errors
import org.springframework.web.multipart.MultipartFile
import ru.org.linux.auth.AuthorizedSession
import ru.org.linux.edithistory.{EditHistoryDao, EditHistoryObjectTypeEnum, EditHistoryRecord}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.{PreparedImage, Topic, TopicDao}
import ru.org.linux.user.{User, UserService}
import ru.org.linux.util.BadImageException
import ru.org.linux.util.image.{ImageInfo, ImageUtil}

import java.io.{File, FileNotFoundException, IOException}
import java.nio.file.Files
import java.time.{Duration, Instant}
import javax.annotation.Nullable
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

@Service
class ImageService(imageDao: ImageDao, editHistoryDao: EditHistoryDao,
                   topicDao: TopicDao, userService: UserService, siteConfig: SiteConfig,
                   val transactionManager: PlatformTransactionManager)
  extends StrictLogging with TransactionManagement {

  private val previewPath = new File(siteConfig.getUploadPath + "/gallery/preview")
  private val galleryPath = new File(siteConfig.getUploadPath + "/images")

  def deleteImage(image: Image)(implicit session: AuthorizedSession): Unit = {
    transactional() { _ =>
      val info = new EditHistoryRecord
      info.setEditor(session.user.getId)
      info.setMsgid(image.topicId)
      info.setOldimage(image.id)
      info.setObjectType(EditHistoryObjectTypeEnum.TOPIC)

      imageDao.deleteImage(image)
      editHistoryDao.insert(info)
      topicDao.updateLastmod(image.topicId, false)
    }
  }

  def prepareGalleryItem(item: GalleryItem): PreparedGalleryItem = {
    PreparedGalleryItem(item, userService.getUserCached(item.getUserid))
  }

  def prepareImage(image: Image): Option[PreparedImage] = {
    Preconditions.checkNotNull(image)

    val htmlPath = siteConfig.getUploadPath

    val mediumName = image.getMedium

    try {
      val mediumImageInfo = new ImageInfo(htmlPath + mediumName)
      val fullInfo = new ImageInfo(htmlPath + image.original)
      val medURI = siteConfig.getSecureUrl + mediumName
      val fullURI = siteConfig.getSecureUrl + image.original

      Some(new PreparedImage(medURI, mediumImageInfo, fullURI, fullInfo, image))
    } catch {
      case e: FileNotFoundException =>
        logger.error(s"Image not found! id=${image.id}: ${e.getMessage}")
        None
      case NonFatal(e) =>
        logger.error(s"Bad image id=${image.id}", e)
        None
    }
  }

  def prepareGalleryItem(items: java.util.List[GalleryItem]): java.util.List[PreparedGalleryItem] =
    items.asScala.map(prepareGalleryItem).asJava

  def getGalleryItems(countItems: Int, tagId: Int): Seq[GalleryItem] = imageDao.getGalleryItems(countItems, tagId)

  def getGalleryItems(countItems: Int): java.util.List[GalleryItem] = imageDao.getGalleryItems(countItems).asJava

  def allImagesForTopic(topic: Topic): Seq[Image] = imageDao.allImagesForTopic(topic)

  @throws(classOf[IOException])
  @throws(classOf[BadImageException])
  private def createImagePreview(user: User, file: File, errors: Errors): Option[UploadedImagePreview] = {
    if (!file.isFile) {
      errors.reject(null, "Сбой загрузки изображения: не файл")
    }

    if (!file.canRead) {
      errors.reject(null, "Сбой загрузки изображения: файл нельзя прочитать")
    }

    if (file.length > Image.MaxFileSize) {
      errors.reject(null, "Сбой загрузки изображения: слишком большой файл")
    }

    val imageParam = ImageUtil.imageCheck(file)

    if (imageParam.getHeight < Image.MinDimension || imageParam.getHeight > Image.MaxDimension) {
      errors.reject(null, "Сбой загрузки изображения: недопустимые размеры изображения")
    }

    if (imageParam.getWidth < Image.MinDimension || imageParam.getWidth > Image.MaxDimension) {
      errors.reject(null, "Сбой загрузки изображения: недопустимые размеры изображения")
    }

    if (imageParam.getHeight / (imageParam.getWidth+1d) > 2) {
      errors.reject(null, "Сбой загрузки изображения: слишком узкое изображение")
    }

    if (imageParam.getWidth / (imageParam.getHeight+1d) > 5) {
      errors.reject(null, "Сбой загрузки изображения: слишком широкое изображение")
    }

    if (!errors.hasErrors) {
      Some(UploadedImagePreview.create(
        prefix = uploadedImagePrefix(user),
        extension = imageParam.getExtension,
        previewPath = previewPath,
        uploadedData = file))
    } else {
      None
    }
  }

  private def uploadedImagePrefix(user: User) = s"preview-${user.getId}-"

  private def saveToTempFile(@Nullable imageUpload: MultipartFile): Option[File] = {
    if (imageUpload != null && !imageUpload.isEmpty) {
      val uploadedFile = File.createTempFile("lor-image-", "")
      logger.debug(s"Transferring upload to: $uploadedFile")
      imageUpload.transferTo(uploadedFile.toPath)

      Some(uploadedFile)
    } else {
      None
    }
  }

  def processUpload(uploadedImage: Option[String], imageUpload: MultipartFile, errors: Errors)
                   (implicit currentUser: AuthorizedSession): Option[UploadedImagePreview] = {
    val image = saveToTempFile(imageUpload)

    image match {
      case Some(image) =>
        try {
          createImagePreview(currentUser.user, image, errors).map { previewImage =>
            logger.info(s"Created image preview: $image -> ${previewImage.mainFile}")

            previewImage
          }
        } catch {
          case e: BadImageException =>
            errors.reject(null, "Некорректное изображение: " + e.getMessage)
            None
        }
      case None =>
        uploadedImage
          .filter(_.startsWith(uploadedImagePrefix(currentUser.user)))
          .map(f => UploadedImagePreview.reuse(previewPath, f))
          .filter(_.mainFile.exists)
    }
  }

  def saveImage(imagePreview: UploadedImagePreview, msgid: Int, main: Boolean): Unit = transactional() { _ =>
    val id = imageDao.saveImage(msgid, imagePreview.extension, main)

    imagePreview.moveTo(galleryPath, id.toString)
  }

  def cleanOldPreviews(age: Duration): Unit = {
    if (previewPath.exists()) {
      val deadline = Instant.now.minus(age)

      Files.newDirectoryStream(previewPath.toPath)
        .asScala
        .filter(p => p.toFile.isFile && Files.getLastModifiedTime(p).toInstant.isBefore(deadline))
        .foreach { p =>
          logger.info(s"Delete old preview $p (last modified ${Files.getLastModifiedTime(p)})")

          p.toFile.delete()
        }
    }
  }
}
