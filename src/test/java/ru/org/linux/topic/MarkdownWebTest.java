package ru.org.linux.topic;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.httpclient.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import ru.org.linux.csrf.CSRFProtectionService;
import ru.org.linux.section.Section;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.test.WebHelper;
import ru.org.linux.util.markdown.MarkdownFormatter;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by bvn13 on 18.11.2018.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = TopicIntegrationTestConfiguration.class)
@ImportResource({"classpath:database.xml", "classpath:common.xml"})
public class MarkdownWebTest {

  private static final int TEST_GROUP = 4068;
  private static final String[] TEST_USER = new String[] {"Shaman007", "maxcom", "Casus", "svu"};
  private static final String TEST_PASSWORD = "passwd";
  private static final String TEST_TITLE = "Test Title";
  private static final String TEST_MESSAGE = "# First header \n" +
          "\n" +
          "## Second Header\n" +
          "\n" +
          "```sql\n" +
          "select id from table1;\n" +
          "```\n" +
          "\n" +
          "Вот такой должно получиться\n" +
          "\n" +
          "И это тоже должно работать";
  private static final String TEST_MESSAGE_RESULT = "<h1>First header</h1> \n" +
          "<h2>Second Header</h2> \n" +
          "<pre><code class=\"language-sql\">select id from table1;\n" +
          "</code></pre> \n" +
          "<p>Вот такой должно получиться</p> \n" +
          "<p>И это тоже должно работать</p>";

  private static final String TEST_COMMENT = "# First comment header \n" +
          "\n" +
          "## Second comment Header\n" +
          "\n" +
          "```sql\n" +
          "select id from table2;\n" +
          "```\n" +
          "\n" +
          "Вот такой должен получиться комментарий";

  private static final String TEST_COMMENT_RESULT = "<h1>First comment header</h1>\n" +
          "<h2>Second comment Header</h2>\n" +
          "<pre><code class=\"language-sql\">select id from table2;\n" +
          "</code></pre>\n" +
          "<p>Вот такой должен получиться комментарий</p>\n";

  private static final String TEST_COMMENT_EDITED = "# Отредактированное сообщение \n" +
          "\n" +
          "## Second comment Header\n" +
          "\n" +
          "```sql\n" +
          "select id from table2;\n" +
          "```\n" +
          "\n" +
          "Вот такой должен получиться комментарий";

  private static final String TEST_COMMENT_EDITED_RESULT = "<h1>Отредактированное сообщение</h1>\n" +
          "<h2>Second comment Header</h2>\n" +
          "<pre><code class=\"language-sql\">select id from table2;\n" +
          "</code></pre>\n" +
          "<p>Вот такой должен получиться комментарий</p>\n";


  private static Pattern PATTERN_TOPIC_ID;
  private static Pattern PATTERN_COMMENT_ID;

  private WebResource resource;

  @Autowired
  private MsgbaseDao msgbaseDao;

  @Autowired
  @Qualifier("flexmark")
  private MarkdownFormatter markdownFormatter;

  private int topicId;


  @Before
  public void initResource() {
    Client client = new Client();

    client.setFollowRedirects(false);

    resource = client.resource(WebHelper.MAIN_URL);

    PATTERN_TOPIC_ID = Pattern.compile(""+resource.getURI().toString()+".+/(\\d+)");
    PATTERN_COMMENT_ID = Pattern.compile(""+resource.getURI().toString()+".+#comment-(\\d+)");
  }

  private int createTopic(int loginIndex) throws IOException {
    String auth = WebHelper.doLogin(resource, TEST_USER[loginIndex], TEST_PASSWORD);

    MultivaluedMap<String, String> formData = new MultivaluedMapImpl();

    formData.add("section", Integer.toString(Section.SECTION_FORUM));
    formData.add("group", Integer.toString(TEST_GROUP));
    formData.add("csrf", "csrf");
    formData.add("title", TEST_TITLE);
    formData.add("msg", TEST_MESSAGE);
    formData.add("mode", "markdown");

    ClientResponse cr = resource
            .path("add.jsp")
            .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
            .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
            .post(ClientResponse.class, formData);

    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    assertTrue(doc.select(".error").text(), doc.select("#messageForm").isEmpty());

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr.getStatus());

    ClientResponse tempPage = resource      // TODO remove temp redirect from Controller
            .uri(cr.getLocation())
            .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, tempPage.getStatus());

    ClientResponse page = resource
            .uri(tempPage.getLocation())
            .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_OK, page.getStatus());

    Document finalDoc = Jsoup.parse(page.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    assertEquals(TEST_MESSAGE_RESULT, finalDoc.select("div[itemprop='articleBody']").html());

    Matcher matcher = PATTERN_TOPIC_ID.matcher(tempPage.getLocation().toString());

    assertTrue(matcher.find());

    int topicId = Integer.parseInt(matcher.group(1));

    return topicId;
  }

  private int createComment(int topicId) throws IOException {

    String auth = WebHelper.doLogin(resource, TEST_USER[1], TEST_PASSWORD);

    MultivaluedMap<String, String> formData = new MultivaluedMapImpl();

    formData.add("csrf", "csrf");
    formData.add("topic", String.valueOf(topicId));
    formData.add("mode", "markdown");
    formData.add("msg", TEST_COMMENT);
    formData.add("title", "");

    ClientResponse cr = resource
            .path("add_comment.jsp")
            .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
            .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
            .post(ClientResponse.class, formData);

    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    assertTrue(doc.select(".error").text(), doc.select("#messageForm").isEmpty());

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr.getStatus());

    ClientResponse tempPage = resource
            .uri(cr.getLocation())
            .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, tempPage.getStatus());

    ClientResponse page = resource
            .uri(tempPage.getLocation())
            .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_OK, page.getStatus());

    Matcher matcher = PATTERN_COMMENT_ID.matcher(tempPage.getLocation().toString());

    assertTrue(matcher.find());

    int commentId = Integer.parseInt(matcher.group(1));

    Document finalDoc = Jsoup.parse(page.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    String commentText = finalDoc.select("article#comment-"+commentId+" .msg-container .msg_body").html();
    commentText = commentText.substring(0, commentText.indexOf("<div class=\"sign\">")).replace(" \n", "\n");

    assertEquals(TEST_COMMENT_RESULT, commentText);

    return commentId;
  }

  @Test
  public void testCreateTopicWithMarkdown() throws IOException, InterruptedException {
    Thread.sleep(30000);

    int topicId = createTopic(0);

    MessageText msg = msgbaseDao.getMessageText(topicId);

    assertTrue(msg.isMarkdown());
    assertFalse(msg.isLorcode());

    assertEquals(TEST_MESSAGE, msg.getText()); //was stored the same as the input text

    assertEquals(markdownFormatter.renderToHtml(TEST_MESSAGE), markdownFormatter.renderToHtml(msg.getText()));
  }

  @Test
  public void testCreateCommentWithMarkdown() throws IOException, InterruptedException {
    Thread.sleep(30000);

    int topicId = createTopic(1);

    int commentId = createComment(topicId);

    MessageText msg = msgbaseDao.getMessageText(commentId);

    assertEquals(TEST_COMMENT, msg.getText());

    assertTrue(msg.isMarkdown());
    assertFalse(msg.isLorcode());

    assertEquals(TEST_COMMENT_RESULT, markdownFormatter.renderToHtml(msg.getText()));

    assertEquals(markdownFormatter.renderToHtml(TEST_COMMENT), markdownFormatter.renderToHtml(msg.getText()));

  }

  @Test
  public void testEditCommentWithMarkdown() throws IOException, InterruptedException {
    Thread.sleep(30000);

    int topicId = createTopic(0);

    int commentId = createComment(topicId); // user 1

    String auth = WebHelper.doLogin(resource, TEST_USER[1], TEST_PASSWORD);

    MultivaluedMap<String, String> formData = new MultivaluedMapImpl();

    formData.add("csrf", "csrf");
    formData.add("topic", String.valueOf(topicId));
    formData.add("reploty", "0");
    formData.add("original", String.valueOf(commentId));
    formData.add("mode", "markdown");
    formData.add("title", "");
    formData.add("msg", TEST_COMMENT_EDITED);

    Thread.sleep(30000);

    ClientResponse cr = resource
            .path("edit_comment")
            .cookie(new Cookie(WebHelper.AUTH_COOKIE, auth, "/", "127.0.0.1", 1))
            .cookie(new Cookie(CSRFProtectionService.CSRF_COOKIE, "csrf"))
            .post(ClientResponse.class, formData);

    Document doc = Jsoup.parse(cr.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    assertTrue(doc.select(".error").text(), doc.select("#messageForm").isEmpty());

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, cr.getStatus());

    ClientResponse tempPage = resource
            .uri(cr.getLocation())
            .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, tempPage.getStatus());

    ClientResponse page = resource
            .uri(tempPage.getLocation())
            .get(ClientResponse.class);

    assertEquals(HttpStatus.SC_OK, page.getStatus());

    Matcher matcher = PATTERN_COMMENT_ID.matcher(tempPage.getLocation().toString());

    assertTrue(matcher.find());

    int commentEditedId = Integer.parseInt(matcher.group(1));

    assertEquals(commentId, commentEditedId);

    Document finalDoc = Jsoup.parse(page.getEntityInputStream(), "UTF-8", resource.getURI().toString());

    String commentText = finalDoc.select("article#comment-"+commentId+" .msg-container .msg_body").html();
    commentText = commentText.substring(0, commentText.indexOf("<div class=\"sign\">")).replace(" \n", "\n");

    assertEquals(TEST_COMMENT_EDITED_RESULT, commentText);
  }

}
