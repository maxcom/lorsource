package ru.org.linux.site.tags;

import com.google.common.collect.Maps;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.JadeTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.user.ApiUserService;
import ru.org.linux.user.User;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import java.util.Date;
import java.util.Map;

public class SignTag extends TagSupport {
  private User user;
  private boolean shortMode;
  private boolean author;
  private Date postdate;
  private String timeprop;

  public void setUser(User user) {
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

    ApiUserService apiUserService = context.getBean(ApiUserService.class);

    Map<String, Object> data = Maps.newHashMap();

    data.put("user", apiUserService.ref(user, AuthUtil.getCurrentUser()));
    data.put("shortMode", shortMode);
    data.put("author", author);
    data.put("postdate", postdate);

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
}
