package ru.org.linux.util.images;

import org.imgscalr.Scalr;
import org.springframework.cache.annotation.Cacheable;
import org.w3c.dom.Node;
import ru.org.linux.util.BadImageException;

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
public class ImageUtil {

  public static String supportedFormat[] = {"JPEG", "gif", "png"};

  @Cacheable("ImageInfo")
  public static ImageInfo imageInfo(String filename) throws BadImageException, IOException {
    ImageInputStream iis = ImageIO.createImageInputStream(new File(filename));
    if(iis == null) {
      throw new BadImageException("Invalid image");
    }
    Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
    if(!iter.hasNext()) {
      throw new BadImageException("Invalid image");
    }
    ImageReader reader = iter.next();
    reader.setInput(iis, true, true);
    ImageInfo imageInfo = new ImageInfo(reader.getWidth(0), reader.getHeight(0));
    iis.close();
    return imageInfo;
  }

  public static ImageCheck imageCheck(String filename) throws BadImageException, IOException  {
    return imageCheck(new File(filename));
  }

  public static ImageCheck imageCheck(File file) throws BadImageException, IOException  {
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
      throw new BadImageException("Invalid image");
    }
    boolean animated = hasAnimatedPng(reader) || reader.getNumImages(true) > 1;
    int height = reader.getHeight(0);
    int width = reader.getWidth(0);
    iis.close();
    return new ImageCheck(formatName, animated, width, height, size);
  }

  private static boolean hasAnimatedPng(ImageReader reader) throws IOException {
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

  public static void resizeImage(String filename, String iconname, int size) throws IOException, BadImageException {
    BufferedImage source = ImageIO.read(new File(filename));
    BufferedImage destination = null;
    destination = Scalr.resize(source, size);
    ImageIO.write(destination, "JPEG", new File(iconname));
  }

}
