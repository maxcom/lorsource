package ru.org.linux.site;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

public class ProfileTest extends TestCase {
  public void testDefaultProfile() {
    new Profile(null);
  }

  public void testDefaultProfileSave() throws IOException, ClassNotFoundException {
    Profile profile = new Profile(null);
    ByteArrayOutputStream os = new ByteArrayOutputStream();

    profile.write(os);

    new Profile(new ByteArrayInputStream(os.toByteArray()) , "test");
  }
}
