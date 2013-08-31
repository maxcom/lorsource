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

import java.io.File;

/**
 */
public class ImageUtilBenchTest {

  private ImageParam imageCheckTestPass1() throws Exception {
    return ImageUtil.imageInfo(new File("src/test/resources/images/i_want_to_be_a_hero__apng_animated__by_tamalesyatole-d5ht8eu.png"));
  }

  private ImageParam imageCheckTestPass2() throws Exception {
    return ImageUtil.imageInfo(new File("src/main/webapp/img/pcard.jpg"));
  }

  private ImageParam imageCheckTestPass3() throws Exception {
    return ImageUtil.imageInfo(new File("src/main/webapp/img/nonexistent"));
  }



  @Before
  public void coldStart() throws Exception {
    ImageParam param1 = imageCheckTestPass1();
    if(param1.getWidth() != 1275) {
      System.out.println("masaka!");
    }
    ImageParam param2 = imageCheckTestPass2();
    if(param2.getWidth() != 1241) {
      System.out.println("masaka!");
    }
  }

  @Test
  public void imageInfoTest1() throws Exception {
    for(int i=0; i< 10000; i++) {
      ImageParam param = imageCheckTestPass1();
      assertEquals(param.getHeight(), 720);
      assertEquals(param.getWidth(), 1275);
    }
  }

  @Test
  public void imageInfoTest2() throws Exception {
    for(int i=0; i< 10000; i++) {
      ImageParam param = imageCheckTestPass2();
      assertEquals(param.getHeight(), 870);
      assertEquals(param.getWidth(), 1241);
    }
  }



}

/**
 * #1
 Running ru.org.linux.util.image.ImageInfoBenchTest
 Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.228 sec
 Running ru.org.linux.util.image.ImageUtilBenchTest
 Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.637 sec
 * #2
 Running ru.org.linux.util.image.ImageInfoBenchTest
 Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.374 sec
 Running ru.org.linux.util.image.ImageUtilBenchTest
 Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.152 sec
 * #3

 */
