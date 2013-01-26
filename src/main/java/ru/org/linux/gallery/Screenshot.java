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

package ru.org.linux.gallery;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.Errors;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.images.ImageCheck;
import ru.org.linux.util.UtilException;
import ru.org.linux.util.images.ImageInfo;
import ru.org.linux.util.images.ImageUtil;

import java.io.File;
import java.io.IOException;

public class Screenshot {
  private static final Log logger = LogFactory.getLog(Screenshot.class);
  public static final int MAX_SCREENSHOT_FILESIZE = 1500000;
  public static final int MIN_SCREENSHOT_SIZE = 400;
  public static final int MAX_SCREENSHOT_SIZE = 3000;

  private final String name;
  private final String ext;
  private final String path;

  private String fullName;
  private String mediumName;
  private String iconName;

  private static final int ICON_WIDTH = 200;
  private static final int MEDIUM_WIDTH = 500;

  public static Screenshot createScreenshot(File file, Errors errors, String dir) throws IOException, BadImageException, UtilException {
    boolean error = false;

    if (!file.isFile()) {
      errors.reject(null, "Сбой загрузки изображения: не файл");
      error = true;
    }

    if (!file.canRead()) {
      errors.reject(null, "Сбой загрузки изображения: файл нельзя прочитать");
      error = true;
    }

    if (file.length() > MAX_SCREENSHOT_FILESIZE) {
      errors.reject(null, "Сбой загрузки изображения: слишком большой файл");
      error = true;
    }

    ImageCheck check = ImageUtil.imageCheck(file);

    if (check.getHeight()< MIN_SCREENSHOT_SIZE || check.getHeight() > MAX_SCREENSHOT_SIZE) {
      errors.reject(null, "Сбой загрузки изображения: недопустимые размеры изображения");
      error = true;
    }

    if (check.getWidth()<MIN_SCREENSHOT_SIZE || check.getWidth() > MAX_SCREENSHOT_SIZE) {
      errors.reject(null, "Сбой загрузки изображения: недопустимые размеры изображения");
      error = true;
    }

    if (!error) {
      File tempFile = File.createTempFile("preview-", "", new File(dir));

      try {
        String name = tempFile.getName();

        Screenshot scrn = new Screenshot(name, dir, check.getExtension());

        scrn.doResize(file);

        return scrn;
      } finally {
        tempFile.delete();
      }
    } else {
      return null;
    }
  }

  private Screenshot(String name, String path, String extension) {
    this.name = name;
    this.path = path;
    this.ext = extension;
  }

  public Screenshot moveTo(String dir, String name) throws IOException, UtilException, BadImageException {
    Screenshot dest = new Screenshot(name, dir, ext);
    dest.doResize(new File(fullName));
    new File(mediumName).delete();
    new File(iconName).delete();
    return dest;
  }

  private void doResize(File uploadedFile) throws IOException, UtilException, BadImageException {
    ImageCheck check = ImageUtil.imageCheck(uploadedFile);
    File mainFile = new File(path, name + "-w" + check.getWidth() + "-h" + check.getHeight() + '.' + check.getExtension());
    fullName = mainFile.getAbsolutePath();
    if (mainFile.exists()) {
      mainFile.delete();
    }
    
    FileUtils.moveFile(uploadedFile, mainFile);

    boolean error = true;

    File mediumFile=null;
    File iconFile=null;

    try {
      iconName = ImageUtil.resizeImage(mainFile.getAbsolutePath(), name, path, "-icon", ICON_WIDTH);
      mediumName = ImageUtil.resizeImage(mainFile.getAbsolutePath(), name, path, "-med", MEDIUM_WIDTH);
      iconFile = new File(iconName);
      mediumFile = new File(mediumName);
      error = false;
    } catch (Exception e) {
      logger.debug(e.getMessage());
    } finally {
      if (error) {
        logger.debug("error move");
        if (mainFile.exists()) {
          mainFile.delete();
        }

        if (iconFile != null && iconFile.exists()) {
          iconFile.delete();
        }

        if (mediumFile != null && mediumFile.exists()) {
          iconFile.delete();
        }
      }
    }
  }

  public File getMainFile() {
    return new File(fullName);
  }

  public File getIconFile() {
    return new File(iconName);
  }
}
