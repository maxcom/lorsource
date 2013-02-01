package ru.org.linux.site.tags;

import com.google.common.collect.Maps;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.JadeTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import ru.org.linux.site.DateFormats;
import ru.org.linux.user.ApiUserRef;

import javax.servlet.jsp.JspException;
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
  public int doStartTag() throws JspException {
    WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(pageContext.getServletContext());

    JadeConfiguration jadeConfiguration = context.getBean(JadeConfiguration.class);
    JadeTemplate jadeTemplate = context.getBean("TemplateSign", JadeTemplate.class);

    Map<String, Object> data = Maps.newHashMap();

    data.put("user", user);
    data.put("shortMode", shortMode);
    data.put("author", author);
    data.put("postdate", postdate);

    // TODO: move to globals
    data.put("dateFormat", new DateFormatHandler());

    if (timeprop!=null) {
      data.put("timeprop", timeprop);
    }

    jadeConfiguration.renderTemplate(jadeTemplate, data, pageContext.getOut());

/*
    ObjectMapper mapper = new ObjectMapper();
    try {
      pageContext.getOut().append(mapper.writer().writeValueAsString(data.get("user")));
    } catch (Exception e) {
      e.printStackTrace();
     // throw new RuntimeException(e);
    }
*/

    return SKIP_BODY;
  }

  public static class DateFormatHandler {
    public String apply(Date input) {
      return DateFormats.getShort().print(input.getTime());
    }

    public String iso(Date input) {
      return DateFormats.iso8601().print(input.getTime());
    }
  }
}
