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
package ru.org.linux.util;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 */
public class ImageCheck {

  private final String formatName;
  private final int numImages;
  private final int height;
  private final int width;

  public ImageCheck(File file) throws IOException {
    ImageInputStream iis = ImageIO.createImageInputStream(file);
    Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
    if(!iter.hasNext()) {
      throw new RuntimeException("No readers");
    }
    ImageReader reader = iter.next();
    reader.setInput(iis, false, false);
    formatName = reader.getFormatName();
    numImages = reader.getNumImages(true);
    height = reader.getHeight(reader.getMinIndex());
    width = reader.getWidth(reader.getMinIndex());
    iis.close();
  }

  public String getFormatName() {
    return formatName;
  }

  public int getNumImages() {
    return numImages;
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }
}
