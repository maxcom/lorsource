package ru.org.linux.site.tags;

import com.google.common.collect.Maps;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.JadeTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import ru.org.linux.comment.PreparedComment;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import java.util.Map;

public class CommentTag extends TagSupport {
  private PreparedComment comment;
  private boolean enableSchema;

  public void setComment(PreparedComment comment) {
    this.comment = comment;
  }

  public void setEnableSchema(boolean enableSchema) {
    this.enableSchema = enableSchema;
  }

  @Override
  public int doStartTag() throws JspException {
    WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(pageContext.getServletContext());

    JadeConfiguration jadeConfiguration = context.getBean(JadeConfiguration.class);
    JadeTemplate jadeTemplate = context.getBean("TemplateComment", JadeTemplate.class);

    Map<String, Object> data = Maps.newHashMap();

    data.put("comment", comment);
    data.put("enableSchema", enableSchema);

    // TODO: move to globals
    data.put("dateFormat", new SignTag.DateFormatHandler());

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
