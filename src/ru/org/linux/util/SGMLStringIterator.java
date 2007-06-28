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
