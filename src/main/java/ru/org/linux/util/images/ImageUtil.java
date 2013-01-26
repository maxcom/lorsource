package ru.org.linux.util.images;

import ru.org.linux.util.BadImageException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 */
public class ImageUtil {

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
}
