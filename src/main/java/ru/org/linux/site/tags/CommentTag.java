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

package ru.org.linux.site.tags;

import com.google.common.collect.Maps;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.JadeTemplate;
import org.joda.time.DateTimeZone;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import ru.org.linux.comment.ApiCommentTopicInfo;
import ru.org.linux.comment.PreparedComment;
import ru.org.linux.site.DateFormats;
import ru.org.linux.topic.Topic;

import javax.servlet.jsp.tagext.TagSupport;
import java.util.Date;
import java.util.Map;

public class CommentTag extends TagSupport {
  private PreparedComment comment;
  private Topic topic;
  private boolean showMenu;
  private boolean commentsAllowed;

  public void setComment(PreparedComment comment) {
    this.comment = comment;
  }

  public void setTopic(Topic topic) {
    this.topic = topic;
  }

  public void setShowMenu(boolean showMenu) {
    this.showMenu = showMenu;
  }

  public void setCommentsAllowed(boolean commentsAllowed) {

    this.commentsAllowed = commentsAllowed;
  }

  @Override
  public int doStartTag() {
    WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(pageContext.getServletContext());

    JadeConfiguration jadeConfiguration = context.getBean(JadeConfiguration.class);
    JadeTemplate jadeTemplate = context.getBean("TemplateComment", JadeTemplate.class);

    Map<String, Object> data = Maps.newHashMap();

    data.put("comment", comment);
    data.put("topic", new ApiCommentTopicInfo(
            topic.getId(),
            topic.getLink(),
            commentsAllowed
    ));

    data.put("showMenu", showMenu);

    data.put("dateFormat", new DateFormatHandler((DateTimeZone) pageContext.getRequest().getAttribute("timezone")));

    jadeConfiguration.renderTemplate(jadeTemplate, data, pageContext.getOut());

    return SKIP_BODY;
  }

  public static class DateFormatHandler {
    private final DateTimeZone tz;

    public DateFormatHandler(DateTimeZone tz) {
      this.tz = tz;
    }

    public String apply(Date input) {
      return DateFormats.getDefault(tz).print(input.getTime());
    }

    public String iso(Date input) {
      return DateFormats.Iso8601().print(input.getTime());
    }
  }
}
