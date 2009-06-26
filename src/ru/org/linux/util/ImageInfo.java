/*
 * Copyright 1998-2009 Linux.org.ru
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Gets image dimensions by parsing file headers.
 * <p/>
 * currently supported file types: Jpeg Gif Png
 */
public class ImageInfo{
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  private int height = -1;
  private int width = -1;
  private int size = 0;

  @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
  public static void main(String[] args) throws Exception {
    ImageInfo info = new ImageInfo(args[0]);

    if (info.width > info.height) {
      System.out.print("horizontal ");
    } else {
      System.out.print("vertical ");
    }

    System.out.println(info.width + " " + info.height);
  }

  public static String detectImageType(String filename) throws BadImageException, IOException {
    File file = new File(filename);

    logger.fine("Detecting image type for: " + filename+ " ("+file.length()+" bytes)");

    ImageInfo2 ii = new ImageInfo2();

    FileInputStream is = null;

    try {
      is = new FileInputStream(filename);

      ii.setInput(is);

      ii.check();
      
      int format = ii.getFormat();

      switch (format) {
        case ImageInfo2.FORMAT_GIF:
          return "gif";
        case ImageInfo2.FORMAT_JPEG:
          return "jpg";
        case ImageInfo2.FORMAT_PNG:
          return "png";
        default:
          throw new BadImageException("Unsupported format: " + ii.getMimeType());
      }
    } finally {
      if (is != null) {
        is.close();
      }
    }
  }

  /**
   * constructs image from filename
   * <p/>
   * file type is determined from file's extension
   */
  public ImageInfo(String filename) throws BadImageException, IOException {
    FileInputStream fileStream = null;

    try {
      fileStream = new FileInputStream(filename);
      size = fileStream.available();

      String lowname = filename.toLowerCase();

      if (lowname.endsWith("gif")) {
        getGifInfo(fileStream);
      } else if (lowname.endsWith("jpg") || lowname.endsWith("jpeg")) {
        getJpgInfo(fileStream);
      } else if (lowname.endsWith("png")) {
        getPngInfo(fileStream);
      } else {
        throw new BadImageException("Invalid image extension");        
      }

      if (height == -1 || width == -1) {
        throw new BadImageException();
      }
    } finally {
      if (fileStream != null) {
        fileStream.close();
      }
    }
  }

  public ImageInfo(String filename, String extension) throws BadImageException, IOException {
    FileInputStream fileStream = null;

    try {
      fileStream = new FileInputStream(filename);
      size = fileStream.available();

      if ("gif".equals(extension)) {
        getGifInfo(fileStream);
      } else if ("jpg".equals(extension) || "jpeg".equals(extension)) {
        getJpgInfo(fileStream);
      } else if ("png".equals(extension)) {
        getPngInfo(fileStream);
      } else {
        throw new BadImageException("Invalid image extension");
      }

      if (height == -1 || width == -1) {
        throw new BadImageException();
      }
    } finally {
      if (fileStream != null) {
        fileStream.close();
      }
    }
  }

  private void getGifInfo(FileInputStream fileStream) throws IOException, BadImageException {
    byte[] bytes = new byte[13];
    int bytesread = fileStream.read(bytes);
    if (bytesread == 13) {
      String header = new String(bytes);
      if ("GIF".equals(header.substring(0, 3))) //It's a gif, continue processing
      {
        width = shortLittleEndian(bytes[6], bytes[7]);
        height = shortLittleEndian(bytes[8], bytes[9]);
      } else {
        throw new BadImageException();
      }
    }
  }

  private void getPngInfo(FileInputStream fileStream) throws IOException, BadImageException {
    byte[] bytes = new byte[24];
    int bytesread = fileStream.read(bytes);
    if (bytesread == 24) {
      String header = new String(bytes);
      if ("PNG".equals(header.substring(1, 4))) {
        width = intBigEndian(bytes[16], bytes[17], bytes[18], bytes[19]);
        height = intBigEndian(bytes[20], bytes[21], bytes[22], bytes[23]);
      } else {
        throw new BadImageException();
      }
    }
  }


  private void getJpgInfo(FileInputStream fileStream) throws IOException, BadImageException {
    if (fileStream.read() == 0xFF && fileStream.read() == 0xD8) {
      while (true) {
        int marker;
        do {
          marker = fileStream.read();
        } while (marker != 0xFF);
        do {
          marker = fileStream.read();
        } while (marker == 0xFF);

        if (((marker >= 0xC0) && (marker <= 0xC3)) || ((marker >= 0xC5) && (marker <= 0xCB)) || ((marker >= 0xCD) && (marker <= 0xCF)))
        {
          fileStream.skip(3);
          height = shortBigEndian((byte) fileStream.read(), (byte) fileStream.read());
          width = shortBigEndian((byte) fileStream.read(), (byte) fileStream.read());
          break;
        } else {
          fileStream.skip(shortBigEndian((byte) fileStream.read(), (byte) fileStream.read()) - 2);
        }
      }
    } else {
      throw new BadImageException();
    }
  }

  private short shortBigEndian(byte firstRead, byte lastRead) {
    return (short) (((firstRead & 0xFF) << 8) | lastRead & 0xFF);
  }

  private short shortLittleEndian(byte firstRead, byte lastRead) {
    return shortBigEndian(lastRead, firstRead);
  }

  private int intBigEndian(byte a1, byte a2, byte a3, byte a4) {
    return ((a1 & 0xFF) << 24) | ((a2 & 0xFF) << 16) | ((a3 & 0xFF) << 8) | a4 & 0xFF;
  }

  private int intLittleEndian(byte a1, byte a2, byte a3, byte a4) {
    return intBigEndian(a4, a3, a2, a1);
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  /**
   * get file size
   */
  public int getSize() {
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
    return "width=" + width + " height=" + height;
  }

  public static void resizeImage(String filename, String iconname, int size) throws IOException, UtilException, InterruptedException {
    String[] cmd = {
      "/usr/bin/convert",
      "-scale",
      Integer.toString(size),
      filename,
      iconname };

    Process proc = Runtime.getRuntime().exec(cmd);

    int exitStatus = proc.waitFor();

    if (exitStatus!=0) {
      throw new UtilException("Can't convert image: convert failed");
    }
  }
}
