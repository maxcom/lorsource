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
import org.springframework.web.multipart.MultipartRequest
import ru.org.linux.edithistory.{EditHistoryDao, EditHistoryObjectTypeEnum, EditHistoryRecord}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.{PreparedImage, Topic, TopicDao}
import ru.org.linux.user.{User, UserService}
import ru.org.linux.util.BadImageException
import ru.org.linux.util.image.{ImageInfo, ImageUtil}

import java.io.{File, FileNotFoundException, IOException}
import java.util.Optional
import javax.servlet.http.HttpServletRequest
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.RichOption
import scala.util.control.NonFatal

@Service
class ImageService(imageDao: ImageDao, editHistoryDao: EditHistoryDao,
                   topicDao: TopicDao, userService: UserService, siteConfig: SiteConfig,
                   val transactionManager: PlatformTransactionManager)
  extends StrictLogging with TransactionManagement {

  private val previewPath = new File(siteConfig.getUploadPath + "/gallery/preview")
  private val galleryPath = new File(siteConfig.getUploadPath + "/images")

  def deleteImage(editor: User, image: Image):Unit = {
    transactional() { _ =>
      val info = new EditHistoryRecord
      info.setEditor(editor.getId)
      info.setMsgid(image.topicId)
      info.setOldimage(image.id)
      info.setObjectType(EditHistoryObjectTypeEnum.TOPIC)

      imageDao.deleteImage(image)
      editHistoryDao.insert(info)
      topicDao.updateLastmod(image.topicId, false)
    }
  }

  private def prepareException(image: Image):PartialFunction[Throwable, None.type] = {
    case e: FileNotFoundException =>
      logger.error(s"Image not found! id=${image.id}: ${e.getMessage}")
      None
    case NonFatal(e) =>
      logger.error(s"Bad image id=${image.id}", e)
      None
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
    } catch prepareException(image)
  }


  // java api
  def prepareImageJava(image: Image): Optional[PreparedImage] = prepareImage(image).toJava

  def prepareGalleryItem(items: java.util.List[GalleryItem]): java.util.List[PreparedGalleryItem] =
    items.asScala.map(prepareGalleryItem).asJava

  def getGalleryItems(countItems: Int, tagId: Int): Seq[GalleryItem] = imageDao.getGalleryItems(countItems, tagId)

  def getGalleryItems(countItems: Int): java.util.List[GalleryItem] = imageDao.getGalleryItems(countItems).asJava

  def imageForTopic(topic: Topic): Optional[Image] = Option(imageDao.imageForTopic(topic)).toJava

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

  def processUploadImage(request: HttpServletRequest): Option[File] = {
    request match {
      case multipartRequest: MultipartRequest =>
        val multipartFile = multipartRequest.getFile("image")

        if (multipartFile != null && !multipartFile.isEmpty) {
          val uploadedFile = File.createTempFile("lor-image-", "")
          logger.debug("Transfering upload to: " + uploadedFile)
          multipartFile.transferTo(uploadedFile)

          Some(uploadedFile)
        } else {
          None
        }
      case _ =>
        None
    }
  }

  def processUpload(currentUser: User, uploadedImage: Option[String], image: Option[File], errors: Errors): Option[UploadedImagePreview] = {
    image match {
      case Some(image) =>
        try {
          createImagePreview(currentUser, image, errors).map { previewImage =>
            logger.info("SCREEN: " + image.getAbsolutePath + "\nINFO: SCREEN: " + image)

            previewImage
          }
        } catch {
          case e: BadImageException =>
            errors.reject(null, "Некорректное изображение: " + e.getMessage)
            None
        }
      case None =>
        uploadedImage
          .filter(_.startsWith(uploadedImagePrefix(currentUser)))
          .map(f => UploadedImagePreview.reuse(previewPath, f))
          .filter(_.mainFile.exists)
    }
  }

  def saveScreenshot(imagePreview: UploadedImagePreview, msgid: Int): Unit = transactional() { _ =>
    val id = imageDao.saveImage(msgid, imagePreview.extension)

    imagePreview.moveTo(galleryPath, id.toString)
  }
}
