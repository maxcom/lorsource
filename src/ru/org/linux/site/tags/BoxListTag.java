package ru.org.linux.site.tags;

import java.io.IOException;
import java.util.List;

import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.JspException;
import javax.servlet.ServletException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Closure;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;

import ru.org.linux.site.Template;
import ru.org.linux.site.cli.mkdefprofile;


public class BoxListTag extends BodyTagSupport {
  private String object;
  private String var;

  public String getObject() {
    return object;
  }

  public void setObject(String object) {
    this.object = object;
  }

  public String getVar() {
    return var;
  }

  public void setVar(String var) {
    this.var = var;
  }

  @Override
  public int doStartTag() throws JspException {
    Template t = Template.getTemplate(pageContext.getRequest());
    String s = getObject();
    if (StringUtils.isEmpty(s)){
      s = "main2";
    }
    @SuppressWarnings("unchecked")
    List<String> boxnames = (List<String>) t.getProf().getObject(s);
    CollectionUtils.filter(boxnames, new Predicate() {
      @Override
      public boolean evaluate(Object o) {
        String s = (String) o;
        return mkdefprofile.isBox(s);
      }
    });
    pageContext.setAttribute(var, boxnames);
    return EVAL_BODY_INCLUDE;
  }

  @Override
  public int doEndTag() throws JspException {
    pageContext.removeAttribute(var);
    return SKIP_BODY;
  }
}
