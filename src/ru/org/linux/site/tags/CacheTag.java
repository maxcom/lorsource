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

package ru.org.linux.site.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.org.linux.spring.commons.CacheProvider;
import ru.org.linux.spring.commons.MemCachedProvider;

/**
 * User: rsvato
 * Date: May 6, 2009
 * Time: 1:15:00 PM
 */
public class CacheTag extends BodyTagSupport {
  private static final Log log = LogFactory.getLog(CacheTag.class);
  private String key;
  private Long expire;
  private boolean foundInCache;

  private final CacheProvider provider = new MemCachedProvider();
  private static final long serialVersionUID = -5460272871141784844L;

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Long getExpire() {
    return expire;
  }

  public void setExpire(Long expire) {
    this.expire = expire;
  }

  @Override
  public int doStartTag() throws JspException {
    String cached = (String) provider.getFromCache(getKey());
    foundInCache = cached != null;
    if (foundInCache) {
      try {
        pageContext.getOut().write(cached);
      } catch (IOException e) {
        log.error(e);
      }
      return SKIP_BODY;
    }
    return EVAL_BODY_BUFFERED;
  }

  @Override
  public int doAfterBody() throws JspException {
    if (!foundInCache) {
      if (bodyContent != null) {
        String result = bodyContent.getString();
        provider.storeToCache(getKey(), result, getExpire());
        try {
          bodyContent.clearBody();
          bodyContent.write(result);
          bodyContent.writeOut(bodyContent.getEnclosingWriter());
        } catch (IOException e) {
          log.error(e);
        }
      }
    }
    return SKIP_BODY;
  }
}
