/*
 * Copyright 1998-2010 Linux.org.ru
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

import ru.org.linux.exception.storage.StorageNotFoundException;

import java.io.*;

public class FileStorage extends Storage {
  private final String root;

  public FileStorage(String RootPath) {
    root = RootPath;
  }

  @Override
  protected InputStream getReadStreamImpl(String domain, String msgid) throws StorageNotFoundException {
    FileInputStream in  ;
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
