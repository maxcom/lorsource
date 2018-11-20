package ru.org.linux.util;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.user.Profile;
import ru.org.linux.util.bbcode.LorCodeService;
import ru.org.linux.util.formatter.ToLorCodeFormatter;
import ru.org.linux.util.formatter.ToLorCodeTexFormatter;
import ru.org.linux.util.markdown.MarkdownFormatter;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Created by bvn13 on 11.11.2018.
 */
@Service
public class CommonMessageFormatter {

  @Autowired
  private ToLorCodeFormatter toLorCodeFormatter;

  @Autowired
  private ToLorCodeTexFormatter toLorCodeTexFormatter;

  @Autowired
  @Qualifier("flexmark")
  private MarkdownFormatter markdownFormatter;

  @Autowired
  private LorCodeService lorCodeService;

  public String detectFormat(boolean isLorcode, boolean isMarkdown) {
    if (isLorcode) {
      return "ntobr";
    } else if (isMarkdown) {
      return "markdown";
    } else {
      return "";
    }
  }

  /**
   * Обработать тект комментария посредством парсеров (LorCode или Tex).
   *
   * @param msg   текст комментария
   * @param mode  режим обработки
   * @return обработанная строка
   */
  public String processMessage(String msg, String mode) {
    if (msg == null) {
      return "";
    }

    if ("ntobr".equals(mode)) {
      return toLorCodeFormatter.format(msg);
    } else if ("markdown".equals(mode)) {
      return markdownFormatter.renderToHtml(msg);
    } else {
      return toLorCodeTexFormatter.format(msg);
    }
  }

  public String getFormatToStoreInDB(String formatFromProfile, @Nullable String messageFormatMode) {
    return ((messageFormatMode != null ? messageFormatMode : formatFromProfile).equalsIgnoreCase("markdown") ? "MARKDOWN" : "BBCODE_TEX");
  }

  public String getFormatToStoreInDB(Profile profile, @Nullable String messageFormatMode) {
    return getFormatToStoreInDB(profile.getFormatMode(), messageFormatMode);
  }

  public String getFormatToStoreInDB(Profile profile) {
    return getFormatToStoreInDB(profile.getFormatMode(), null);
  }

  public Map<String, String> getModes() {
    return ImmutableMap.of("lorcode", "LORCODE", "ntobr", "User line break", "markdown", "Markdown");
  }

  /**
   * Получить html представление текста комментария
   *
   * @param messageText текст комментария
   * @return строку html комментария
   */
  public String prepareCommentText(MessageText messageText, boolean nofollow) {
    if (messageText.isLorcode()) {
      return lorCodeService.parseComment(messageText.getText(), nofollow);
    } else if (messageText.isMarkdown()) {
      return markdownFormatter.renderToHtml(messageText.getText());
    } else {
      return "<p>" + messageText.getText() + "</p>";
    }
  }

  public String prepareCommentTextRSS(MessageText messageText) {
    if (messageText.isLorcode()) {
      return lorCodeService.parseCommentRSS(messageText.getText());
    } else if (messageText.isMarkdown()) {
      return markdownFormatter.renderToHtml(messageText.getText());
    } else {
      return "<p>" + messageText.getText() + "</p>";
    }
  }

}
