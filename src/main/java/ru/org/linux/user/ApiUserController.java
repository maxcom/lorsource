/*
 * Copyright 1998-2015 Linux.org.ru
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

package ru.org.linux.user;

import com.google.common.collect.ImmutableMap;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.site.PublicApi;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Controller
@PublicApi
public class ApiUserController {
  @Autowired
  private UserDao userDao;
  @Autowired
  private UserTagService userTagService;
  @Autowired
  private IgnoreListDao ignoreListDao;

  @RequestMapping(value = "api/user", produces = "application/json; charset=UTF-8", method = RequestMethod.GET)
  @ResponseBody
  public Map<String, Object> getUser(
          @RequestParam(value = "user") String user
  ) throws Exception {
      User authUser = userDao.getUserCached(userDao.findUserId(user));
      UserInfo authUserInfo = userDao.getUserInfoClass(authUser);
      BanInfo banInfo = userDao.getBanInfoClass(authUser);
      Set<Integer> ignoredIds = ignoreListDao.get(authUser);
      ArrayList<String> ignoredUsers = new ArrayList<>();

      for (Integer id : ignoredIds) {
        ignoredUsers.add(userDao.getUser(id).getNick());
      }

      ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();
      builder.put("id", authUser.getId())
              .put("nick", user)
              .put("url", authUserInfo.getUrl());
    Timestamp regDate = authUserInfo.getRegistrationDate();
    if (regDate != null) {
      builder.put("regDate", regDate);
    }
              builder.put("lastVisit", authUserInfo.getLastLogin())
              .put("status", Jsoup.parse(authUser.getStatus()).text());
    if (AuthUtil.isSessionAuthorized()) {
      if (Objects.equals(AuthUtil.getNick(), user)) {
        builder.put("email", authUser.getEmail())
                .put("score", authUser.getScore())
                .put("ignoredUsers", ignoredUsers)
                .put("ignoredTags", userTagService.ignoresGet(authUser));
      }
    }
    builder.put("favTags", userTagService.favoritesGet(authUser));
      if (authUser.isBlocked()) {
        builder.put("banned", authUser.isBlocked())
                .put("banDate", banInfo.getDate())
                .put("bannedBy", banInfo.getModerator())
                .put("banReason", banInfo.getReason());
      }

      return ImmutableMap.of("user", builder.build());
  }
}

