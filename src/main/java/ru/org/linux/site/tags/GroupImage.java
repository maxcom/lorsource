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

import ru.org.linux.group.Group;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;

/**
 */
public class GroupImage extends TagSupport {
  private Group group;
  private String htmlPath;
  private String style;

  public void setGroup(Group group) {
    this.group = group;
  }

  public void setHtmlPath(String htmlPath) {
    this.htmlPath = htmlPath;
  }

  public void setStyle(String style) {
    this.style = style;
  }

  @Override
  public int doStartTag() throws JspException {
    JspWriter out = pageContext.getOut();
    try {
      String image = group.getImage();
      try {
        ImageInfo info = new ImageInfo(htmlPath + style + image);
        out.append("<img src=\"/").append(style).append(image).append("\" ").append(info.getCode()).append(" border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
      } catch (IOException e) {
        out.append("[bad image] <img class=newsimage src=\"/").append(style).append(image).append("\" " + " border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
      } catch (BadImageException e) {
        out.append("[bad image] <img class=newsimage src=\"/").append(style).append(image).append("\" " + " border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
      }
    } catch (IOException e) {
      throw new JspException("Error:" + e.getMessage());
    }
    return SKIP_BODY;
  }
}