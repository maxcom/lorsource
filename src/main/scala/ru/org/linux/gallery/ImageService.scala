/*
 * Copyright 1998-2017 Linux.org.ru
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
import java.nio.file.Files

import com.google.common.base.Preconditions
import com.typesafe.scalalogging.StrictLogging
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.validation.Errors
import ru.org.linux.edithistory.{EditHistoryDto, EditHistoryObjectTypeEnum, EditHistoryService}
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
      val info = new EditHistoryDto
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

    val mediumName = if (!new File(htmlPath, image.getMedium).exists) {
      image.getIcon
    } else {
      image.getMedium
    }

    try {
      val mediumImageInfo = new ImageInfo(htmlPath + mediumName)
      val fullInfo = new ImageInfo(htmlPath + image.getOriginal)
      val medURI = siteConfig.getSecureUrl + mediumName
      val fullURI = siteConfig.getSecureUrl + image.getOriginal

      Some(new PreparedImage(medURI, mediumImageInfo, fullURI, fullInfo, image))
    } catch prepareException(image)
  }

  def prepareGalleryItem(items: java.util.List[GalleryItem]): java.util.List[PreparedGalleryItem] =
    items.asScala.map(prepareGalleryItem).asJava

  def getGalleryItems(countItems: Int, tagId: Int): java.util.List[GalleryItem] =
    imageDao.getGalleryItems(countItems, tagId)

  def getGalleryItems(countItems: Int): java.util.List[GalleryItem] = imageDao.getGalleryItems(countItems)

  def imageForTopic(topic:Topic): Image = imageDao.imageForTopic(topic)

  @throws(classOf[IOException])
  @throws(classOf[BadImageException])
  def createScreenshot(user:User, file: File, errors: Errors): UploadedImagePreview = {
    if (!file.isFile) {
      errors.reject(null, "Сбой загрузки изображения: не файл")
    }

    if (!file.canRead) {
      errors.reject(null, "Сбой загрузки изображения: файл нельзя прочитать")
    }

    if (file.length > UploadedImagePreview.MAX_SCREENSHOT_FILESIZE) {
      errors.reject(null, "Сбой загрузки изображения: слишком большой файл")
    }

    val imageParam = ImageUtil.imageCheck(file)

    if (imageParam.getHeight < UploadedImagePreview.MIN_SCREENSHOT_SIZE || imageParam.getHeight > UploadedImagePreview.MAX_SCREENSHOT_SIZE) {
      errors.reject(null, "Сбой загрузки изображения: недопустимые размеры изображения")
    }

    if (imageParam.getWidth < UploadedImagePreview.MIN_SCREENSHOT_SIZE || imageParam.getWidth > UploadedImagePreview.MAX_SCREENSHOT_SIZE) {
      errors.reject(null, "Сбой загрузки изображения: недопустимые размеры изображения")
    }

    if (imageParam.getHeight / (imageParam.getWidth+1d) > 2) {
      errors.reject(null, "Сбой загрузки изображения: слишком узкое изображение")
    }

    if (imageParam.getWidth / (imageParam.getHeight+1d) > 5) {
      errors.reject(null, "Сбой загрузки изображения: слишком широкое изображение")
    }

    if (!errors.hasErrors) {
      val tempFile = File.createTempFile(s"preview-${user.getId}-", "", previewPath)

      try {
        val name = tempFile.getName
        val scrn = new UploadedImagePreview(name, previewPath, imageParam.getExtension)
        scrn.doResize(file)
        scrn
      } finally {
        Files.delete(tempFile.toPath)
      }
    } else {
      null
    }
  }

  def saveScreenshot(scrn: UploadedImagePreview, msgid: Int): Unit = {
    transactional() { _ ⇒
      val id = imageDao.saveImage(msgid, scrn.getExtension)

      scrn.moveTo(galleryPath, id.toString)
    }
  }
}
