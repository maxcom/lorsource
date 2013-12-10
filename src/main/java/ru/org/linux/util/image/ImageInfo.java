/*
 * Copyright 1998-2013 Linux.org.ru
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

import ru.org.linux.util.BadImageException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Gets image dimensions by parsing file headers.
 * <p/>
 * currently supported file types: Jpeg Gif Png
 */
public class ImageInfo{
  private int height = -1;
  private int width = -1;
  private int size = 0;

  private final String filename;

  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public static void main(String[] args) throws Exception {
    ImageInfo info = new ImageInfo(args[0]);

    if (info.width > info.height) {
      System.out.print("horizontal ");
    } else {
      System.out.print("vertical ");
    }

    System.out.println(info.width + " " + info.height);
  }

  /**
   * constructs image from filename
   * <p/>
   * file type is determined from file's extension
   */
  public ImageInfo(String filename) throws BadImageException, IOException {
    this.filename = filename;

    FileInputStream fileStream = null;

    try {
      fileStream = new FileInputStream(filename);
      size = (int) new File(filename).length();

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

  public ImageInfo(File file, String extension) throws BadImageException, IOException {
    filename = file.getName();

    FileInputStream fileStream = null;

    try {
      fileStream = new FileInputStream(file);
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
        throw new BadImageException("Bad GIF image: "+filename);
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
        throw new BadImageException("Bad PNG image: "+filename);
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
          int skip = shortBigEndian((byte) fileStream.read(), (byte) fileStream.read()) - 2;

          if (skip<0) {
            throw new BadImageException("Bad JPG image: "+filename);
          }

          fileStream.skip(skip);
        }
      }
    } else {
      throw new BadImageException("Bad JPG image: "+filename);
    }
  }

  private static short shortBigEndian(byte firstRead, byte lastRead) {
    return (short) (((firstRead & 0xFF) << 8) | lastRead & 0xFF);
  }

  private static short shortLittleEndian(byte firstRead, byte lastRead) {
    return shortBigEndian(lastRead, firstRead);
  }

  private static int intBigEndian(byte a1, byte a2, byte a3, byte a4) {
    return ((a1 & 0xFF) << 24) | ((a2 & 0xFF) << 16) | ((a3 & 0xFF) << 8) | a4 & 0xFF;
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

}
