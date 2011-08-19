package ru.org.linux.spring.dao;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.org.linux.site.User;
import ru.org.linux.site.UserNotFoundException;
import ru.org.linux.util.StringUtil;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class UserDao {
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public UserDao(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Deprecated
  public static User getUser(JdbcTemplate jdbcTemplate, String nick) throws UserNotFoundException {
    return getUserInternal(jdbcTemplate, nick);
  }

  private static User getUserInternal(JdbcTemplate jdbcTemplate, String nick) throws UserNotFoundException {
    if (nick == null) {
      throw new NullPointerException();
    }

    if (!StringUtil.checkLoginName(nick)) {
      throw new UserNotFoundException("<invalid name>");
    }

    Cache cache = CacheManager.create().getCache("Users");

    List<User> list = jdbcTemplate.query(
            "SELECT id,nick,candel,canmod,corrector,passwd,blocked,score,max_score,activated,photo,email,name,unread_events FROM users where nick=?",
            new RowMapper<User>() {
              @Override
              public User mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new User(rs);
              }
            },
            nick
    );

    if (list.isEmpty()) {
      throw new UserNotFoundException(nick);
    }

    if (list.size()>1) {
      throw new RuntimeException("list.size()>1 ???");
    }

    User user = list.get(0);

    String cacheId = "User?id="+ user.getId();

    cache.put(new Element(cacheId, user));

    return user;
  }

  public User getUser(String nick) throws UserNotFoundException {
    return getUserInternal(jdbcTemplate, nick);
  }

  public User getUser(int id, boolean useCache) throws UserNotFoundException {
    return getUserInternal(jdbcTemplate, id, useCache);
  }

  public User getUserCached(int id) throws UserNotFoundException {
    return getUserInternal(jdbcTemplate, id, true);
  }

  public User getUser(int id) throws UserNotFoundException {
    return getUserInternal(jdbcTemplate, id, false);
  }

  @Deprecated
  public static User getUser(JdbcTemplate jdbcTemplate, int id, boolean useCache) throws UserNotFoundException {
    return getUserInternal(jdbcTemplate, id, useCache);
  }

  private static User getUserInternal(JdbcTemplate jdbcTemplate, int id, boolean useCache) throws UserNotFoundException {
    Cache cache = CacheManager.create().getCache("Users");

    String cacheId = "User?id="+id;

    User res = null;

    if (useCache) {
      Element element = cache.get(cacheId);

      if (element!=null) {
        res = (User) element.getObjectValue();
      }
    }

    if (res==null) {
      List<User> list = jdbcTemplate.query(
                "SELECT id, nick,score, max_score, candel,canmod,corrector,passwd,blocked,activated,photo,email,name,unread_events FROM users where id=?",              new RowMapper<User>() {
                @Override
                public User mapRow(ResultSet rs, int rowNum) throws SQLException {
                  return new User(rs);
                }
              },
              id
      );

      if (list.isEmpty()) {
        throw new UserNotFoundException(id);
      }

      if (list.size()>1) {
        throw new RuntimeException("list.size()>1 ???");
      }

      res = list.get(0);

      cache.put(new Element(cacheId, res));
    }

    return res;
  }
}
