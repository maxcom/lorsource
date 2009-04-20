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

package ru.org.linux.site.cli;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

public final class mkdefprofile {
  private static final String[] boxlist = {"poll", "top10", "gallery", "tagcloud", "archive", "ibm"};
  private static final Set<String> boxSet = new HashSet<String>(Arrays.asList(boxlist));

  private mkdefprofile() {
  }

  public static String[] getBoxlist() {
    return boxlist;
  }

  public static boolean isBox(String name) {
    return boxSet.contains(name);
  }

  public static Hashtable getDefaultProfile() {
    Hashtable defaults = new Hashtable();

    defaults.put("newfirst", Boolean.FALSE);
    defaults.put("hover", Boolean.TRUE);
    defaults.put("style", "black");
    defaults.put("format.mode", "quot");
    defaults.put("topics", 30);
    defaults.put("messages", 50);
    defaults.put("tags", 50);
    defaults.put("photos", Boolean.FALSE);
    defaults.put("sortwarning", Boolean.TRUE);
    defaults.put("system.timestamp", new Date().getTime());
    defaults.put("showinfo", Boolean.TRUE);
    defaults.put("showanonymous", Boolean.TRUE);
    defaults.put("showsticky", Boolean.TRUE);

    defaults.put("DebugMode", Boolean.FALSE);

// main page settings
    defaults.put("main.3columns", Boolean.FALSE);

    Vector boxes = new Vector();
    boxes.addElement("ibm");
    boxes.addElement("poll");
    boxes.addElement("top10");
    boxes.addElement("gallery");
//		boxes.addElement("justnews");
//		boxes.addElement("projects");
    boxes.addElement("tagcloud");
    boxes.addElement("archive");
//    boxes.addElement("profile");
    defaults.put("main2", boxes);

    boxes = new Vector();
    boxes.addElement("ibm");
    boxes.addElement("poll");
//		boxes.addElement("projects");
    boxes.addElement("archive");
//    boxes.addElement("profile");
    boxes.addElement("tagcloud");
    defaults.put("main3-1", boxes);

    boxes = new Vector();
//		boxes.addElement("login");
    boxes.addElement("top10");
    boxes.addElement("gallery");
//		boxes.addElement("justnews");
    defaults.put("main3-2", boxes);

    return defaults;
  }

  public static void main(String[] args) throws IOException {
    Hashtable defaults = getDefaultProfile();
    FileOutputStream f = new FileOutputStream("anonymous");
    ObjectOutputStream o = new ObjectOutputStream(f);

    o.writeObject(defaults);

    o.close();
    f.close();

    defaults = new Hashtable();
    f = new FileOutputStream("_debug");
    defaults.put("DebugMode", Boolean.TRUE);

    o = new ObjectOutputStream(f);
    o.writeObject(defaults);
    o.close();
    f.close();


    defaults = new Hashtable();
    f = new FileOutputStream("_white2");
    defaults.put("style", "white2");

    o = new ObjectOutputStream(f);
    o.writeObject(defaults);
    o.close();
    f.close();

    defaults = new Hashtable();
    f = new FileOutputStream("_white");
    defaults.put("style", "white");

    o = new ObjectOutputStream(f);
    o.writeObject(defaults);
    o.close();
    f.close();
  }

}
