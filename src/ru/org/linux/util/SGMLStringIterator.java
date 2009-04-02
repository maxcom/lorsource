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

package ru.org.linux.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SGMLStringIterator implements Iterator {
  final String str;
  int index = 0;

  public SGMLStringIterator(String str) {
    this.str = str;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  public boolean hasNext() {
    return index<str.length();
  }

  public Object next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    if (str.charAt(index)=='&') {
      StringBuffer buf = new StringBuffer();

      while (str.charAt(index)!=';') {
        buf.append(str.charAt(index));
        index++;
      }

      buf.append(str.charAt(index++));

      return buf.toString();
    } else {
      return Character.toString(str.charAt(index++));
    }
  }
}
