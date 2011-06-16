/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.site;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;

public class MessageTable {
  public static final int RSS_MIN = 10;
  public static final int RSS_MAX = 30;
  public static final int RSS_DEFAULT = 20;

  private MessageTable() {
  }

  public static String getTopicRss(String htmlPath, String fullUrl, PreparedMessage preparedTopic) throws   IOException, BadImageException {
    StringBuilder buf = new StringBuilder();

    Message topic = preparedTopic.getMessage();

    if (topic.getSection().isImagepost()) {
      buf.append(NewsViewer.showMediumImage(htmlPath, topic, true));

      ImageInfo info = new ImageInfo(htmlPath + topic.getUrl(), ImageInfo.detectImageType(new File(htmlPath + topic.getUrl())));

      buf.append(preparedTopic.getProcessedMessage());
      buf.append("<p><i>" + info.getWidth() + 'x' + info.getHeight() + ", " + info.getSizeString() + "</i>");
    } else if (topic.isVotePoll()) {
      PreparedPoll poll = preparedTopic.getPoll();
      buf.append(poll.renderPoll(fullUrl));
    } else {
      buf.append(preparedTopic.getProcessedMessage());
    }

    return buf.toString();
  }
}
