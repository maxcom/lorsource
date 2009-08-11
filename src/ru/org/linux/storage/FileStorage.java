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

package ru.org.linux.storage;

import java.io.*;

public class FileStorage extends Storage {
  private final String root;

  public FileStorage(String RootPath) {
    root = RootPath;
  }

  @Override
  protected String readMessageImpl(String domain, String msgid) throws IOException, StorageNotFoundException {
    BufferedReader in = null;

    try {
      in = new BufferedReader(new InputStreamReader(new FileInputStream(root + domain + '/' + msgid), "KOI8-R"));
    } catch (FileNotFoundException e) {
      throw new StorageNotFoundException(domain, msgid, e);
    }

    char[] buf = new char[8192];
    StringBuffer out = new StringBuffer();

    int i = 0;
    while ((i = in.read(buf, 0, buf.length)) > -1) {
      if (i > 0) {
        out.append(buf, 0, i);
      }
    }

    in.close();

    return out.toString();
  }

  @Override
  protected void writeMessageImpl(String domain, String msgid, String message) throws IOException, StorageException {
    if (new File(root + domain + '/' + msgid).exists()) {
      throw new StorageExistsException(domain, msgid);
    }

    FileOutputStream out = new FileOutputStream(root + domain + '/' + msgid);
    out.write(message.getBytes("KOI8-R"));
    out.close();
  }

  @Override
  protected void updateMessageImpl(String domain, String msgid, String message) throws IOException, StorageException {
    if (!(new File(root + domain + '/' + msgid).exists())) {
      throw new StorageNotFoundException(domain, msgid);
    }

    FileOutputStream out = new FileOutputStream(root + domain + '/' + msgid);
    out.write(message.getBytes("KOI8-R"));
    out.close();
  }

  @Override
  protected InputStream getReadStreamImpl(String domain, String msgid) throws StorageNotFoundException {
    FileInputStream in = null;
    // TODO: try buffered input stream and check perfomance
    try {
      in = new FileInputStream(root + domain + '/' + msgid);
    } catch (FileNotFoundException e) {
      throw new StorageNotFoundException(domain, msgid);
    }

    return in;
  }

  @Override
  protected OutputStream getWriteStreamImpl(String domain, String msgid) throws IOException {
    return new FileOutputStream(root + domain + '/' + msgid);
  }
}
