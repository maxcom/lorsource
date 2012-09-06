/*
 * Copyright 1998-2012 Linux.org.ru
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.site.Template;
import ru.org.linux.spring.Configuration;
import ru.org.linux.user.ProfileProperties;
import ru.org.linux.user.User;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;

/**
 * userpic tag
 */
@Configurable
public class UserPic extends TagSupport {

  @Autowired
  private Configuration configuration;

  private User author;

  public void setAuthor(User author) {
    this.author = author;
  }

  @Override
  public int doStartTag() throws JspException {
    ProfileProperties properties = AuthUtil.getProf();
    JspWriter out = pageContext.getOut();
    if (author.getPhoto() != null) {
      try {
        ImageInfo info = new ImageInfo(configuration.getHTMLPathPrefix() + "/photos/" + author.getPhoto());

        out
            .append("<div class=\"userpic\">")
            .append("<img class=\"photo\" src=\"/photos/")
            .append(author.getPhoto())
            .append("\" alt=\"")
            .append(author.getNick())
            .append("\" ")
            .append(info.getCode())
            .append(" >")
            .append("</div>");
      } catch (BadImageException e) {
  //      logger.warning(StringUtil.getStackTrace(e));
      } catch (IOException e) {
  //      logger.warning(StringUtil.getStackTrace(e));
      }
    } else {
      if (author.hasGravatar()) {
        String avatarStyle = properties.getAvatarMode();

        try {
          out
              .append("<div class=\"userpic\">")
              .append("<img width=150 height=150 class=\"photo\" src=\"")
              .append(author.getGravatar(avatarStyle, 150, pageContext.getRequest().isSecure()))
              .append("\" alt=\"")
              .append(author.getNick())
              .append("\">")
              .append("</div>");

        } catch (IOException e) {
          throw new JspException("Error:" + e.getMessage());
        }
      }
    }

    return SKIP_BODY;
  }
}
