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

import net.tanesha.recaptcha.ReCaptcha;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.auth.IPBlockInfo;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;

/**
 */
public class Captcha extends TagSupport {
  private IPBlockInfo ipBlockInfo;

  public void setIpBlockInfo(IPBlockInfo ipBlockInfo) {
    this.ipBlockInfo = ipBlockInfo;
  }

  @Override
  public int doStartTag() throws JspException {
    JspWriter out = pageContext.getOut();
    try {
      WebApplicationContext ctx =
          WebApplicationContextUtils.getWebApplicationContext(pageContext.getServletContext());
      ReCaptcha captcha = (ReCaptcha) ctx.getBean("reCaptcha");
      if(AuthUtil.isSessionAuthorized() || ipBlockInfo != null && ipBlockInfo.isCaptchaRequired()) {
        out
            .append("<p>")
            .append(captcha.createRecaptchaHtml(null, null));
      }
    } catch (IOException e) {
      throw new JspException("Error:" + e.getMessage());
    }
    return SKIP_BODY;
  }

}
