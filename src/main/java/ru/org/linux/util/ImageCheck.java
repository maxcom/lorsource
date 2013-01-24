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

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.xml.bind.NotIdentifiableEvent;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 */
public class ImageCheck {

  private final String formatName;
  private final boolean animated;
  private final int height;
  private final int width;

  public ImageCheck(File file) throws Exception {
    ImageInputStream iis = ImageIO.createImageInputStream(file);
    Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
    if(!iter.hasNext()) {
      throw new RuntimeException("No readers");
    }
    ImageReader reader = iter.next();
    reader.setInput(iis);
    formatName = reader.getFormatName();
    if("png".equals(formatName) && hasAnimatedPng(reader)) {
      animated = true;
    } else {
      animated = reader.getNumImages(true) > 1;
    }
    height = reader.getHeight(0);
    width = reader.getWidth(0);
    iis.close();
  }

  private boolean hasAnimatedPng(ImageReader reader) throws Exception {
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
}
