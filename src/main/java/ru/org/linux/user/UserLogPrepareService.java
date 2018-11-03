/*
 * Copyright 1998-2016 Linux.org.ru
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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.org.linux.util.StringUtil.escapeHtml;

@Service
public class UserLogPrepareService {
  @Autowired
  private UserDao userDao;

  private final static ImmutableMap<String, String> OPTION_DESCRIPTION;

  static {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

    builder.put(UserLogDao.OPTION_BONUS, "Изменение score");
    builder.put(UserLogDao.OPTION_NEW_EMAIL, "Новый email");
    builder.put(UserLogDao.OPTION_NEW_USERPIC, "Новая фотография");
    builder.put(UserLogDao.OPTION_OLD_EMAIL, "Старый email");
    builder.put(UserLogDao.OPTION_OLD_INFO, "Старый текст информации");
    builder.put(UserLogDao.OPTION_OLD_USERPIC, "Старая фотография");
    builder.put(UserLogDao.OPTION_REASON, "Причина");

    OPTION_DESCRIPTION = builder.build();
  }

  @Nonnull
  public List<PreparedUserLogItem> prepare(@Nonnull List<UserLogItem> items) {
    return ImmutableList.copyOf(Lists.transform(
            items, new Function<UserLogItem, PreparedUserLogItem>() {
      @Override
      public PreparedUserLogItem apply(UserLogItem item) {
        Map<String, String> options = new HashMap<>();

        for (Map.Entry<String, String> option : item.getOptions().entrySet()) {
          String key = OPTION_DESCRIPTION.get(option.getKey());
          if (key==null) {
            key = escapeHtml(option.getKey());
          }

          String value;

          switch (option.getKey()) {
            case UserLogDao.OPTION_OLD_USERPIC:
            case UserLogDao.OPTION_NEW_USERPIC:
              value = "<a href=\"/photos/" + escapeHtml(option.getValue()) + "\">" + escapeHtml(option.getValue())+"</a>";
              break;
            case UserLogDao.OPTION_IP:
              value = "<a href=\"/sameip.jsp?ip=" + escapeHtml(option.getValue()) + "\">" + escapeHtml(option.getValue())+"</a>";
              break;
            default:
              value = escapeHtml(option.getValue());
              break;
          }

          options.put(key, value);
        }

        return new PreparedUserLogItem(item, userDao.getUserCached(item.getActionUser()), options);
      }
    }));
  }
}
