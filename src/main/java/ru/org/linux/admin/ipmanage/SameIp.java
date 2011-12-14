/*
* Copyright 1998-2011 Linux.org.ru
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
package ru.org.linux.admin.ipmanage;

import ru.org.linux.util.StringUtil;

import java.sql.Timestamp;

public class SameIp {

  public static class UserItem {
    private Timestamp lastdate;
    private String nick;
    private boolean sameUa;
    private String userAgent;

    public UserItem() {

    }

    public UserItem(SameIpDto.UserItem userItemDto, int uaId) {
      lastdate = userItemDto.getLastdate();
      nick = userItemDto.getNick();
      sameUa = uaId == userItemDto.getUaId();
      userAgent = userItemDto.getUserAgent();
    }

    public Timestamp getLastdate() {
      return lastdate;
    }

    public void setLastdate(Timestamp lastdate) {
      this.lastdate = lastdate;
    }

    public String getNick() {
      return nick;
    }

    public void setNick(String nick) {
      this.nick = nick;
    }

    public boolean isSameUa() {
      return sameUa;
    }

    public void setSameUa(boolean sameUa) {
      this.sameUa = sameUa;
    }

    public String getUserAgent() {
      return userAgent;
    }

    public void setUserAgent(String userAgent) {
      this.userAgent = userAgent;
    }
  }

  public static class TopicItem {
    private String ptitle;
    private String gtitle;
    private int topicId;
    private String title;
    private Timestamp postdate;
    private int commentId;
    private boolean deleted;

    public TopicItem() {

    }

    public TopicItem(SameIpDto.TopicItem topicItemDto, boolean isComment) {
      ptitle = topicItemDto.getPtitle();
      gtitle = topicItemDto.getGtitle();
      topicId = topicItemDto.getTopicId();
      title = StringUtil.makeTitle(topicItemDto.getTitle());
      postdate = topicItemDto.getPostdate();

      if (isComment) {
        commentId = topicItemDto.getCommentId();
      } else {
        commentId = 0;
      }
      deleted = topicItemDto.isDeleted();
    }

    public String getPtitle() {
      return ptitle;
    }

    public void setPtitle(String ptitle) {
      this.ptitle = ptitle;
    }

    public String getGtitle() {
      return gtitle;
    }

    public void setGtitle(String gtitle) {
      this.gtitle = gtitle;
    }

    public int getTopicId() {
      return topicId;
    }

    public void setTopicId(int topicId) {
      this.topicId = topicId;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public Timestamp getPostdate() {
      return postdate;
    }

    public void setPostdate(Timestamp postdate) {
      this.postdate = postdate;
    }

    public int getCommentId() {
      return commentId;
    }

    public void setCommentId(int commentId) {
      this.commentId = commentId;
    }

    public boolean isDeleted() {
      return deleted;
    }

    public void setDeleted(boolean deleted) {
      this.deleted = deleted;
    }
  }

  public static class BlockInfo {
    private boolean blocked;
    private boolean blockExpired;
    private String reason;
    private Timestamp banDate;
    private boolean tor;
    private Timestamp originalDate;
    private String moderatorNick;

    public BlockInfo() {

    }

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }

    public Timestamp getBanDate() {
      return banDate;
    }

    public void setBanDate(Timestamp banDate) {
      this.banDate = banDate;
    }

    public boolean isTor() {
      return tor;
    }

    public void setTor(boolean tor) {
      this.tor = tor;
    }

    public Timestamp getOriginalDate() {
      return originalDate;
    }

    public void setOriginalDate(Timestamp originalDate) {
      this.originalDate = originalDate;
    }

    public String getModeratorNick() {
      return moderatorNick;
    }

    public void setModeratorNick(String moderatorNick) {
      this.moderatorNick = moderatorNick;
    }

    public boolean isBlocked() {
      return blocked;
    }

    public void setBlocked(boolean blocked) {
      this.blocked = blocked;
    }

    public boolean isBlockExpired() {
      return blockExpired;
    }

    public void setBlockExpired(boolean blockExpired) {
      this.blockExpired = blockExpired;
    }
  }

  public static class IpInfo {
    private String ipAddress;
    private int userAgentId;

    public String getIpAddress() {
      return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
    }

    public int getUserAgentId() {
      return userAgentId;
    }

    public void setUserAgentId(int userAgentId) {
      this.userAgentId = userAgentId;
    }
  }

}
