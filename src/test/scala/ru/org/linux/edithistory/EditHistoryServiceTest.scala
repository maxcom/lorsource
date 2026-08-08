/*
 * Copyright 1998-2026 Linux.org.ru
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
package ru.org.linux.edithistory

import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito
import ru.org.linux.gallery.{Image, ImageDao, ImageService}
import ru.org.linux.markup.{MarkupType, MessageTextService}
import ru.org.linux.msgbase.{MessageText, MsgbaseDao}
import ru.org.linux.poll.PollDao
import ru.org.linux.reaction.Reactions
import ru.org.linux.tag.TagRef
import ru.org.linux.topic.{PreparedImage, Topic, TopicTagService}
import ru.org.linux.user.UserService
import ru.org.linux.util.image.ImageInfo

import java.sql.Timestamp

/** Юнит-тесты для routing'а изображений в истории редактирования ([[EditHistoryService]]).
  *
  * Проверяет, что legacy-правки основного изображения (колонка `edit_info.oldimage`) маршрутизируются в
  * `addedMainImage`/`removedMainImage` (а не в обычные `addedImages`/`removedImages`), и что при отсутствии файла
  * попадают в `*MainMissingImage` плейсхолдеры.
  */
class EditHistoryServiceTest:
  private val topicTagService = Mockito.mock(classOf[TopicTagService])
  private val userService = Mockito.mock(classOf[UserService])
  private val textService = Mockito.mock(classOf[MessageTextService])
  private val msgbaseDao = Mockito.mock(classOf[MsgbaseDao])
  private val editHistoryDao = Mockito.mock(classOf[EditHistoryDao])
  private val imageDao = Mockito.mock(classOf[ImageDao])
  private val imageService = Mockito.mock(classOf[ImageService])
  private val pollDao = Mockito.mock(classOf[PollDao])

  private val service =
    EditHistoryService(
      topicTagService,
      userService,
      textService,
      msgbaseDao,
      editHistoryDao,
      imageDao,
      imageService,
      pollDao)

  private def topic(id: Int): Topic =
    Topic(
      id = id,
      postscore = 0,
      sticky = false,
      linktext = null,
      url = null,
      title = "test",
      authorUserId = 1,
      groupId = 1,
      deleted = false,
      expired = false,
      commitby = 1,
      postdate = new Timestamp(System.currentTimeMillis()),
      commitDate = null,
      groupUrl = "test",
      lastModified = new Timestamp(System.currentTimeMillis()),
      sectionId = 1,
      commentCount = 0,
      commited = true,
      notop = false,
      userAgentId = 0,
      postIP = "",
      resolved = false,
      minor = false,
      draft = false,
      allowAnonymous = false,
      reactions = Reactions.empty,
      expireDate = null,
      openWarnings = 0
    )

  private def preparedImage(image: Image): PreparedImage =
    PreparedImage(
      mediumName = "images/" + image.id + "/1000px.jpg",
      mediumInfo = Mockito.mock(classOf[ImageInfo]),
      fullName = "images/" + image.id + "/original.jpg",
      fullInfo = Mockito.mock(classOf[ImageInfo]),
      image = image,
      lazyLoad = false
    )

  private def stubCommon(t: Topic, currentImages: Seq[Image]): Unit =
    Mockito.when(msgbaseDao.getMessageText(t.id)).thenReturn(MessageText("text", MarkupType.Markdown))
    Mockito.when(topicTagService.getTagRefs(t)).thenReturn(Seq.empty[TagRef])
    Mockito.when(imageService.allImagesForTopic(t)).thenReturn(currentImages)
    currentImages.foreach { img =>
      Mockito.when(imageService.prepareImage(img)).thenReturn(Some(preparedImage(img)))
      Mockito.when(imageService.prepareImage(img, false)).thenReturn(Some(preparedImage(img)))
    }

  @Test
  def legacyMainImageAddedRoutesToAddedMainImage(): Unit =
    val t = topic(100)
    val img = Image(id = 7, topicId = 100, original = "images/7/original.jpg", deleted = false, purged = false)
    stubCommon(t, Seq(img))

    val dto = EditHistoryRecord(
      id = 1,
      msgid = 100,
      editor = 2,
      objectType = EditHistoryObjectTypeEnum.TOPIC,
      legacyMainImage = Some(0))

    Mockito.when(editHistoryDao.getEditInfo(100, EditHistoryObjectTypeEnum.TOPIC)).thenReturn(Seq(dto))

    val result = service.prepareEditInfo(t)

    assertEquals(2, result.size)
    val edit = result.head
    // legacy main-image edit must populate addedMainImage, not regular addedImages
    assertNotNull("addedMainImage must be populated for legacy main-image add", edit.addedMainImage)
    assertFalse("addedMainImage must be non-empty", edit.addedMainImage.isEmpty)
    assertNull("addedImages must be null for legacy main-image add", edit.addedImages)
    // missing variant empty (not null) because file (mock) present
    assertTrue("addedMainMissingImage must be empty when file present", edit.addedMainMissingImage.isEmpty)

  @Test
  def legacyMainImageRemovedRoutesToRemovedMainImage(): Unit =
    val t = topic(101)
    // current images do not contain xId=9
    stubCommon(t, Seq.empty)
    val removedImg = Image(id = 9, topicId = 101, original = "images/9/original.jpg", deleted = false, purged = false)
    Mockito.when(imageDao.getImage(9)).thenReturn(removedImg)
    Mockito.when(imageService.prepareImage(removedImg)).thenReturn(Some(preparedImage(removedImg)))
    Mockito.when(imageService.prepareImage(removedImg, false)).thenReturn(Some(preparedImage(removedImg)))

    val dto = EditHistoryRecord(
      id = 1,
      msgid = 101,
      editor = 2,
      objectType = EditHistoryObjectTypeEnum.TOPIC,
      legacyMainImage = Some(9))

    Mockito.when(editHistoryDao.getEditInfo(101, EditHistoryObjectTypeEnum.TOPIC)).thenReturn(Seq(dto))

    val result = service.prepareEditInfo(t)

    assertEquals(2, result.size)
    val edit = result.head
    assertNotNull("removedMainImage must be populated for legacy main-image remove", edit.removedMainImage)
    assertFalse("removedMainImage must be non-empty", edit.removedMainImage.isEmpty)
    assertNull("removedImages must be null for legacy main-image remove", edit.removedImages)
    assertTrue("removedMainMissingImage must be empty when file present", edit.removedMainMissingImage.isEmpty)

  @Test
  def legacyMainImageRemovedPurgedRoutesToRemovedMainMissingImage(): Unit =
    val t = topic(102)
    stubCommon(t, Seq.empty)
    val purgedImg = Image(id = 11, topicId = 102, original = "images/11/original.jpg", deleted = true, purged = true)
    Mockito.when(imageDao.getImage(11)).thenReturn(purgedImg)
    // purged → prepareImage returns None (real guard) — emulate by returning None
    Mockito.when(imageService.prepareImage(purgedImg)).thenReturn(None)
    Mockito.when(imageService.prepareImage(purgedImg, false)).thenReturn(None)

    val dto = EditHistoryRecord(
      id = 1,
      msgid = 102,
      editor = 2,
      objectType = EditHistoryObjectTypeEnum.TOPIC,
      legacyMainImage = Some(11))

    Mockito.when(editHistoryDao.getEditInfo(102, EditHistoryObjectTypeEnum.TOPIC)).thenReturn(Seq(dto))

    val result = service.prepareEditInfo(t)

    assertEquals(2, result.size)
    val edit = result.head
    assertTrue("removedMainImage must be empty when purged", edit.removedMainImage.isEmpty)
    assertNotNull("removedMainMissingImage must be populated when purged", edit.removedMainMissingImage)
    assertFalse("removedMainMissingImage must be non-empty", edit.removedMainMissingImage.isEmpty)
    assertEquals(Integer.valueOf(11), edit.removedMainMissingImage.get(0).id)

  @Test
  def oldaddimagesRoutesToRegularBuckets(): Unit =
    val t = topic(103)
    val img = Image(id = 20, topicId = 103, original = "images/20/original.jpg", deleted = false, purged = false)
    stubCommon(t, Seq(img))
    val removedImg = Image(id = 21, topicId = 103, original = "images/21/original.jpg", deleted = false, purged = false)
    Mockito.when(imageDao.getImage(21)).thenReturn(removedImg)
    Mockito.when(imageService.prepareImage(removedImg)).thenReturn(Some(preparedImage(removedImg)))
    Mockito.when(imageService.prepareImage(removedImg, false)).thenReturn(Some(preparedImage(removedImg)))

    val dto = EditHistoryRecord(
      id = 1,
      msgid = 103,
      editor = 2,
      objectType = EditHistoryObjectTypeEnum.TOPIC,
      oldaddimages = Some(Seq(21))
    ) // 21 not in current → removed; current 20 → added

    Mockito.when(editHistoryDao.getEditInfo(103, EditHistoryObjectTypeEnum.TOPIC)).thenReturn(Seq(dto))

    val result = service.prepareEditInfo(t)

    assertEquals(2, result.size)
    val edit = result.head
    assertNotNull("addedImages must be populated for oldaddimages edit", edit.addedImages)
    assertFalse(edit.addedImages.isEmpty)
    assertNotNull("removedImages must be populated for oldaddimages edit", edit.removedImages)
    assertFalse(edit.removedImages.isEmpty)
    assertNull("addedMainImage must be null for oldaddimages edit", edit.addedMainImage)
    assertNull("removedMainImage must be null for oldaddimages edit", edit.removedMainImage)
