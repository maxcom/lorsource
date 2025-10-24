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
package ru.org.linux.topic

import org.springframework.web.multipart.MultipartFile
import ru.org.linux.group.Group
import ru.org.linux.poll.Poll
import ru.org.linux.user.User

import java.util
import scala.beans.{BeanProperty, BooleanBeanProperty}

trait ImageTopicRequest {
  def additionalImage: Array[MultipartFile]
  def uploadedImage: String
  def image: MultipartFile
  def additionalUploadedImages: Array[String]

  def uploadedImage_=(value: String): Unit
  def additionalUploadedImages_=(value: Array[String]): Unit
}

class AddTopicRequest(
                       @BeanProperty var title: String = null,
                       @BeanProperty var msg: String = null,
                       @BeanProperty var url: String = null,
                       @BeanProperty var group: Group = null,
                       @BeanProperty var linktext: String = null,
                       @BeanProperty var tags: String = null,
                       @BooleanBeanProperty var noinfo: Boolean = false,
                       @BeanProperty var poll: Array[String] = new Array[String](Poll.MaxPollSize),
                       @BooleanBeanProperty var multiSelect: Boolean = false,
                       @BeanProperty var additionalUploadedImages: Array[String] = new Array[String](0),
                       @BeanProperty var additionalImage: Array[MultipartFile] = null,
                       @BeanProperty var nick: User = null,
                       @BeanProperty var password: String = null,
                       @BeanProperty var preview: String = null,
                       @BeanProperty var draft: String = null,
                       @BooleanBeanProperty var allowAnonymous: Boolean = true,
                       @BeanProperty var image: MultipartFile = null,
                       @BeanProperty var uploadedImage: String = null) extends ImageTopicRequest {
  def this() = this(title = null) // нужен конструктор по умолчанию для spring

  def isPreviewMode: Boolean = preview != null
  def isDraftMode: Boolean = draft != null
}

class EditTopicRequest(
                        @BeanProperty var msgid: Topic = null,
                        @BeanProperty var url: String = null,
                        @BeanProperty var linktext: String = null,
                        @BeanProperty var title: String = null,
                        @BeanProperty var msg: String = null,
                        @BooleanBeanProperty var minor: Boolean = false,
                        @BeanProperty var bonus: Int = 3,
                        @BeanProperty var tags: String = null,
                        @BeanProperty var poll: util.Map[Integer, String] = null,
                        @BeanProperty var editorBonus: util.Map[User, Integer] = null,
                        @BeanProperty var newPoll: Array[String] = new Array[String](3),
                        @BooleanBeanProperty var multiselect: Boolean = false,
                        @BeanProperty var fromHistory: Integer = null,
                        @BeanProperty var image: MultipartFile = null,
                        @BeanProperty var uploadedImage: String = null,
                        @BeanProperty var additionalUploadedImages: Array[String] = new Array[String](0),
                        @BeanProperty var additionalImage: Array[MultipartFile] = null,
                        @BeanProperty var lastEdit: java.lang.Long = null
                      ) extends ImageTopicRequest {
  def this() = this(title = null) // нужен конструктор по умолчанию для spring

  def topic: Topic = msgid
}