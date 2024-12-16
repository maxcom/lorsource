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

import java.util
import scala.beans.{BeanProperty, BooleanBeanProperty}

class EditTopicRequest(
                        @BeanProperty var url: String = null,
                        @BeanProperty var linktext: String = null,
                        @BeanProperty var title: String = null,
                        @BeanProperty var msg: String = null,
                        @BooleanBeanProperty var minor: Boolean = false,
                        @BeanProperty var bonus: Int = 3,
                        @BeanProperty var tags: String = null,
                        @BeanProperty var poll: util.Map[Integer, String] = null,
                        @BeanProperty var editorBonus: util.Map[Integer, Integer] = null,
                        @BeanProperty var newPoll: Array[String] = new Array[String](3),
                        @BooleanBeanProperty var multiselect: Boolean = false,
                        @BeanProperty var fromHistory: Integer = null,
                        @BeanProperty var image: MultipartFile = null,
                        @BeanProperty var uploadedImage: String = null,
                        @BeanProperty var additionalUploadedImages: Array[String] = new Array[String](0),
                        @BeanProperty var additionalImage: Array[MultipartFile] = null) {
  def this() = this(title = null) // нужен конструктор по умолчанию для spring
}