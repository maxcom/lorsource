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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;

/**
 */
public class CommentsWithSuffix extends TagSupport {
  private int stat;

  public void setStat(int stat) {
    this.stat = stat;
  }

  @Override
  public int doStartTag() throws JspException {
    JspWriter out = pageContext.getOut();
    try {
      if (stat % 100 >= 10 && stat % 100 <= 20) {
        out.append("комментариев");
      } else {
        switch (stat % 10) {
          case 1:
            out.append("комментарий");
            break;
          case 2:
          case 3:
          case 4:
            out.append("комментария");
            break;
          default:
            out.append("комментариев");
            break;
        }
      }
    } catch (IOException e) {
      throw new JspException("Error:" + e.getMessage());
    }
    return SKIP_BODY;
  }
}
