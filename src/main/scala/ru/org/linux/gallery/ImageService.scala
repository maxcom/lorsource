/*
 * Copyright 1998-2018 Linux.org.ru
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

import java.io.{File, FileNotFoundException, IOException}

import com.google.common.base.Preconditions
import com.typesafe.scalalogging.StrictLogging
import javax.servlet.http.{HttpServletRequest, HttpSession}
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.validation.Errors
import org.springframework.web.multipart.{MultipartHttpServletRequest, MultipartRequest}
import ru.org.linux.edithistory.{EditHistoryObjectTypeEnum, EditHistoryRecord, EditHistoryService}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.{PreparedImage, Topic, TopicDao}
import ru.org.linux.user.{User, UserDao}
import ru.org.linux.util.BadImageException
import ru.org.linux.util.image.{ImageInfo, ImageUtil}

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

@Service
class ImageService(imageDao: ImageDao, editHistoryService: EditHistoryService,
                   topicDao: TopicDao, userDao: UserDao, siteConfig: SiteConfig,
                   val transactionManager: PlatformTransactionManager)
  extends StrictLogging with TransactionManagement {

  private val previewPath = new File(siteConfig.getUploadPath + "/gallery/preview")
  private val galleryPath = new File(siteConfig.getUploadPath + "/images")

  def deleteImage(editor: User, image: Image):Unit = {
    transactional() { _ ⇒
      val info = new EditHistoryRecord
      info.setEditor(editor.getId)
      info.setMsgid(image.getTopicId)
      info.setOldimage(image.getId)
      info.setObjectType(EditHistoryObjectTypeEnum.TOPIC)

      imageDao.deleteImage(image)
      editHistoryService.insert(info)
      topicDao.updateLastmod(image.getTopicId, false)
    }
  }

  private def prepareException(image: Image):PartialFunction[Throwable, None.type] = {
    case e: FileNotFoundException ⇒
      logger.error(s"Image not found! id=${image.getId}: ${e.getMessage}")
      None
    case NonFatal(e) ⇒
      logger.error(s"Bad image id=${image.getId}", e)
      None
  }

  def prepareGalleryItem(item: GalleryItem):PreparedGalleryItem = {
    PreparedGalleryItem(item, userDao.getUserCached(item.getUserid))
  }

  def prepareImage(image: Image): Option[PreparedImage] = {
    Preconditions.checkNotNull(image)

    val htmlPath = siteConfig.getUploadPath

    val mediumName = image.getMedium

    try {
      val mediumImageInfo = new ImageInfo(htmlPath + mediumName)
      val fullInfo = new ImageInfo(htmlPath + image.getOriginal)
      val medURI = siteConfig.getSecureUrl + mediumName
      val fullURI = siteConfig.getSecureUrl + image.getOriginal

      Some(new PreparedImage(medURI, mediumImageInfo, fullURI, fullInfo, image))
    } catch prepareException(image)
  }

  // java api
  def prepareImageOrNull(image: Image): PreparedImage = prepareImage(image).orNull

  def prepareGalleryItem(items: java.util.List[GalleryItem]): java.util.List[PreparedGalleryItem] =
    items.asScala.map(prepareGalleryItem).asJava

  def getGalleryItems(countItems: Int, tagId: Int): java.util.List[GalleryItem] =
    imageDao.getGalleryItems(countItems, tagId)

  def getGalleryItems(countItems: Int): java.util.List[GalleryItem] = imageDao.getGalleryItems(countItems)

  def imageForTopic(topic: Topic): Image = imageDao.imageForTopic(topic)

  @throws(classOf[IOException])
  @throws(classOf[BadImageException])
  def createImagePreview(user: User, file: File, errors: Errors): UploadedImagePreview = {
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
      UploadedImagePreview.create(
        prefix = s"preview-${user.getId}-",
        extension = imageParam.getExtension,
        previewPath = previewPath,
        uploadedData = file)
    } else {
      null
    }
  }

  def processUploadImage(request: HttpServletRequest): File = {
    if (request.isInstanceOf[MultipartHttpServletRequest]) {
      val multipartFile = request.asInstanceOf[MultipartRequest].getFile("image")
      if (multipartFile != null && !multipartFile.isEmpty) {
        val uploadedFile = File.createTempFile("lor-image-", "")
        logger.debug("Transfering upload to: " + uploadedFile)
        multipartFile.transferTo(uploadedFile)

        uploadedFile
      } else {
        null
      }
    } else {
      null
    }
  }

  def processUpload(currentUser: User, session: HttpSession, image: File, errors: Errors): UploadedImagePreview = {
    if (session == null) return null
    if (image != null) {
      try {
        val screenShot = createImagePreview(currentUser, image, errors)
        if (screenShot != null) {
          logger.info("SCREEN: " + image.getAbsolutePath + "\nINFO: SCREEN: " + image)
          session.setAttribute("image", screenShot)
        }
        screenShot
      } catch {
        case e: BadImageException ⇒
          errors.reject(null, "Некорректное изображение: " + e.getMessage)
          null
      }
    } else if (session.getAttribute("image") != null && !("" == session.getAttribute("image"))) {
      val screenShot = session.getAttribute("image").asInstanceOf[UploadedImagePreview]
      if (!screenShot.mainFile.exists) {
        null
      } else {
        screenShot
      }
    } else {
      null
    }
  }


  def saveScreenshot(imagePreview: UploadedImagePreview, msgid: Int): Unit = {
    transactional() { _ ⇒
      val id = imageDao.saveImage(msgid, imagePreview.extension)

      imagePreview.moveTo(galleryPath, id.toString)
    }
  }
}
