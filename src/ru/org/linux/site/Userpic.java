package ru.org.linux.site;

import java.io.File;
import java.io.IOException;

import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;

public class Userpic {
  public static int MAX_USERPIC_FILESIZE = 25000;
  public static final int MIN_IMAGESIZE = 50;
  private static final int MAX_IMAGESIZE = 150;

  public static void checkUserpic(String filename) throws UserErrorException, IOException, BadImageException {
    File file = new File(filename);

    if (!file.isFile()) {
      throw new UserErrorException("Сбой загрузки изображения: не файл");
    }

    if (file.length() > MAX_USERPIC_FILESIZE) {
      throw new UserErrorException("Сбой загрузки изображения: слишком большой файл");      
    }

    String extension = ImageInfo.detectImageType(filename);

    ImageInfo info = new ImageInfo(filename, extension);

    if (info.getHeight()<MIN_IMAGESIZE || info.getHeight() > MAX_IMAGESIZE) {
      throw new UserErrorException("Сбой загрузки изображения: недопустимые размеры фотографии");
    }

    if (info.getWidth()<MIN_IMAGESIZE || info.getWidth() > MAX_IMAGESIZE) {
      throw new UserErrorException("Сбой загрузки изображения: недопустимые размеры фотографии");
    }
  }
}
