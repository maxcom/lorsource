/*
 * Copyright 1998-2022 Linux.org.ru
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

import ru.org.linux.group.Group
import ru.org.linux.markup.MarkupType
import ru.org.linux.poll.PreparedPoll
import ru.org.linux.reaction.PreparedReactions
import ru.org.linux.section.Section
import ru.org.linux.site.DeleteInfo
import ru.org.linux.tag.TagRef
import ru.org.linux.user.{Remark, User}

import javax.annotation.Nullable
import scala.beans.BeanProperty

case class PreparedTopic(@BeanProperty message: Topic, @BeanProperty author: User,
                         @Nullable @BeanProperty deleteInfo: DeleteInfo, @Nullable @BeanProperty deleteUser: User,
                         @BeanProperty processedMessage: String, @BeanProperty poll: PreparedPoll,
                         @Nullable @BeanProperty commiter: User, @BeanProperty tags: java.util.List[TagRef],
                         @BeanProperty group: Group, @BeanProperty section: Section,
                         @BeanProperty markupType: MarkupType, @Nullable @BeanProperty image: PreparedImage,
                         @BeanProperty postscoreInfo: String, @Nullable @BeanProperty remark: Remark,
                         @BeanProperty showRegisterInvite: Boolean, @Nullable @BeanProperty userAgent: String,
                         @BeanProperty reactions: PreparedReactions) {
  def getId: Int = message.id
}