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
package ru.org.linux.util.image;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class ImageInfoBenchTest {
  private ImageInfo imageInfoTestPass1() throws Exception {
    return new ImageInfo("src/test/resources/images/i_want_to_be_a_hero__apng_animated__by_tamalesyatole-d5ht8eu.png");
  }
  private ImageInfo imageInfoTestPass2() throws Exception {
    return new ImageInfo("src/main/webapp/img/pcard.jpg");
  }

  @Before
  public void coldStart() throws Exception {
    ImageInfo ii1 = imageInfoTestPass1();
    if(ii1.getWidth() != 1275) {
      System.out.println("masaka!");
    }
    ImageInfo ii2 = imageInfoTestPass2();
    if(ii2.getWidth() != 1241) {
      System.out.println("masaka!");
    }
  }

  @Test
  public void imageInfoTest1() throws Exception {
    for(int i=0; i< 10000; i++) {
      ImageInfo info = imageInfoTestPass1();
      assertEquals(info.getHeight(), 720);
      assertEquals(info.getWidth(), 1275);
    }
  }

  @Test
  public void imageInfoTest2() throws Exception {
    for(int i=0; i< 10000; i++) {
      ImageInfo info = imageInfoTestPass2();
      assertEquals(info.getHeight(), 870);
      assertEquals(info.getWidth(), 1241);
    }
  }

}
