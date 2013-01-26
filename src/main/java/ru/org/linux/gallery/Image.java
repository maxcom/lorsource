package ru.org.linux.gallery;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class  Image {
  private static final Pattern GALLERY_NAME = Pattern.compile("(gallery/[^.]+)(\\.\\w+)");

  private final int id;
  private final int topicId;
  private final String original;
  private final String icon;

  public Image(int id, int topicId, String original, String icon) {
    this.id = id;
    this.topicId = topicId;
    this.original = original;
    this.icon = icon;
  }

  public int getId() {
    return id;
  }

  public int getTopicId() {
    return topicId;
  }

  public String getOriginal() {
    return original;
  }

  public String getIcon() {
    return icon;
  }

  public String getMedium() {
    return getMediumName(original);
  }

  private static String getMediumName(String name) {
    Matcher m = GALLERY_NAME.matcher(name);

    if (!m.matches()) {
      throw new IllegalArgumentException("Not gallery path: "+name);
    }

    return m.group(1)+"-med.jpg";
  }
}
