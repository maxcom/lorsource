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
import ru.org.linux.site.DateFormats;
import ru.org.linux.user.ApiUserRef;

import javax.servlet.jsp.tagext.TagSupport;
import java.util.Date;
import java.util.Map;

public class SignTag extends TagSupport {
  private ApiUserRef user;
  private boolean shortMode;
  private boolean author;
  private Date postdate;
  private String timeprop;

  public void setUser(ApiUserRef user) {
    this.user = user;
  }

  public void setShortMode(boolean shortMode) {
    this.shortMode = shortMode;
  }

  public void setAuthor(boolean author) {
    this.author = author;
  }

  public void setPostdate(Date postdate) {
    this.postdate = postdate;
  }

  public void setTimeprop(String timeprop) {
    this.timeprop = timeprop;
  }

  @Override
  public int doStartTag() {
    WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(pageContext.getServletContext());

    JadeConfiguration jadeConfiguration = context.getBean(JadeConfiguration.class);
    JadeTemplate jadeTemplate = context.getBean("TemplateSign", JadeTemplate.class);

    Map<String, Object> data = Maps.newHashMap();

    data.put("user", user);
    data.put("shortMode", shortMode);
    data.put("author", author);
    data.put("postdate", postdate);

    data.put("dateFormat", new SignTag.DateFormatHandler((DateTimeZone) pageContext.getRequest().getAttribute("timezone")));

    if (timeprop!=null) {
      data.put("timeprop", timeprop);
    }

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
      return DateFormats.iso8601().print(input.getTime());
    }
  }
}
