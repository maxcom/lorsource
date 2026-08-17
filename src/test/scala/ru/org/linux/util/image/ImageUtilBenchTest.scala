/*
 * Copyright 1998-2026 Linux.org.ru
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
package ru.org.linux.util.image

import munit.FunSuite

import java.io.File

class ImageUtilBenchTest extends FunSuite:
  private def imageCheckTestPass1(): ImageParam =
    ImageUtil.imageInfo(new File("src/test/resources/images/i_want_to_be_a_hero__apng_animated__by_tamalesyatole-d5ht8eu.png"))

  private def imageCheckTestPass2(): ImageParam =
    ImageUtil.imageInfo(new File("src/main/webapp/img/pcard.jpg"))

  override def beforeAll(): Unit =
    val param1 = imageCheckTestPass1()
    if param1.getWidth != 1275 then
      println("masaka!")
    val param2 = imageCheckTestPass2()
    if param2.getWidth != 1241 then
      println("masaka!")

  test("imageInfoTest1"):
    for _ <- 0 until 10000 do
      val param = imageCheckTestPass1()
      assertEquals(param.getHeight, 720)
      assertEquals(param.getWidth, 1275)

  test("imageInfoTest2"):
    for _ <- 0 until 10000 do
      val param = imageCheckTestPass2()
      assertEquals(param.getHeight, 870)
      assertEquals(param.getWidth, 1241)