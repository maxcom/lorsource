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

package ru.org.linux.site;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;
import ru.org.linux.util.UtilException;

public class ScreenshotProcessor {
  public static final int MAX_SCREENSHOT_FILESIZE = 1000000;
  public static final int MIN_SCREENSHOT_SIZE = 400;
  public static final int MAX_SCREENSHOT_SIZE = 3000;

  private final File file;
  private final String extension;

  private File mainFile;
  private File mediumFile;
  private File iconFile;
  private static final Pattern GALLERY_NAME = Pattern.compile("(gallery/[^.]+)(\\.\\w+)");
  private static final int ICON_WIDTH = 200;
  private static final int MEDIUM_WIDTH = 500;

  public ScreenshotProcessor(String filename) throws IOException, BadImageException {
    file = new File(filename);

    if (!file.isFile()) {
      throw new BadImageException("Сбой загрузки изображения: не файл");
    }

    if (file.length() > MAX_SCREENSHOT_FILESIZE) {
      throw new BadImageException("Сбой загрузки изображения: слишком большой файл");
    }

    extension = ImageInfo.detectImageType(file);

    ImageInfo info = new ImageInfo(filename, extension);

    if (info.getHeight()< MIN_SCREENSHOT_SIZE || info.getHeight() > MAX_SCREENSHOT_SIZE) {
      throw new BadImageException("Сбой загрузки изображения: недопустимые размеры изображения");
    }

    if (info.getWidth()<MIN_SCREENSHOT_SIZE || info.getWidth() > MAX_SCREENSHOT_SIZE) {
      throw new BadImageException("Сбой загрузки изображения: недопустимые размеры изображения");
    }
  }

  private void initFiles(String name, String path) {
    String mainname = name + '.' + extension;
    String iconname = name + "-icon.jpg";
    String medname = name + "-med.jpg";

    mainFile = new File(path, mainname);
    iconFile = new File(path, iconname);
    mediumFile = new File(path, medname);
  }

  public void copyScreenshotFromPreview(Template tmpl, int msgid) throws IOException, UtilException, InterruptedException {
    initFiles(Integer.toString(msgid), tmpl.getObjectConfig().getHTMLPathPrefix() + "/gallery");

    doResize();
  }

  private void doResize() throws IOException, UtilException, InterruptedException {
    file.renameTo(mainFile);

    boolean error = true;

    try {
      ImageInfo.resizeImage(mainFile.getAbsolutePath(), iconFile.getAbsolutePath(), ICON_WIDTH);
      ImageInfo.resizeImage(mainFile.getAbsolutePath(), mediumFile.getAbsolutePath(), MEDIUM_WIDTH);
      error = false;
    } finally {
      if (error) {
        if (mainFile.exists()) {
          mainFile.delete();
        }

        if (iconFile.exists()) {
          iconFile.delete();
        }

        if (mediumFile.exists()) {
          iconFile.delete();
        }
      }
    }
  }

  public void copyScreenshot(Template tmpl, String sessionId) throws IOException, UtilException, InterruptedException {
    initFiles(sessionId, tmpl.getObjectConfig().getHTMLPathPrefix() + "/gallery/preview");

    doResize();
  }

  public File getMainFile() {
    return mainFile;
  }

  public File getIconFile() {
    return iconFile;
  }

  public File getMediumFile() {
    return mediumFile;
  }

  public static String getMediumName(String name) {
    Matcher m = GALLERY_NAME.matcher(name);

    if (!m.matches()) {
      throw new IllegalArgumentException("Not gallery path: "+name);
    }

    return m.group(1)+"-med.jpg";
  }
}
