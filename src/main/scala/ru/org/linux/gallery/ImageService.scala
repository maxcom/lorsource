/*
 * Copyright 1998-2015 Linux.org.ru
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

import java.io.{File, FileNotFoundException}
import java.nio.file.Files

import com.google.common.base.Preconditions
import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scala.transaction.support.TransactionManagement
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import ru.org.linux.edithistory.{EditHistoryDto, EditHistoryObjectTypeEnum, EditHistoryService}
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.{PreparedImage, Topic, TopicDao}
import ru.org.linux.user.{User, UserDao}
import ru.org.linux.util.LorURL
import ru.org.linux.util.image.ImageInfo

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

@Service
class ImageService @Autowired() (imageDao: ImageDao, editHistoryService: EditHistoryService,
                                 topicDao: TopicDao, userDao: UserDao, siteConfig: SiteConfig,
                                 val transactionManager:PlatformTransactionManager)
  extends StrictLogging with TransactionManagement {

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

  private def prepareException(image:Image):PartialFunction[Throwable, None.type] = {
    case e: FileNotFoundException ⇒
      logger.error(s"Image not found! id=${image.getId}: ${e.getMessage}")
      None
    case NonFatal(e) ⇒
      logger.error(s"Bad image id=${image.getId}", e)
      None
  }

  def prepareGalleryItem(item: GalleryItem):Option[PreparedGalleryItem] = {
    val htmlPath = siteConfig.getHTMLPathPrefix

    try {
      val iconInfo: ImageInfo = new ImageInfo(htmlPath + item.getImage.getIcon)
      val fullInfo: ImageInfo = new ImageInfo(htmlPath + item.getImage.getOriginal)
      Some(new PreparedGalleryItem(item, userDao.getUserCached(item.getUserid), iconInfo, fullInfo))
    } catch prepareException(item.getImage)
  }

  def prepareImage(image: Image, secure: Boolean): Option[PreparedImage] = {
    Preconditions.checkNotNull(image)

    val htmlPath = siteConfig.getHTMLPathPrefix

    val mediumName = if (!new File(htmlPath, image.getMedium).exists) {
      image.getIcon
    } else {
      image.getMedium
    }

    try {
      val mediumImageInfo = new ImageInfo(htmlPath + mediumName)
      val fullInfo = new ImageInfo(htmlPath + image.getOriginal)
      val medURI = new LorURL(siteConfig.getMainURI, siteConfig.getMainUrl + mediumName)
      val fullURI = new LorURL(siteConfig.getMainURI, siteConfig.getMainUrl + image.getOriginal)
      val existsMedium2x = Files.exists(new File(htmlPath, image.getMedium2x).toPath)

      Some(new PreparedImage(
        medURI.fixScheme(secure),
        mediumImageInfo,
        fullURI.fixScheme(secure),
        fullInfo,
        image,
        existsMedium2x))
    } catch prepareException(image)
  }

  def prepareGalleryItem(items: java.util.List[GalleryItem]): java.util.List[PreparedGalleryItem] =
    items.asScala.map(prepareGalleryItem).flatMap(_.toSeq).asJava

  def getGalleryItems(countItems: Int, tagId: Int): java.util.List[GalleryItem] =
    imageDao.getGalleryItems(countItems, tagId)

  def getGalleryItems(countItems: Int): java.util.List[GalleryItem] = imageDao.getGalleryItems(countItems)

  def imageForTopic(topic:Topic):Image = imageDao.imageForTopic(topic)
}