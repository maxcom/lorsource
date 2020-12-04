/*
 * Copyright 1998-2019 Linux.org.ru
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
package ru.org.linux.util.image;

import com.drew.imaging.riff.RiffProcessingException;
import com.drew.imaging.webp.WebpMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.webp.WebpDirectory;
import org.imgscalr.Scalr;
import org.w3c.dom.Node;
import ru.org.linux.util.BadImageException;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/**
 */
public class ImageUtil {
  public static String[] supportedFormat = {"JPEG", "gif", "png", "WebP"};

  /**
   * Get image info without animation
   * @param file
   * @return
   * @throws BadImageException
   * @throws IOException
   */
  public static ImageParam imageInfo(File file) throws BadImageException, IOException {
    long size = file.length();
    ImageInputStream iis = ImageIO.createImageInputStream(file);
    if(iis == null) {
      throw new BadImageException("Invalid image");
    }
    Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
    if(!iter.hasNext()) {
      throw new BadImageException("Invalid image");
    }
    ImageReader reader = iter.next();
    reader.setInput(iis);
    String formatName = reader.getFormatName();
    if(!Arrays.asList(supportedFormat).contains(formatName)) {
      throw new BadImageException("Does unsupported format "+formatName);
    }
    boolean animated = false;
    int height = reader.getHeight(0);
    int width = reader.getWidth(0);
    iis.close();
    return new ImageParam(formatName, animated, height, width, size);
}

  public static ImageParam imageCheck(File file) throws BadImageException, IOException, MetadataException, RiffProcessingException {
    long size = file.length();
    ImageInputStream iis = ImageIO.createImageInputStream(file);
    if(iis == null) {
      throw new BadImageException("Invalid image");
    }
    Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
    if(!iter.hasNext()) {
      throw new BadImageException("Invalid image");
    }
    ImageReader reader = iter.next();
    reader.setInput(iis);
    String formatName = reader.getFormatName();
    if(!Arrays.asList(supportedFormat).contains(formatName)) {
      throw new BadImageException("Does unsupported format "+formatName);
    }
    boolean animated = hasAnimatedPng(reader) || hasAnimatedWebp(file, reader) || reader.getNumImages(true) > 1;
    int height = reader.getHeight(0);
    int width = reader.getWidth(0);
    iis.close();
    return new ImageParam(formatName, animated, height, width, size);
  }

  private static boolean hasAnimatedPng(ImageReader reader) throws IOException {
    if(! "png".equals(reader.getFormatName())) {
      return false;
    }
    try {
      IIOMetadata metadata = reader.getImageMetadata(0);
      XPath xPath = XPathFactory.newInstance().newXPath();

      for(String name : metadata.getMetadataFormatNames()) {
        Node root = metadata.getAsTree(name);
        if((Boolean)xPath.evaluate("//UnknownChunk[@type='acTL'] | //UnknownChunk[@type='fcTL']", root, XPathConstants.BOOLEAN)) {
          return true;
        }
      }
    } catch (XPathExpressionException e) {
      throw new IOException(e.getMessage());
    }
    return false;
  }

  private static boolean hasAnimatedWebp(File file, ImageReader reader) throws IOException, MetadataException, RiffProcessingException {
    if(! "WebP".equals(reader.getFormatName())) {
      return false;
    }

    Metadata metadata = WebpMetadataReader.readMetadata(file);
    WebpDirectory directory = metadata.getFirstDirectoryOfType(WebpDirectory.class);

    if(directory.containsTag(WebpDirectory.TAG_IS_ANIMATION)) {
      return directory.getBoolean(WebpDirectory.TAG_IS_ANIMATION);
    } else {
      return false;
    }
  }

  private static BufferedImage removeTransparency(BufferedImage image) {
    BufferedImage outImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
    Graphics2D g = outImage.createGraphics();
    g.drawImage(image, 0, 0, outImage.getWidth(), outImage.getHeight(), Color.WHITE, null);
    return outImage;
  }

  public static void resizeImage(String filename, String iconname, int size) throws IOException, BadImageException {
    try {
      BufferedImage source = ImageIO.read(new File(filename));
      BufferedImage destination = Scalr.resize(source, Scalr.Mode.FIT_TO_WIDTH, size);
      // openjdk cannot write JPEG file if image has transparency
      ImageIO.write(removeTransparency(destination), "JPEG", new File(iconname));
    } catch (IIOException ex) {
      throw new BadImageException("Can't resize image", ex);
    }
  }
}

