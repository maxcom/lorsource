/*
 * Copyright 1998-2010 Linux.org.ru
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
import com.google.common.collect.Lists;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jasypt.util.password.PasswordEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.URLUtil;

import javax.mail.internet.InternetAddress;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Repository
public class UserDao {
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private IgnoreListDao ignoreListDao;

  /**
   * изменение score пользователю
   */
  private static final String queryChangeScore = "UPDATE users SET score=score+? WHERE id=?";
  private static final String queryUserById = "SELECT id,nick,score,max_score,candel,canmod,corrector,passwd,blocked,activated,photo,email,name,unread_events,style FROM users where id=?";
  private static final String queryUserByNick = "SELECT id,nick,candel,canmod,corrector,passwd,blocked,score,max_score,activated,photo,email,name,unread_events,style FROM users where nick=?";
  private static final String updateUserStyle = "UPDATE users SET style=? WHERE id=?";

  private static final String queryNewUsers = "SELECT id FROM users where " +
                                                "regdate IS NOT null " +
                                                "AND regdate > CURRENT_TIMESTAMP - interval '3 days' " +
                                              "ORDER BY regdate";

  private static final String queryUserInfoClass = "SELECT url, town, lastlogin, regdate FROM users WHERE id=?";
  private static final String queryBanInfoClass = "SELECT * FROM ban_info WHERE userid=?";

  private static final String queryCommentStat = "SELECT count(*) as c FROM comments WHERE userid=? AND not deleted";
  private static final String queryTopicDates = "SELECT min(postdate) as first,max(postdate) as last FROM topics WHERE topics.userid=?";
  private static final String queryCommentDates = "SELECT min(postdate) as first,max(postdate) as last FROM comments WHERE comments.userid=?";
  private static final String queryCommentsBySectionStat =
            "SELECT sections.name as pname, count(*) as c " +
                    "FROM topics, groups, sections " +
                    "WHERE topics.userid=? " +
                    "AND groups.id=topics.groupid " +
                    "AND sections.id=groups.section " +
                    "AND not deleted " +
                    "GROUP BY sections.name";

  private static final String updateResetUnreadReplies = "UPDATE users SET unread_events=0 where id=?";

  @Autowired
  public void setJdbcTemplate(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  public User getUser(String nick) throws UserNotFoundException {
    if (nick == null) {
      throw new NullPointerException();
    }

    if (!StringUtil.checkLoginName(nick)) {
      throw new UserNotFoundException("<invalid name>");
    }

    Cache cache = CacheManager.create().getCache("Users");

    List<User> list = jdbcTemplate.query(
            queryUserByNick,
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

  public User getUserCached(int id) throws UserNotFoundException {
    return getUser(id, true);
  }

  /**
   * Загружает пользователя из БД не используя кеш (всегда обновляет кеш).
   * Метод используется там, где нужно проверить права пользователя, совершить какой-то
   * update или получить самый свежий варинт из БД. В остальных случаях нужно
   * использовать метод getUserCached()
   *
   * @param id идентификатор пользователя
   * @return объект пользователя
   * @throws UserNotFoundException если пользователь с таким id не найден
   */
  public User getUser(int id) throws UserNotFoundException {
    return getUser(id, false);
  }

  private User getUser(int id, boolean useCache) throws UserNotFoundException {
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
          queryUserById, new RowMapper<User>() {
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

  /**
   * Получить поле userinfo пользователя
   * TODO надо переименовать?
   * @param user пользователь
   * @return поле userinfo
   */
  public String getUserInfo(User user) {
    return jdbcTemplate.queryForObject("SELECT userinfo FROM users where id=?",
        new Object[] {user.getId()}, String.class);
  }

  /**
   * Получить информацию о пользователе
   * @param user пользователь
   * @return информация
   */
  public UserInfo getUserInfoClass(User user) {
    return jdbcTemplate.queryForObject(queryUserInfoClass, new RowMapper<UserInfo>() {
      @Override
      public UserInfo mapRow(ResultSet resultSet, int i) throws SQLException {
        return new UserInfo(resultSet);
      }
    }, user.getId());
  }

  /**
   * Получить информацию о бане
   * @param user пользователь
   * @return информация о бане :-)
   */
  public BanInfo getBanInfoClass(User user) {
    List<BanInfo> infoList = jdbcTemplate.query(queryBanInfoClass, new RowMapper<BanInfo>() {
      @Override
      public BanInfo mapRow(ResultSet resultSet, int i) throws SQLException {
        Timestamp date = resultSet.getTimestamp("bandate");
        String reason = resultSet.getString("reason");
        User moderator;
        try {
          moderator = getUser(resultSet.getInt("ban_by"));
        } catch (UserNotFoundException exception) {
          throw new SQLException(exception.getMessage());
        }
        return new BanInfo(date, reason, moderator);
      }
    }, user.getId());

    if (infoList.isEmpty()) {
      return null;
    } else {
      return infoList.get(0);
    }
  }

  /**
   * Получить статситику пользователя
   * @param user пользователь
   * @return статистика
   */
  public UserStatistics getUserStatisticsClass(User user) {
    int ignoreCount = ignoreListDao.getIgnoreStat(user);

    int commentCount;

    try {
      commentCount = jdbcTemplate.queryForInt(queryCommentStat, user.getId());
    } catch (EmptyResultDataAccessException exception) {
      commentCount = 0;
    }

    List<Timestamp> commentStat;

    try {
      commentStat = jdbcTemplate.queryForObject(queryCommentDates, new RowMapper<List<Timestamp>>() {
        @Override
        public List<Timestamp> mapRow(ResultSet resultSet, int i) throws SQLException {
          return Lists.newArrayList(resultSet.getTimestamp("first"), resultSet.getTimestamp("last"));
        }
      }, user.getId());
    } catch (EmptyResultDataAccessException exception) {
      commentStat = null;
    }

    List<Timestamp> topicStat;

    try {
      topicStat = jdbcTemplate.queryForObject(queryTopicDates, new RowMapper<List<Timestamp>>() {
        @Override
        public List<Timestamp> mapRow(ResultSet resultSet, int i) throws SQLException {
          return Lists.newArrayList(resultSet.getTimestamp("first"), resultSet.getTimestamp("last"));
        }
      }, user.getId());
    } catch (EmptyResultDataAccessException exception) {
      topicStat = null;
    }

    final ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
    jdbcTemplate.query(queryCommentsBySectionStat, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet resultSet) throws SQLException {
        builder.put(resultSet.getString("pname"), resultSet.getInt("c"));
      }
    }, user.getId());
    
    return new UserStatistics(ignoreCount, commentCount,
        commentStat.get(0), commentStat.get(1),
        topicStat.get(0), topicStat.get(1),
        builder.build());
  }

  /**
   * Получить список новых пользователей зарегистрирововавшихся за последние 3(три) дня
   * @return список новых пользователей
   */
  public List<User> getNewUsers() {
    return getUsersCached(jdbcTemplate.queryForList(queryNewUsers, Integer.class));
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void removeUserInfo(User user) {
    String userInfo = getUserInfo(user);
    if(userInfo == null || userInfo.trim().isEmpty()) {
      return;
    }

    setUserInfo(user.getId(), null);
    changeScore(user.getId(), -10);
  }

  /**
   * Отчистка userpicture пользователя, с обрезанием шкворца если удляет моедратор
   * @param user пользовтель у которого чистят
   * @param cleaner пользователь который чистит
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void removePhoto(User user, User cleaner) {
    setPhoto(user, null);
    if(cleaner.isModerator() && cleaner.getId() != user.getId()){
      changeScore(user.getId(), -10);
    }
  }

  /**
   * Обновление userpic-а пользовтаеля
   * @param user пользователь
   * @param photo userpick
   */
  public void setPhoto(User user, String photo){
    jdbcTemplate.update("UPDATE users SET photo=? WHERE id=?", photo, user.getId());
  }

  /**
   * Обновление дополнительной информации пользователя
   * @param userid пользователь
   * @param text текст дополнительной информации
   */
  private void setUserInfo(int userid, String text){
    jdbcTemplate.update("UPDATE users SET userinfo=? where id=?", text, userid);
  }

  /**
   * Сброс уведомлений
   * @param user пользователь которому сбрасываем
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void resetUnreadReplies(User user) {
    jdbcTemplate.update(updateResetUnreadReplies, user.getId());
    jdbcTemplate.update("UPDATE user_events SET unread=false WHERE userid=?", user.getId());
  }

  /**
   * Изменение шкворца пользовтаеля, принимает отрицательные и положительные значения
   * не накладывает никаких ограничений на параметры используется в купэ с другими
   * методами и не является транзакцией
   * @param id id пользователя
   * @param delta дельта на которую меняется шкворец
   */
  public void changeScore(int id, int delta) {
    if (jdbcTemplate.update(queryChangeScore, delta, id)==0) {
      throw new IllegalArgumentException(new UserNotFoundException(id));
    }

    updateCache(id);
  }

  /**
   * Смена признака корректора для пользователя
   * @param user пользователь у которого меняется признак корректора
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void toggleCorrector(User user){
    if(user.canCorrect()){
      jdbcTemplate.update("UPDATE users SET corrector='f' WHERE id=?", user.getId());
    }else{
      jdbcTemplate.update("UPDATE users SET corrector='t' WHERE id=?", user.getId());
    }
  }
  
  /**
   * Смена стиля\темы пользователя
   * @param user пользователь у которого меняется стиль\тема
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void setStyle(User user, String theme){
    jdbcTemplate.update(updateUserStyle, theme, user.getId());
  }
  

  /**
   * Сброс пороля на случайный
   * @param user пользователь которому сбрасывается пароль
   * @return новый пароь в открытом виде
   */
  public String resetPassword(User user){
    String password = StringUtil.generatePassword();
    return setPassword(user, password);
  }

  private String setPassword(User user, String password) {
    PasswordEncryptor encryptor = new BasicPasswordEncryptor();
    String encryptedPassword = encryptor.encryptPassword(password);

    jdbcTemplate.update("UPDATE users SET passwd=?, lostpwd = 'epoch' WHERE id=?",
        encryptedPassword, user.getId());

    return password;
  }

  public void updateResetDate(User user, Timestamp now) {
    jdbcTemplate.update("UPDATE users SET lostpwd=? WHERE id=?",  now, user.getId());
  }

  public Timestamp getResetDate(User user) {
    return jdbcTemplate.queryForObject("SELECT lostpwd FROM users WHERE id=?", Timestamp.class, user.getId());
  }

  /**
   * Блокирование пользователя без транзакации(используется в CommentDao для массового удаления с блокировкой)
   * @param user пользователь которого блокируем
   * @param moderator модератор который блокирует
   * @param reason причина блокировки
   */
  public void blockWithoutTransaction(User user, User moderator, String reason) {
    jdbcTemplate.update("UPDATE users SET blocked='t' WHERE id=?", user.getId());
    jdbcTemplate.update("INSERT INTO ban_info (userid, reason, ban_by) VALUES (?, ?, ?)",
        user.getId(), reason, moderator.getId());
    updateCache(user.getId());
  }

  private void updateCache(int id) {
    try {
      getUser(id);
    } catch (UserNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Блокировка пользователя и сброс пароля одной транзикацией
   * @param user блокируемый пользователь
   * @param moderator модератор который блокирует пользователя
   * @param reason причина блокировки
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void blockWithResetPassword(User user, User moderator, String reason) {

    jdbcTemplate.update("UPDATE users SET blocked='t' WHERE id=?", user.getId());
    jdbcTemplate.update("INSERT INTO ban_info (userid, reason, ban_by) VALUES (?, ?, ?)",
        user.getId(), reason, moderator.getId());
    PasswordEncryptor encryptor = new BasicPasswordEncryptor();
    String password = encryptor.encryptPassword(StringUtil.generatePassword());
    jdbcTemplate.update("UPDATE users SET passwd=?, lostpwd = 'epoch' WHERE id=?",
        password, user.getId());
    updateCache(user.getId());
  }


  /**
   * Разблокировка пользователя
   * @param user разблокируемый пользователь
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void unblock(User user){
    jdbcTemplate.update("UPDATE users SET blocked='f' WHERE id=?", user.getId());
    jdbcTemplate.update("DELETE FROM ban_info WHERE userid=?", user.getId());
  }

  public User getAnonymous() {
    try {
      return getUserCached(2);
    } catch (UserNotFoundException e) {
      throw new RuntimeException("Anonymous not found!?", e);
    }
  }

  public List<User> getModerators() {
    return getUsersCached(jdbcTemplate.queryForList(
            "SELECT id FROM users WHERE canmod ORDER BY id",
            Integer.class
    ));
  }

  public List<User> getCorrectors() {
    return getUsersCached(jdbcTemplate.queryForList(
            "SELECT id FROM users WHERE corrector ORDER BY id",
            Integer.class
    ));
  }

  public List<User> getUsersCached(List<Integer> ids) {
    List<User> users = new ArrayList<User>(ids.size());

    for (int id : ids) {
      try {
        users.add(getUserCached(id));
      } catch (UserNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    return users;
  }

  public User getByEmail(String email) {
    try {
      int id = jdbcTemplate.queryForInt(
              "SELECT id FROM users WHERE email=? AND not blocked",
              email
      );

      return getUser(id);
    } catch (EmptyResultDataAccessException ex) {
      return null;
    } catch (UserNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean canResetPassword(User user) {
    return !jdbcTemplate.queryForObject(
            "SELECT lostpwd>CURRENT_TIMESTAMP-'1 week'::interval as datecheck FROM users WHERE id=?",
            Boolean.class,
            user.getId()
    );
  }

  public void activateUser(User user) {
    jdbcTemplate.update("UPDATE users SET activated='t' WHERE id=?", user.getId());
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void updateUser(User user, String name, String url, String new_email, String town, String password, String info) {
    jdbcTemplate.update(
            "UPDATE users SET  name=?, url=?, new_email=?, town=? WHERE id=?",
            name,
            url,
            new_email,
            town,
            user.getId()
    );

    if (password != null) {
      setPassword(user, password);
    }

    setUserInfo(user.getId(), info);
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public int createUser(String name, String nick, String password, String url, InternetAddress mail, String town, String info) {
    PasswordEncryptor encryptor = new BasicPasswordEncryptor();

    int userid = jdbcTemplate.queryForInt("select nextval('s_uid') as userid");

    jdbcTemplate.update(
            "INSERT INTO users " +
              "(id, name, nick, passwd, url, email, town, score, max_score,regdate) " +
              "VALUES (?,?,?,?,?,?,?,45,45,current_timestamp)",
            userid,
            name,
            nick,
            encryptor.encryptPassword(password),
            url==null?null: URLUtil.fixURL(url),
            mail.getAddress(),
            town
    );

    if (info != null) {
      setUserInfo(userid, info);
    }

    return userid;
  }

  public boolean isUserExists(String nick) {
    int c = jdbcTemplate.queryForInt("SELECT count(*) as c FROM users WHERE nick=?", nick);

    return c>0;
  }

  public String getNewEmail(User user) {
    return jdbcTemplate.queryForObject("SELECT new_email FROM users WHERE id=?", String.class, user.getId());
  }

  public void acceptNewEmail(User user) {
    jdbcTemplate.update("UPDATE users SET email=new_email WHERE id=?", user.getId());
  }

  /**
   * Update lastlogin time in database
   * @param user logged user
   * @throws SQLException on database failure
   */
  public void updateLastlogin(User user) {
    jdbcTemplate.update("UPDATE users SET lastlogin=CURRENT_TIMESTAMP WHERE id=?", user.getId());
  }
}
