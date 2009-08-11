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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import gnu.regexp.RE;
import gnu.regexp.REException;

public abstract class Storage {
  public String readMessageDefault(String domain, String msgid, String def) throws IOException, StorageException {
    check(domain, msgid);
    return readMessageDefaultImpl(domain, msgid, def);
  }


  public void writeMessage(String domain, String msgid, String message) throws IOException, StorageException {
    check(domain, msgid);
    writeMessageImpl(domain, msgid, message);
  }

  public void updateMessage(String domain, String msgid, String message) throws IOException, StorageException {
    check(domain, msgid);
    updateMessageImpl(domain, msgid, message);
  }

  public InputStream getReadStream(String domain, String msgid) throws StorageException {
    check(domain, msgid);
    return getReadStreamImpl(domain, msgid);
  }

  public OutputStream getWriteStream(String domain, String msgid) throws IOException, StorageException {
    check(domain, msgid);
    return getWriteStreamImpl(domain, msgid);
  }

// private
private static final RE DOMAIN_CHECK_RE;
  private static final RE NAME_CHECK_RE;

  static {
    try {
      DOMAIN_CHECK_RE = new RE("[a-z]*");
      NAME_CHECK_RE = new RE("([_a-z][a-z0-9_-]*)|([0-9]*)", RE.REG_ICASE);
    } catch (REException e) {
      throw new RuntimeException(e);
    }
  }

  private void check(String domain, String msgid) throws StorageException {
    if (!DOMAIN_CHECK_RE.isMatch(domain)) {
      throw new StorageBadDomainException(domain);
    }

    if (!NAME_CHECK_RE.isMatch(msgid)) {
      throw new StorageBadMsgidException(msgid);
    }
  }

// Implementaion methods

  protected String readMessageImpl(String domain, String msgid) throws IOException, StorageException {
    throw new StorageNotImplException();
  }

  protected String readMessageDefaultImpl(String domain, String msgid, String def) throws IOException, StorageException {
    try {
      return readMessageImpl(domain, msgid);
    } catch (StorageNotFoundException e) {
      return def;
    }
  }

  protected void writeMessageImpl(String domain, String msgid, String message) throws IOException, StorageException {
    throw new StorageNotImplException();
  }

  protected void updateMessageImpl(String domain, String msgid, String message) throws IOException, StorageException {
    throw new StorageNotImplException();
  }

  protected InputStream getReadStreamImpl(String domain, String msgid) throws StorageException {
    throw new StorageNotImplException();
  }

  protected OutputStream getWriteStreamImpl(String domain, String msgid) throws IOException, StorageException {
    throw new StorageNotImplException();
  }
}
