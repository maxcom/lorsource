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

import ru.org.linux.util.ImageInfo;

/**
 * User: rsvato
* Date: Apr 29, 2009
* Time: 6:44:26 PM
*/
public class GalleryItem {
  Integer msgid;
  String nick;
  String icon;
  ImageInfo info;
  ImageInfo imginfo;
  String title;
  Integer stat;
  String url;

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
    return info;
  }

  public void setInfo(ImageInfo info) {
    this.info = info;
  }

  public ImageInfo getImginfo() {
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
}
