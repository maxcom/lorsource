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

class ImageInfoBenchTest extends FunSuite:
  private def imageInfoTestPass1(): ImageInfo =
    new ImageInfo("src/test/resources/images/i_want_to_be_a_hero__apng_animated__by_tamalesyatole-d5ht8eu.png")

  private def imageInfoTestPass2(): ImageInfo =
    new ImageInfo("src/main/webapp/img/pcard.jpg")

  override def beforeAll(): Unit =
    val ii1 = imageInfoTestPass1()
    if ii1.getWidth != 1275 then
      println("masaka!")
    val ii2 = imageInfoTestPass2()
    if ii2.getWidth != 1241 then
      println("masaka!")

  test("imageInfoTest1"):
    for _ <- 0 until 10000 do
      val info = imageInfoTestPass1()
      assertEquals(info.getHeight, 720)
      assertEquals(info.getWidth, 1275)

  test("imageInfoTest2"):
    for _ <- 0 until 10000 do
      val info = imageInfoTestPass2()
      assertEquals(info.getHeight, 870)
      assertEquals(info.getWidth, 1241)