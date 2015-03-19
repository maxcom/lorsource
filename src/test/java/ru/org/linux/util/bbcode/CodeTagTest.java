/*
 * Copyright 1998-2015 Linux.org.ru
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

package ru.org.linux.util.bbcode;

import org.apache.commons.httpclient.URI;
import org.junit.Before;
import org.junit.Test;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.user.UserDao;
import ru.org.linux.util.formatter.ToHtmlFormatter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CodeTagTest {
  private LorCodeService lorCodeService;

  @Before
  public void init() throws Exception {
    UserDao userDao = mock(UserDao.class);

    String mainUrl = "http://127.0.0.1:8080/";
    URI mainURI = new URI(mainUrl, true, "UTF-8");
    SiteConfig siteConfig = mock(SiteConfig.class);
    when(siteConfig.getMainURI()).thenReturn(mainURI);
    when(siteConfig.getMainUrl()).thenReturn(mainUrl);

    ToHtmlFormatter toHtmlFormatter = new ToHtmlFormatter();
    toHtmlFormatter.setSiteConfig(siteConfig);


    lorCodeService = new LorCodeService();
    lorCodeService.setUserDao(userDao);
    lorCodeService.setSiteConfig(siteConfig);
    lorCodeService.setToHtmlFormatter(toHtmlFormatter);
  }

  @Test
  public void codeTest() {
    assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>[list][*]one[*]two[*]three[/list]</code></pre></div>",
            lorCodeService.parseComment("[code][list][*]one[*]two[*]three[/list][/code]", false, false));
    assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>simple code</code></pre></div>",
            lorCodeService.parseComment("[code]\nsimple code[/code]", false, false));
    assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>[list][*]one[*]two[*]three[/list]</code></pre></div>",
            lorCodeService.parseComment("[code]\n[list][*]one[*]two[*]three[/list][/code]", false, false));
  }

  @Test
  public void codeSpacesTest() {
    assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>[url]test[/url] [url]test[/url]</code></pre></div>",
            lorCodeService.parseComment("[code][url]test[/url] [url]test[/url][/code]", false, false));
  }

  @Test
  public void codeCleanTest() {
    assertEquals("", lorCodeService.parseComment("[code][/code]", false, false));
  }

  @Test
  public void codeKnowTest() {
    assertEquals("<div class=\"code\"><pre class=\"language-cpp\"><code>#include &lt;stdio.h&gt;</code></pre></div>",
            lorCodeService.parseComment("[code=cxx]#include <stdio.h>[/code]", false, false));
  }

  @Test
  public void codeUnKnowTest() {
    assertEquals("<div class=\"code\"><pre class=\"no-highlight\"><code>#include &lt;stdio.h&gt;</code></pre></div>",
            lorCodeService.parseComment("[code=foo]#include <stdio.h>[/code]", false, false));
  }

}
