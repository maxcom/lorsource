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
