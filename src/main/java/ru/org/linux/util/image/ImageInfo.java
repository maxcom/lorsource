/*
 * Copyright 1998-2016 Linux.org.ru
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

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.gif.GifMetadataReader;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.imaging.png.PngMetadataReader;
import com.drew.imaging.png.PngProcessingException;
import com.drew.imaging.riff.RiffProcessingException;
import com.drew.imaging.webp.WebpMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.gif.GifHeaderDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.png.PngDirectory;
import com.drew.metadata.webp.WebpDirectory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Gets image dimensions by parsing file headers.
 * <p/>
 * currently supported file types: Jpeg Gif Png WebP
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
  public ImageInfo(String filename) throws BadImageException, IOException, RiffProcessingException, MetadataException, PngProcessingException, JpegProcessingException {
    this.filename = filename;

    FileInputStream fileStream = null;
    // Требуется для работы metadata-extractor.
    BufferedInputStream bufferedFileStream = null;

    try {
      fileStream = new FileInputStream(filename);
      bufferedFileStream = new BufferedInputStream(fileStream);
      size = (int) new File(filename).length();

      String lowname = filename.toLowerCase();

      if (lowname.endsWith("gif")) {
        getGifInfo(bufferedFileStream);
      } else if (lowname.endsWith("jpg") || lowname.endsWith("jpeg")) {
        getJpgInfo(bufferedFileStream);
      } else if (lowname.endsWith("png")) {
        getPngInfo(bufferedFileStream);
      } else if (lowname.endsWith("webp")) {
        getWebpInfo(bufferedFileStream);
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

  public ImageInfo(File file, String extension) throws BadImageException, IOException, RiffProcessingException, MetadataException, JpegProcessingException, PngProcessingException {
    filename = file.getName();

    FileInputStream fileStream = null;
    // Требуется для работы metadata-extractor.
    BufferedInputStream bufferedFileStream = null;

    try {
      fileStream = new FileInputStream(file);
      bufferedFileStream = new BufferedInputStream(fileStream);
      size = fileStream.available();

      if ("gif".equals(extension)) {
        getGifInfo(bufferedFileStream);
      } else if ("jpg".equals(extension) || "jpeg".equals(extension)) {
        getJpgInfo(bufferedFileStream);
      } else if ("png".equals(extension)) {
        getPngInfo(bufferedFileStream);
      } else if ("webp".equals(extension)) {
        getWebpInfo(bufferedFileStream);
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

  private void getGifInfo(BufferedInputStream filteredFileStream) throws IOException, BadImageException, MetadataException {
    FileType fileType = FileTypeDetector.detectFileType(filteredFileStream);

    if(fileType == FileType.Gif) {
      Metadata metadata = GifMetadataReader.readMetadata(filteredFileStream);
      GifHeaderDirectory directory = metadata.getFirstDirectoryOfType(GifHeaderDirectory.class);

      width = directory.getInt(GifHeaderDirectory.TAG_IMAGE_WIDTH);
      height = directory.getInt(GifHeaderDirectory.TAG_IMAGE_HEIGHT);
    } else {
      throw new BadImageException("Bad GIF image: "+filename);
    }
  }

  private void getPngInfo(BufferedInputStream filteredFileStream) throws IOException, BadImageException, MetadataException, PngProcessingException {
    FileType fileType = FileTypeDetector.detectFileType(filteredFileStream);

    if(fileType == FileType.Png) {
      Metadata metadata = PngMetadataReader.readMetadata(filteredFileStream);
      PngDirectory directory = metadata.getFirstDirectoryOfType(PngDirectory.class);

      width = directory.getInt(PngDirectory.TAG_IMAGE_WIDTH);
      height = directory.getInt(PngDirectory.TAG_IMAGE_HEIGHT);
    } else {
      throw new BadImageException("Bad PNG image: "+filename);
    }
  }

  private void getJpgInfo(BufferedInputStream filteredFileStream) throws IOException, BadImageException, JpegProcessingException, MetadataException {
    FileType fileType = FileTypeDetector.detectFileType(filteredFileStream);

    if(fileType == FileType.Jpeg) {
      Metadata metadata = JpegMetadataReader.readMetadata(filteredFileStream);
      JpegDirectory directory = metadata.getFirstDirectoryOfType(JpegDirectory.class);

      width = directory.getInt(JpegDirectory.TAG_IMAGE_WIDTH);
      height = directory.getInt(JpegDirectory.TAG_IMAGE_HEIGHT);
    } else {
      throw new BadImageException("Bad JPG image: "+filename);
    }
  }

  private void getWebpInfo(BufferedInputStream filteredFileStream) throws IOException, BadImageException, RiffProcessingException, MetadataException {
    FileType fileType = FileTypeDetector.detectFileType(filteredFileStream);

    if(fileType == FileType.WebP) {
      Metadata metadata = WebpMetadataReader.readMetadata(filteredFileStream);
      WebpDirectory directory = metadata.getFirstDirectoryOfType(WebpDirectory.class);

      width = directory.getInt(WebpDirectory.TAG_IMAGE_WIDTH);
      height = directory.getInt(WebpDirectory.TAG_IMAGE_HEIGHT);
    } else {
      throw new BadImageException("Bad WebP image: "+filename);
    }
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
