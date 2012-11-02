/*
 * Copyright 1998-2012 Linux.org.ru
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

package ru.org.linux.test;

import java.sql.ResultSet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ResultSet для разных пользователей
 */
public class Users {
  public static ResultSet getMaxcom() throws Exception {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getInt("id")).thenReturn(1);
    when(resultSet.getString("nick")).thenReturn("maxcom");
    when(resultSet.getString("style")).thenReturn("tango");
    when(resultSet.getBoolean("canmod")).thenReturn(true);
    when(resultSet.getBoolean("candel")).thenReturn(true);
    when(resultSet.getBoolean("corrector")).thenReturn(false);
    when(resultSet.getBoolean("activated")).thenReturn(true);
    when(resultSet.getBoolean("blocked")).thenReturn(false);
    when(resultSet.getInt("score")).thenReturn(600);
    when(resultSet.getInt("max_score")).thenReturn(600);
    when(resultSet.getString("name")).thenReturn("Максим Валянский");
    when(resultSet.getString("passwd")).thenReturn("UEX2F5/8Q5loMT3EQaknMyNbSxtlgain");
    when(resultSet.getString("photo")).thenReturn("1:403073453.png");
    when(resultSet.getString("email")).thenReturn("max@linux.org.ru");
    when(resultSet.getInt("unread_events")).thenReturn(0);
    return resultSet;
  }

  public static ResultSet getHizel() throws Exception {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getInt("id")).thenReturn(34590);
    when(resultSet.getString("nick")).thenReturn("hizel");
    when(resultSet.getString("style")).thenReturn("tango");
    when(resultSet.getBoolean("canmod")).thenReturn(false);
    when(resultSet.getBoolean("candel")).thenReturn(false);
    when(resultSet.getBoolean("corrector")).thenReturn(false);
    when(resultSet.getBoolean("activated")).thenReturn(false);
    when(resultSet.getBoolean("blocked")).thenReturn(false);
    when(resultSet.getInt("score")).thenReturn(45);
    when(resultSet.getInt("max_score")).thenReturn(45);
    when(resultSet.getString("name")).thenReturn("");
    when(resultSet.getString("passwd")).thenReturn("DffBkILVpGCDTC8ykceJzvcj5dJbhF38");
    when(resultSet.getString("photo")).thenReturn("");
    when(resultSet.getString("email")).thenReturn("hz@vyborg.ru");
    when(resultSet.getInt("unread_events")).thenReturn(0);
    return resultSet;
  }


  public static ResultSet getAnonymous() throws Exception {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getInt("id")).thenReturn(2);
    when(resultSet.getString("nick")).thenReturn("anonymous");
    when(resultSet.getString("style")).thenReturn("tango");
    when(resultSet.getBoolean("canmod")).thenReturn(false);
    when(resultSet.getBoolean("candel")).thenReturn(false);
    when(resultSet.getBoolean("corrector")).thenReturn(false);
    when(resultSet.getBoolean("activated")).thenReturn(true);
    when(resultSet.getBoolean("blocked")).thenReturn(false);
    when(resultSet.getInt("score")).thenReturn(-117654);
    when(resultSet.getInt("max_score")).thenReturn(4);
    when(resultSet.getString("name")).thenReturn("Anonymous");
    when(resultSet.getString("passwd")).thenReturn(null);
    when(resultSet.getString("photo")).thenReturn(null);
    when(resultSet.getString("email")).thenReturn(null);
    when(resultSet.getInt("unread_events")).thenReturn(161);
    return resultSet;
  }
  public static ResultSet getModerator() throws Exception {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getInt("id")).thenReturn(5280);
    when(resultSet.getString("nick")).thenReturn("svu");
    when(resultSet.getString("style")).thenReturn("tango");
    when(resultSet.getBoolean("canmod")).thenReturn(true);
    when(resultSet.getBoolean("candel")).thenReturn(false);
    when(resultSet.getBoolean("corrector")).thenReturn(false);
    when(resultSet.getBoolean("activated")).thenReturn(true);
    when(resultSet.getBoolean("blocked")).thenReturn(false);
    when(resultSet.getInt("score")).thenReturn(500);
    when(resultSet.getInt("max_score")).thenReturn(500);
    when(resultSet.getString("name")).thenReturn("Sergey V. Udaltsov");
    when(resultSet.getString("passwd")).thenReturn("0vwkMky44u8kIqSasrH+X8mHao1a3jOC");
    when(resultSet.getString("photo")).thenReturn("5280.png");
    when(resultSet.getString("email")).thenReturn(null);
    when(resultSet.getInt("unread_events")).thenReturn(2);
    return resultSet;
  }
  public static ResultSet getUser5star() throws  Exception {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getInt("id")).thenReturn(13);
    when(resultSet.getString("nick")).thenReturn("user5star");
    when(resultSet.getString("style")).thenReturn("tango");
    when(resultSet.getBoolean("canmod")).thenReturn(false);
    when(resultSet.getBoolean("candel")).thenReturn(false);
    when(resultSet.getBoolean("corrector")).thenReturn(false);
    when(resultSet.getBoolean("activated")).thenReturn(true);
    when(resultSet.getBoolean("blocked")).thenReturn(false);
    when(resultSet.getInt("score")).thenReturn(500);
    when(resultSet.getInt("max_score")).thenReturn(500);
    when(resultSet.getString("name")).thenReturn("5 star");
    when(resultSet.getString("passwd")).thenReturn("S+Q/c5dtkvNxO42uEcQBdP8r32zOfdUq");
    when(resultSet.getString("photo")).thenReturn(null);
    when(resultSet.getString("email")).thenReturn(null);
    when(resultSet.getInt("unread_events")).thenReturn(13);
    return resultSet;
  }
  public static ResultSet getUser1star() throws  Exception {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getInt("id")).thenReturn(14);
    when(resultSet.getString("nick")).thenReturn("user1star");
    when(resultSet.getString("style")).thenReturn("tango");
    when(resultSet.getBoolean("canmod")).thenReturn(false);
    when(resultSet.getBoolean("candel")).thenReturn(false);
    when(resultSet.getBoolean("corrector")).thenReturn(false);
    when(resultSet.getBoolean("activated")).thenReturn(true);
    when(resultSet.getBoolean("blocked")).thenReturn(false);
    when(resultSet.getInt("score")).thenReturn(110);
    when(resultSet.getInt("max_score")).thenReturn(110);
    when(resultSet.getString("name")).thenReturn("1 star");
    when(resultSet.getString("passwd")).thenReturn("S+Q/c5dtkvNxO42uEcQBdP8r32zOfdUq");
    when(resultSet.getString("photo")).thenReturn(null);
    when(resultSet.getString("email")).thenReturn(null);
    when(resultSet.getInt("unread_events")).thenReturn(13);
    return resultSet;
  }
  public static ResultSet getUser45Score() throws Exception {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getInt("id")).thenReturn(15);
    when(resultSet.getString("nick")).thenReturn("user45score");
    when(resultSet.getString("style")).thenReturn("tango");
    when(resultSet.getBoolean("canmod")).thenReturn(false);
    when(resultSet.getBoolean("candel")).thenReturn(false);
    when(resultSet.getBoolean("corrector")).thenReturn(false);
    when(resultSet.getBoolean("activated")).thenReturn(true);
    when(resultSet.getBoolean("blocked")).thenReturn(false);
    when(resultSet.getInt("score")).thenReturn(45);
    when(resultSet.getInt("max_score")).thenReturn(45);
    when(resultSet.getString("name")).thenReturn("45 score");
    when(resultSet.getString("passwd")).thenReturn("S+Q/c5dtkvNxO42uEcQBdP8r32zOfdUq");
    when(resultSet.getString("photo")).thenReturn(null);
    when(resultSet.getString("email")).thenReturn(null);
    when(resultSet.getInt("unread_events")).thenReturn(13);
    return resultSet;
  }
  public static ResultSet getUser45ScoreBlocked() throws Exception {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getInt("id")).thenReturn(16);
    when(resultSet.getString("nick")).thenReturn("userBlocked");
    when(resultSet.getString("style")).thenReturn("tango");
    when(resultSet.getBoolean("canmod")).thenReturn(false);
    when(resultSet.getBoolean("candel")).thenReturn(false);
    when(resultSet.getBoolean("corrector")).thenReturn(false);
    when(resultSet.getBoolean("activated")).thenReturn(true);
    when(resultSet.getBoolean("blocked")).thenReturn(true);
    when(resultSet.getInt("score")).thenReturn(45);
    when(resultSet.getInt("max_score")).thenReturn(45);
    when(resultSet.getString("name")).thenReturn("blocked");
    when(resultSet.getString("passwd")).thenReturn("S+Q/c5dtkvNxO42uEcQBdP8r32zOfdUq");
    when(resultSet.getString("photo")).thenReturn(null);
    when(resultSet.getString("email")).thenReturn(null);
    when(resultSet.getInt("unread_events")).thenReturn(13);
    return resultSet;
  }



}
