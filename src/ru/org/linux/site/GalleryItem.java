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

package ru.org.linux.site;

import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;

/**
 * User: rsvato
 * Date: Apr 29, 2009
 * Time: 6:44:26 PM
 */
public class GalleryItem implements Serializable {
  private static final Log log = LogFactory.getLog(GalleryItem.class);

  Integer msgid;
  String nick;
  String icon;
  transient ImageInfo info;
  transient ImageInfo imginfo;
  String title;
  Integer stat;
  String url;
  String htmlPath;
  private static final long serialVersionUID = -4059370603821918387L;

  public Integer getMsgid() {
    return msgid;
  }

  public void setMsgid(Integer msgid) {
    this.msgid = msgid;
  }

  public String getNick() {
    return nick;
  }

  public void setNick(String nick) {
    this.nick = nick;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public ImageInfo getInfo() {
    if (info == null) {
      try {
        info = new ImageInfo(htmlPath + icon);
      } catch (IOException e) {
        log.error(e);
      } catch (BadImageException e) {
        log.error(e);
      }
    }
    return info;
  }

  public void setInfo(ImageInfo info) {
    this.info = info;
  }

  public ImageInfo getImginfo() {
    if (imginfo == null){
      try {
        imginfo = new ImageInfo(htmlPath + url);
      } catch (BadImageException e) {
        log.error(e);
      } catch (IOException e) {
        log.error(e);
      }
    }
    return imginfo;
  }

  public void setImginfo(ImageInfo imginfo) {
    this.imginfo = imginfo;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Integer getStat() {
    return stat;
  }

  public void setStat(Integer stat) {
    this.stat = stat;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void setHtmlPath(String htmlPath) {
    this.htmlPath = htmlPath;
  }
}
