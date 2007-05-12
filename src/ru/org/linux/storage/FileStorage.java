package ru.org.linux.storage;

import java.io.*;

public class FileStorage extends Storage {
  private final String root;

  public FileStorage(String RootPath) {
    root = RootPath;
  }

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

  protected void writeMessageImpl(String domain, String msgid, String message) throws IOException, StorageException {
    if (new File(root + domain + '/' + msgid).exists()) {
      throw new StorageExistsException(domain, msgid);
    }

    FileOutputStream out = new FileOutputStream(root + domain + '/' + msgid);
    out.write(message.getBytes("KOI8-R"));
    out.close();
  }

  protected void updateMessageImpl(String domain, String msgid, String message) throws IOException, StorageException {
    if (!(new File(root + domain + '/' + msgid).exists())) {
      throw new StorageNotFoundException(domain, msgid);
    }

    FileOutputStream out = new FileOutputStream(root + domain + '/' + msgid);
    out.write(message.getBytes("KOI8-R"));
    out.close();
  }

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

  protected OutputStream getWriteStreamImpl(String domain, String msgid) throws IOException {
    FileOutputStream in = null;
    in = new FileOutputStream(root + domain + '/' + msgid);

    return in;
  }
}
