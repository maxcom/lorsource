package ru.org.linux.site.tags;

import java.io.IOException;

import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.JspException;
import javax.servlet.ServletException;

/**
 * ???
 * User: sreentenko
 * Date: 01.05.2009
 * Time: 0:32:07
 */
public class BoxListTag extends TagSupport {
  private String object;
  private String mode;

  public enum Mode {VIEW,EDIT}

  public String getObject() {
    return object;
  }

  public void setObject(String object) {
    this.object = object;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  @Override
  public int doEndTag() throws JspException {
    //todo: after rw boxlets as Spring controllers,
    //add here, depending on object value
    try {
      pageContext.include("/gallery.boxlet");
    } catch (ServletException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    return SKIP_BODY;
  }
}
