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

import org.imgscalr.Scalr;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/**
 */
public class ImageCheck {

  public static String supportedFormat[] = {"JPEG", "gif", "png"};

  private final String formatName;
  private final boolean animated;
  private final int height;
  private final int width;
  private final long size;

  public ImageCheck(String filename) throws BadImageException, IOException {
    this(new File(filename));
  }

  public ImageCheck(File file) throws BadImageException, IOException {
    size = file.length();
    ImageInputStream iis = ImageIO.createImageInputStream(file);
    Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
    if(!iter.hasNext()) {
      throw new BadImageException("Invalid image");
    }
    ImageReader reader = iter.next();
    reader.setInput(iis);
    formatName = reader.getFormatName();
    if(!Arrays.asList(supportedFormat).contains(formatName)) {
      throw new BadImageException("Invalid image");
    }
    animated = hasAnimatedPng(reader) || reader.getNumImages(true) > 1;
    height = reader.getHeight(0);
    width = reader.getWidth(0);
    iis.close();
  }

  private boolean hasAnimatedPng(ImageReader reader) throws IOException {
    try {
      if(! "png".equals(reader.getFormatName())) {
        return false;
      }
      IIOMetadata metadata = reader.getImageMetadata(0);
      XPath xPath = XPathFactory.newInstance().newXPath();

      for(String name : metadata.getMetadataFormatNames()) {
        Node root = metadata.getAsTree(name);
        if((Boolean)xPath.evaluate("//UnknownChunk[@type='acTL'] | //UnknownChunk[@type='fcTL']", root, XPathConstants.BOOLEAN)) {
          return true;
        }
      }
      return false;
    } catch (XPathExpressionException e) {
      throw new IOException(e.getMessage());
    }
  }

  public String getFormatName() {
    return formatName;
  }

  public boolean isAnimated() {
    return animated;
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  public long getSize() {
    return size;
  }

  /**
   * get file size in user-printable form
   */
  public String getSizeString() {
    return size / 1024 + " Kb";
  }

  /**
   * get HTML code for inclusion into IMG tag
   */
  public String getCode() {
    return "width=\"" + width + "\" height=\"" + height + '"';
  }

  /**
   * Get extension for filename
   * @return ext
   */
  public String getExtension() {
    if("JPEG".equals(formatName)) {
      return "jpg";
    } else {
      return formatName;
    }
  }

  public static void resizeImage(String filename, String iconname, int size) throws IOException, BadImageException {
    ImageCheck check = new ImageCheck(filename);
    BufferedImage source = ImageIO.read(new File(filename));
    BufferedImage destination = null;
    destination = Scalr.resize(source, size);
    ImageIO.write(destination, "JPEG", new File(iconname));
  }
}
