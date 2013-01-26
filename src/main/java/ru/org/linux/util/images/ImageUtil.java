package ru.org.linux.util.images;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.imgscalr.Scalr;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class ImageUtil {

  private static final Log logger = LogFactory.getLog(ImageUtil.class);

  public static String supportedFormat[] = {"JPEG", "gif", "png"};
  private static Pattern pattern = Pattern.compile("\\w+(?:-\\w+)?-w(\\d+)?-h(\\d+)");

  public static ImageInfo imageInfo(String filename) throws BadImageException, IOException {
    return imageInfo(new File(filename));
  }

  public static ImageInfo imageInfo(File file) throws BadImageException, IOException {
    Matcher matcher = pattern.matcher(file.getName());
    if(matcher.find()) {
      int width = Integer.parseInt(matcher.group(1));
      int height = Integer.parseInt(matcher.group(2));
      logger.debug("get info from filename " + file.getName() + ":" + width + "x" + height);
      return new ImageInfo(width, height, file.length());
    } else {
      ImageInputStream iis = ImageIO.createImageInputStream(file);
      if(iis == null) {
        throw new BadImageException("Invalid image");
      }
      Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
      if(!iter.hasNext()) {
        throw new BadImageException("Invalid image");
      }
      ImageReader reader = iter.next();
      reader.setInput(iis, true, true);
      ImageInfo imageInfo = new ImageInfo(reader.getWidth(0), reader.getHeight(0), file.length());
      iis.close();
      return imageInfo;
    }
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

  public static String resizeImage(String filename, String name, String path, String suffix, int size) throws IOException, BadImageException {
    BufferedImage source = ImageIO.read(new File(filename));
    BufferedImage destination = null;
    destination = Scalr.resize(source, size);
    int w = destination.getWidth();
    int h = destination.getHeight();
    File f = new File(path, name + "-w" + w + "-h" + h + suffix + ".jpg");
    ImageIO.write(destination, "JPEG", f);
    return f.getAbsolutePath();
  }

}
