package ru.org.linux.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import gnu.regexp.RE;
import gnu.regexp.REException;

public abstract class Storage {
  public String readMessageNull(String domain, String msgid) throws IOException, StorageException {
    check(domain, msgid);
    return readMessageNullImpl(domain, msgid);
  }

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
  private static RE DomainCheckRE = null;
  private static RE NameCheckRE = null;

  private void check(String domain, String msgid) throws StorageException {
    try {
      if (DomainCheckRE == null) {
        DomainCheckRE = new RE("[a-z]*");
      }

      if (!DomainCheckRE.isMatch(domain)) {
        throw new StorageBadDomainException(domain);
      }

      if (NameCheckRE == null) {
        NameCheckRE = new RE("([_a-z][a-z0-9_-]*)|([0-9]*)", RE.REG_ICASE);
      }

      if (!NameCheckRE.isMatch(msgid)) {
        throw new StorageBadMsgidException(msgid);
      }

    } catch (REException e) {
      throw new StorageInternalErrorException(e.toString());
    }
  }

// Implementaion classes

  protected String readMessageImpl(String domain, String msgid) throws IOException, StorageException {
    throw new StorageNotImplException();
  }

  protected String readMessageNullImpl(String domain, String msgid) throws IOException, StorageException {
    try {
      return readMessageImpl(domain, msgid);
    } catch (StorageNotFoundException e) {
      return null;
    }
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
