/*
 * Copyright 1998-2013 Linux.org.ru
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
import org.springframework.validation.Errors;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.image.ImageParam;
import ru.org.linux.util.image.ImageUtil;

import java.io.File;
import java.io.IOException;

public class Screenshot {
  public static final int MAX_SCREENSHOT_FILESIZE = 1500000;
  public static final int MIN_SCREENSHOT_SIZE = 400;
  public static final int MAX_SCREENSHOT_SIZE = 3000;

  private final File mainFile;
  private final File mediumFile;
  private final File iconFile;
  private final String extension;

  private static final int ICON_WIDTH = 200;
  private static final int MEDIUM_WIDTH = 500;

  public static Screenshot createScreenshot(File file, Errors errors, String dir) throws IOException, BadImageException {
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

    ImageParam imageParam = ImageUtil.imageCheck(file);

    if (imageParam.getHeight()< MIN_SCREENSHOT_SIZE || imageParam.getHeight() > MAX_SCREENSHOT_SIZE) {
      errors.reject(null, "Сбой загрузки изображения: недопустимые размеры изображения");
      error = true;
    }

    if (imageParam.getWidth()<MIN_SCREENSHOT_SIZE || imageParam.getWidth() > MAX_SCREENSHOT_SIZE) {
      errors.reject(null, "Сбой загрузки изображения: недопустимые размеры изображения");
      error = true;
    }

    if (!error) {
      File tempFile = File.createTempFile("preview-", "", new File(dir));

      try {
        String name = tempFile.getName();

        Screenshot scrn = new Screenshot(name, dir, imageParam.getExtension());

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
    String mainname = name + '.' + extension;
    String iconname = name + "-icon.jpg";
    String medname = name + "-med.jpg";

    mainFile = new File(path, mainname);
    iconFile = new File(path, iconname);
    mediumFile = new File(path, medname);

    this.extension = extension;
  }

  public Screenshot moveTo(String dir, String name) throws IOException {
    Screenshot dest = new Screenshot(name, dir, extension);

    FileUtils.moveFile(mainFile, dest.mainFile);
    FileUtils.moveFile(iconFile, dest.iconFile);
    FileUtils.moveFile(mediumFile, dest.mediumFile);

    return dest;
  }

  private void doResize(File uploadedFile) throws IOException, BadImageException {
    if (mainFile.exists()) {
      mainFile.delete();
    }
    
    FileUtils.moveFile(uploadedFile, mainFile);

    boolean error = true;

    try {
      ImageUtil.resizeImage(mainFile.getAbsolutePath(), iconFile.getAbsolutePath(), ICON_WIDTH);
      ImageUtil.resizeImage(mainFile.getAbsolutePath(), mediumFile.getAbsolutePath(), MEDIUM_WIDTH);
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

  public File getMainFile() {
    return mainFile;
  }

  public File getIconFile() {
    return iconFile;
  }
}
