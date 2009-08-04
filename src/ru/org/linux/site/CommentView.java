/*
 * Copyright 1998-2009 Linux.org.ru
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

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.jsp.JspWriter;

import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;
import ru.org.linux.util.StringUtil;

public class CommentView {
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  private CommentView() {
  }

  public static void getUserpicHTML(Template tmpl, JspWriter out, User author) throws IOException {
    out.append("<div class=\"userpic\">");
    if (author.getPhoto() != null) {
      try {
        ImageInfo info = new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix() + "/photos/" + author.getPhoto());
        out.append("<img src=\"/photos/").append(author.getPhoto()).append("\" alt=\"").append(author.getNick()).append(" (фотография)\" ").append(info.getCode()).append(" >");
      } catch (BadImageException e) {
        logger.warning(StringUtil.getStackTrace(e));
      } catch (IOException e) {
        logger.warning(StringUtil.getStackTrace(e));
      }
    } else {
      if (author.hasGravatar()) {
        out.append("<img width=150 height=150 src=\""+author.getGravatar()+"\">");
      }
    }

    out.append("</div>");
  }
}
