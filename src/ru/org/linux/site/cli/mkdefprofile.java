package ru.org.linux.site.cli;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

public final class mkdefprofile {
  private mkdefprofile() {
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
    defaults.put("showignored", Boolean.FALSE);
    defaults.put("showsticky", Boolean.TRUE);

    defaults.put("DebugMode", Boolean.FALSE);

// main page settings
    defaults.put("main.3columns", Boolean.FALSE);

    Vector boxlist = new Vector();
    boxlist.addElement("poll");
    boxlist.addElement("top10");
    boxlist.addElement("gallery");
//		boxlist.addElement("justnews");
//		boxlist.addElement("projects");
    boxlist.addElement("tagcloud");
    boxlist.addElement("archive");
    boxlist.addElement("profile");
    boxlist.addElement("ibm");
//		boxlist.addElement("login");
    defaults.put("boxlist", boxlist);

    Vector boxes = new Vector();
    boxes.addElement("ibm");
    boxes.addElement("poll");
    boxes.addElement("top10");
    boxes.addElement("gallery");
//		boxes.addElement("justnews");
//		boxes.addElement("projects");
    boxes.addElement("tagcloud");
    boxes.addElement("archive");
    boxes.addElement("profile");
    defaults.put("main2", boxes);

    boxes = new Vector();
    boxes.addElement("ibm");
    boxes.addElement("poll");
//		boxes.addElement("projects");
    boxes.addElement("archive");
    boxes.addElement("profile");
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
