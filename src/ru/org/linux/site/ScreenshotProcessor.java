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

import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;
import ru.org.linux.util.UtilException;

public class ScreenshotProcessor {
  private static final int MAX_SCREENSHOT_FILESIZE = 500000;
  private static final int MIN_SCREENSHOT_SIZE = 400;
  private static final int MAX_SCREENSHOT_SIZE = 2048;

  private final File file;
  private final String extension;

  private File mainFile;
  private File iconFile;

  public ScreenshotProcessor(String filename) throws IOException, BadImageException {
    file = new File(filename);

    if (!file.isFile()) {
      throw new BadImageException("Сбой загрузки изображения: не файл");
    }

    if (file.length() > MAX_SCREENSHOT_FILESIZE) {
      throw new BadImageException("Сбой загрузки изображения: слишком большой файл");
    }

    extension = ImageInfo.detectImageType(filename);

    ImageInfo info = new ImageInfo(filename, extension);

    if (info.getHeight()< MIN_SCREENSHOT_SIZE || info.getHeight() > MAX_SCREENSHOT_SIZE) {
      throw new BadImageException("Сбой загрузки изображения: недопустимые размеры изображения");
    }

    if (info.getWidth()<MIN_SCREENSHOT_SIZE || info.getWidth() > MAX_SCREENSHOT_SIZE) {
      throw new BadImageException("Сбой загрузки изображения: недопустимые размеры изображения");
    }
  }

  public void copyScreenshot(Template tmpl, int msgid) throws IOException, UtilException, InterruptedException {
    String mainname = Integer.toString(msgid) + "." + extension;
    String iconname = Integer.toString(msgid) + "-icon" + "." + extension;

    mainFile = new File(tmpl.getObjectConfig().getHTMLPathPrefix() + "/gallery", mainname);
    iconFile = new File(tmpl.getObjectConfig().getHTMLPathPrefix() + "/gallery", iconname);

    file.renameTo(mainFile);

    boolean error = true;

    try {
      ImageInfo.resizeImage(mainFile.getAbsolutePath(), iconFile.getAbsolutePath());
      error = false;
    } finally {
      if (error) {
        if (mainFile.exists()) {
          mainFile.delete();
        }

        if (iconFile.exists()) {
          iconFile.delete();
        }
      }
    }
  }

  public void copyScreenshot(Template tmpl, String sessionId) throws IOException, UtilException, InterruptedException {
    String mainname = sessionId + "." + extension;
    String iconname = sessionId + "-icon" + "." + extension;

    mainFile = new File(tmpl.getObjectConfig().getHTMLPathPrefix() + "/gallery/preview", mainname);
    iconFile = new File(tmpl.getObjectConfig().getHTMLPathPrefix() + "/gallery/preview", iconname);

    file.renameTo(mainFile);

    boolean error = true;

    try {
      ImageInfo.resizeImage(mainFile.getAbsolutePath(), iconFile.getAbsolutePath());
      error = false;
    } finally {
      if (error) {
        if (mainFile.exists()) {
          mainFile.delete();
        }

        if (iconFile.exists()) {
          iconFile.delete();
        }
      }
    }
  }

  public File getMainFile() {
    return mainFile;
  }

  public File getIconFile() {
    return iconFile;
  }
}
