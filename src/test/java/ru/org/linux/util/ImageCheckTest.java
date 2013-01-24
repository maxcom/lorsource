/*
 * Copyright 1998-2012 Linux.org.ru
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

import junit.framework.Assert;
import org.junit.Test;

import java.io.File;

/**
 */
public class ImageCheckTest {

  @Test
  public void gifTest() throws Exception {
    ImageCheck typicalGif = new ImageCheck(new File("src/test/resources/images/typical-gif.gif"));
    Assert.assertEquals(1, typicalGif.getNumImages());
    Assert.assertEquals("gif", typicalGif.getFormatName());
    Assert.assertEquals(9, typicalGif.getHeight());
    Assert.assertEquals(9, typicalGif.getWidth());


    ImageCheck failExtGif = new ImageCheck(new File("src/test/resources/images/failext-gif.png"));
    Assert.assertEquals(1, failExtGif.getNumImages());
    Assert.assertEquals("gif", failExtGif.getFormatName());
    Assert.assertEquals(9, failExtGif.getHeight());
    Assert.assertEquals(9, failExtGif.getWidth());
  }

  @Test
  public void pngTest() throws Exception {
    ImageCheck typical = new ImageCheck(new File("src/test/resources/images/typical-png.png"));
    Assert.assertEquals(1, typical.getNumImages());
    Assert.assertEquals("png", typical.getFormatName());
    Assert.assertEquals(32, typical.getHeight());
    Assert.assertEquals(32, typical.getWidth());


    ImageCheck failExt = new ImageCheck(new File("src/test/resources/images/failext-png.jpg"));
    Assert.assertEquals(1, failExt.getNumImages());
    Assert.assertEquals("png", failExt.getFormatName());
    Assert.assertEquals(32, failExt.getHeight());
    Assert.assertEquals(32, failExt.getWidth());
  }

  @Test
  public void jpgTest() throws Exception {
    ImageCheck typical = new ImageCheck(new File("src/test/resources/images/typical-jpg.jpg"));
    Assert.assertEquals(1, typical.getNumImages());
    Assert.assertEquals("JPEG", typical.getFormatName());
    Assert.assertEquals(400, typical.getHeight());
    Assert.assertEquals(240, typical.getWidth());


    ImageCheck failExt = new ImageCheck(new File("src/test/resources/images/failext-jpg.png"));
    Assert.assertEquals(1, failExt.getNumImages());
    Assert.assertEquals("JPEG", failExt.getFormatName());
    Assert.assertEquals(400, failExt.getHeight());
    Assert.assertEquals(240, failExt.getWidth());
  }

  @Test
  public void crapTest() throws Exception {
    ImageCheck crap = new ImageCheck(new File("src/test/resources/images/crap.png"));
  }

}
