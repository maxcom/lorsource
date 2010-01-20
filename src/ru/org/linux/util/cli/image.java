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

package ru.org.linux.util.cli;

import ru.org.linux.util.ImageInfo;

final class image {
  public static void main(String[] args) {
    try {
      for (int i = 0; i < args.length; i++) {
        System.out.print(args[i] + ' ');
        System.out.println(new ImageInfo(args[i]).getCode());
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}