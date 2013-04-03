package ru.org.linux.user;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.List;

@Service
public class UserLogPrepareService {
  @Autowired
  private UserDao userDao;

  @Nonnull
  public List<PreparedUserLogItem> prepare(@Nonnull List<UserLogItem> items) {
    return ImmutableList.copyOf(Lists.transform(
            items, new Function<UserLogItem, PreparedUserLogItem>() {
      @Override
      public PreparedUserLogItem apply(UserLogItem item) {
        return new PreparedUserLogItem(item, userDao.getUserCached(item.getActionUser()));
      }
    }));
  }
}
