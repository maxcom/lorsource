/*
 * Copyright 1998-2017 Linux.org.ru
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

import ru.org.linux.util.BadImageException;
import ru.org.linux.util.image.ImageUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class UploadedImagePreview {
  public static final int MAX_SCREENSHOT_FILESIZE = 3*1024*1024;
  public static final int MIN_SCREENSHOT_SIZE = 400;
  public static final int MAX_SCREENSHOT_SIZE = 5120;

  private final File mainFile;
  private final File mediumFile;
  private final File medium2xFile;
  private final String extension;

  UploadedImagePreview(String name, File path, String extension) {
    mainFile = new File(path, name + '.' + extension);
    mediumFile = new File(path, name + "-med.jpg");
    medium2xFile = new File(path, name + "-med-2x.jpg");

    this.extension = extension;
  }

  public void moveTo(File dir, String name) throws IOException {
    File target = Files.createDirectory(new File(dir, name).toPath()).toFile();

    Files.move(mainFile.toPath(), new File(target, "original."+extension).toPath());
    Files.move(mediumFile.toPath(), new File(target, Image.MediumWidth()+"px.jpg").toPath());
    Files.move(medium2xFile.toPath(), new File(target, Image.Medium2xWidth()+"px.jpg").toPath());
  }

  public String getExtension() {
    return extension;
  }

  void doResize(File uploadedFile) throws IOException, BadImageException {
    Files.move(uploadedFile.toPath(), mainFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

    boolean error = true;

    try {
      ImageUtil.resizeImage(mainFile.getAbsolutePath(), mediumFile.getAbsolutePath(), Image.MediumWidth());
      ImageUtil.resizeImage(mainFile.getAbsolutePath(), medium2xFile.getAbsolutePath(), Image.Medium2xWidth());
      error = false;
    } finally {
      if (error) {
        Files.deleteIfExists(mainFile.toPath());
        Files.deleteIfExists(mediumFile.toPath());
        Files.deleteIfExists(medium2xFile.toPath());
      }
    }
  }

  public File getMainFile() {
    return mainFile;
  }
}
