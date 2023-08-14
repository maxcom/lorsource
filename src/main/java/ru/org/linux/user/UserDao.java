/*
 * Copyright 1998-2022 Linux.org.ru
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

import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jasypt.util.password.PasswordEncryptor;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.URLUtil;
import scala.Tuple2;
import scala.Tuple3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.InternetAddress;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class UserDao {
  private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

  private final JdbcTemplate jdbcTemplate;

  private final UserLogDao userLogDao;

  /**
   * изменение score пользователю
   */
  private static final String queryChangeScore = "UPDATE users SET score=score+? WHERE id=?";
  private static final String queryUserById = "SELECT id,nick,score,max_score,candel,canmod,corrector,passwd,blocked,activated,photo,email,name,unread_events,style,frozen_until,frozen_by,freezing_reason FROM users where id=?";
  private static final String queryUserIdByNick = "SELECT id FROM users where nick=?";
  private static final String updateUserStyle = "UPDATE users SET style=? WHERE id=?";

  private static final String queryBanInfoClass = "SELECT * FROM ban_info WHERE userid=?";

  private static final String queryCommentStat = "SELECT count(*) as c FROM comments WHERE userid=? AND not deleted";
  private static final String queryCommentDates = "SELECT min(postdate) as first,max(postdate) as last FROM comments WHERE comments.userid=?";

  public UserDao(UserLogDao userLogDao, DataSource dataSource) {
    this.userLogDao = userLogDao;
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  public int findUserId(String nick) throws UserNotFoundException {
    if (nick == null) {
      throw new NullPointerException();
    }

    if (!StringUtil.checkLoginName(nick)) {
      logger.warn("Invalid user name '{}'", nick);
      throw new UserNotFoundException("<invalid name>");
    }

    List<Integer> list = jdbcTemplate.queryForList(
            queryUserIdByNick,
            Integer.class,
            nick
    );

    if (list.isEmpty()) {
      throw new UserNotFoundException(nick);
    }

    if (list.size()>1) {
      throw new RuntimeException("list.size()>1 ???");
    }

    return list.get(0);
  }

  @Cacheable("Users")
  public User getUserCached(int id) throws UserNotFoundException {
    return getUserInternal(id);
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
  @CachePut("Users")
  public User getUser(int id) throws UserNotFoundException {
    return getUserInternal(id);
  }

  private User getUserInternal(int id) throws UserNotFoundException {
    List<User> list = jdbcTemplate.query(queryUserById, (rs, rowNum) -> new User(rs), id);

    if (list.isEmpty()) {
      throw new UserNotFoundException(id);
    }

    if (list.size() > 1) {
      throw new RuntimeException("list.size()>1 ???");
    }

    return list.get(0);
  }

  /**
   * Получить поле userinfo пользователя
   * TODO надо переименовать?
   * @param user пользователь
   * @return поле userinfo
   */
  public String getUserInfo(User user) {
    return jdbcTemplate.queryForObject("SELECT userinfo FROM users where id=?", String.class, user.getId());
  }

  /**
   * Получить информацию о пользователе
   * @param user пользователь
   * @return информация
   */
  public UserInfo getUserInfoClass(User user) {
    return jdbcTemplate.queryForObject("SELECT url, town, lastlogin, regdate FROM users WHERE id=?", (resultSet, i) -> new UserInfo(resultSet), user.getId());
  }

  /**
   * Получить информацию о бане
   * @param user пользователь
   * @return информация о бане :-)
   */
  public BanInfo getBanInfoClass(User user) {
    List<BanInfo> infoList = jdbcTemplate.query(queryBanInfoClass, (resultSet, i) -> {
      Timestamp date = resultSet.getTimestamp("bandate");
      String reason = resultSet.getString("reason");
      User moderator = getUser(resultSet.getInt("ban_by"));
      return new BanInfo(date, reason, moderator);
    }, user.getId());

    if (infoList.isEmpty()) {
      return null;
    } else {
      return infoList.get(0);
    }
  }

  public int getExactCommentCount(User user) {
    try {
      return jdbcTemplate.queryForObject(queryCommentStat, Integer.class, user.getId());
    } catch (EmptyResultDataAccessException exception) {
      return 0;
    }
  }

  public Tuple2<Timestamp, Timestamp> getFirstAndLastCommentDate(User user) {
    return jdbcTemplate.queryForObject(queryCommentDates, (resultSet, i) -> new Tuple2<>(resultSet.getTimestamp("first"), resultSet.getTimestamp("last")), user.getId());
  }

  /**
   * Получить список новых пользователей зарегистрирововавшихся за последние 3(три) дня
   * @return список новых пользователей
   */
  public List<Integer> getNewUserIds() {
    return jdbcTemplate.queryForList("SELECT id FROM users where " +
                                                  "regdate IS NOT null " +
                                                  "AND regdate > CURRENT_TIMESTAMP - interval '3 days' " +
                                                "ORDER BY regdate", Integer.class);
  }

  public List<Tuple3<Integer, Timestamp, Timestamp>> getNewUsersByIP(@Nullable String ip) {
    RowMapper<Tuple3<Integer, Timestamp, Timestamp>> mapper = (rs, rowNum) -> Tuple3.apply(
            rs.getInt("id"),
            rs.getTimestamp("regdate"),
            rs.getTimestamp("lastlogin"));


    if (ip!=null) {
      return jdbcTemplate.query("SELECT users.id, lastlogin, regdate from users join user_log on users.id = user_log.userid WHERE " +
                      "regdate IS NOT null " +
                      "AND regdate > CURRENT_TIMESTAMP - interval '3 days' " +
                      "and action='register' and (info->'ip')::inet <<= ?::inet " +
                      "ORDER BY regdate desc",
              mapper, ip);
    } else {
      return jdbcTemplate.query("SELECT users.id, lastlogin, regdate from users WHERE " +
                      "regdate IS NOT null " +
                      "AND regdate > CURRENT_TIMESTAMP - interval '3 days' " +
                      "ORDER BY regdate desc",
              mapper);
    }
  }

  public List<Tuple2<Integer, DateTime>> getFrozenUserIds() {
    return jdbcTemplate.query("SELECT id, lastlogin FROM users where " +
            "frozen_until > CURRENT_TIMESTAMP and not blocked " +
            "ORDER BY frozen_until",
            (rs, rowNum) -> Tuple2.apply(rs.getInt("id"), new DateTime(rs.getTimestamp("lastlogin").getTime())));
  }

  public List<Tuple2<Integer, DateTime>> getUnFrozenUserIds() {
    return jdbcTemplate.query("SELECT id, lastlogin FROM users where " +
                    "frozen_until < CURRENT_TIMESTAMP and frozen_until > CURRENT_TIMESTAMP - '3 days'::interval and not blocked " +
                    "ORDER BY frozen_until",
            (rs, rowNum) -> Tuple2.apply(rs.getInt("id"), new DateTime(rs.getTimestamp("lastlogin").getTime())));
  }

  public void removeTown(User user) {
    jdbcTemplate.update("UPDATE users SET town=null WHERE id=?", user.getId());
  }

  public void removeUrl(User user) {
    jdbcTemplate.update("UPDATE users SET url=null WHERE id=?", user.getId());
  }

  /**
   * Отчистка userpicture пользователя, с обрезанием шкворца если удаляет модератор
   * @param user пользовтель у которого чистят
   * @param cleaner пользователь который чистит
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  @CacheEvict(value="Users", key="#user.id")
  public boolean resetUserpic(User user, User cleaner) {
    boolean r;

    r = jdbcTemplate.update("UPDATE users SET photo=null WHERE id=? and photo is not null", user.getId()) > 0;

    if (!r) {
      return false;
    }

    // Обрезать score у пользователя если его чистит модератор и пользователь не модератор
    if (cleaner.isModerator() && cleaner.getId() != user.getId() && !user.isModerator()) {
      changeScore(user.getId(), -10);
      userLogDao.logResetUserpic(user, cleaner, -10);
    } else {
      userLogDao.logResetUserpic(user, cleaner, 0);
    }

    return r;
  }

  /**
   * Обновление userpic-а пользовтаеля
   * @param user пользователь
   * @param photo userpick
   */
  @CacheEvict(value="Users", key="#user.id")
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void setPhoto(@Nonnull User user, @Nonnull String photo){
    jdbcTemplate.update("UPDATE users SET photo=? WHERE id=?", photo, user.getId());
    userLogDao.logSetUserpic(user, photo);
  }

  /**
   * Обновление дополнительной информации пользователя
   * @param userid пользователь
   * @param text текст дополнительной информации
   */
  public boolean updateUserInfo(int userid, String text){
    return jdbcTemplate.update("UPDATE users SET userinfo=? where id=? AND userinfo is distinct from ?", text, userid, text) > 0;
  }

  /**
   * Изменение шкворца пользовтаеля, принимает отрицательные и положительные значения
   * не накладывает никаких ограничений на параметры используется в купэ с другими
   * методами и не является транзакцией
   * @param id id пользователя
   * @param delta дельта на которую меняется шкворец
   */
  @CacheEvict(value="Users", key="#id")
  public void changeScore(int id, int delta) {
    if (jdbcTemplate.update(queryChangeScore, delta, id)==0) {
      throw new IllegalArgumentException(new UserNotFoundException(id));
    }
  }

  /**
   * Смена признака корректора для пользователя
   * @param user пользователь у которого меняется признак корректора
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  @CacheEvict(value="Users", key="#user.id")
  public void toggleCorrector(User user, User moderator) {
    if(user.isCorrector()){
      jdbcTemplate.update("UPDATE users SET corrector='f' WHERE id=?", user.getId());
      userLogDao.unsetCorrector(user, moderator);
    } else {
      jdbcTemplate.update("UPDATE users SET corrector='t' WHERE id=?", user.getId());
      userLogDao.setCorrector(user, moderator);
    }
  }
  
  /**
   * Смена стиля\темы пользователя
   * @param user пользователь у которого меняется стиль\тема
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  @CacheEvict(value="Users", key="#user.id")
  public void setStyle(User user, String theme){
    jdbcTemplate.update(updateUserStyle, theme, user.getId());
  }
  

  /**
   * Сброс пороля на случайный
   * @param user пользователь которому сбрасывается пароль
   * @return новый пароь в открытом виде
   */
  @CacheEvict(value="Users", key="#user.id")
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public String resetPassword(User user){
    String password = StringUtil.generatePassword();
    userLogDao.logResetPassword(user, user);
    return setPassword(user, password);
  }

  @CacheEvict(value="Users", key="#user.id")
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void resetPassword(User user, User moderator){
    setPassword(user, StringUtil.generatePassword());
    userLogDao.logResetPassword(user, moderator);
  }

  public String setPassword(User user, String password) {
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
   * Блокировка пользователя
   * @param user блокируемый пользователь
   * @param moderator модератор который блокирует пользователя
   * @param reason причина блокировки
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  @CacheEvict(value="Users", key="#user.id")
  public void block(@Nonnull User user, @Nonnull User moderator, @Nonnull String reason) {
    jdbcTemplate.update("UPDATE users SET blocked='t' WHERE id=?", user.getId());
    jdbcTemplate.update("INSERT INTO ban_info (userid, reason, ban_by) VALUES (?, ?, ?)", user.getId(), reason, moderator.getId());
    userLogDao.logBlockUser(user, moderator, reason);
  }

  /**
   * Заморозка и разморозка пользователя.
   * @param user пользователь для совершения над ним действия
   * @param moderator модератор который это делает
   * @param reason причина заморозки
   * @param until до каких пор ему быть замороженным, если указано прошлое,
   *              то пользователь будет разморожен
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  @CacheEvict(value="Users", key="#user.id")
  public void freezeUser(@Nonnull User user, @Nonnull User moderator, 
    @Nonnull String reason, @Nonnull Timestamp until) {

    jdbcTemplate.update(
      "UPDATE users SET frozen_until=?,frozen_by=?,freezing_reason=? WHERE id=?",
       until, moderator.getId(), reason, user.getId());
    userLogDao.logFreezeUser(user, moderator, reason, until);
  }

  /**
   * Ставим score=50 если он меньше
   *
   * @param user кому ставим score
   * @param moderator модератор
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  @CacheEvict(value="Users", key="#user.id")
  public void score50(@Nonnull User user, @Nonnull User moderator) {
    if (jdbcTemplate.update("UPDATE users SET score=GREATEST(score, 50), max_score=GREATEST(max_score, 50) WHERE id=? AND score<50", user.getId()) > 0) {
      userLogDao.logScore50(user, moderator);
    }
  }

  /**
   * Разблокировка пользователя
   * @param user разблокируемый пользователь
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  @CacheEvict(value="Users", key="#user.id")
  public void unblock(@Nonnull User user, @Nonnull User moderator){
    jdbcTemplate.update("UPDATE users SET blocked='f' WHERE id=?", user.getId());
    jdbcTemplate.update("DELETE FROM ban_info WHERE userid=?", user.getId());
    userLogDao.logUnblockUser(user, moderator);
  }

  public List<Tuple2<Integer, DateTime>> getModerators() {
    return jdbcTemplate.query("SELECT id, lastlogin FROM users where canmod ORDER BY id",
            (rs, rowNum) -> Tuple2.apply(rs.getInt("id"), new DateTime(rs.getTimestamp("lastlogin").getTime())));
  }

  public List<Tuple2<Integer, DateTime>> getCorrectors() {
    return jdbcTemplate.query("SELECT id, lastlogin FROM users where corrector ORDER BY id",
            (rs, rowNum) -> Tuple2.apply(rs.getInt("id"), new DateTime(rs.getTimestamp("lastlogin").getTime())));
  }

  public User getByEmail(String email, boolean searchBlocked) {
    try {
      int id;

      if (searchBlocked) {
        id = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE normalize_email(email)=normalize_email(?) ORDER BY blocked ASC, id DESC LIMIT 1",
                Integer.class,
                email.toLowerCase()
        );
      } else {
        id = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE normalize_email(email)=normalize_email(?) AND NOT blocked ORDER BY id DESC LIMIT 1",
                Integer.class,
                email.toLowerCase()
        );
      }

      return getUser(id);
    } catch (EmptyResultDataAccessException ex) {
      return null;
    }
  }

  public List<User> getAllByEmail(String email) {
    if (email==null || email.isEmpty()) {
      return List.of();
    } else {
      List<Integer> userIds;

      userIds = jdbcTemplate.queryForList(
              "SELECT id FROM users WHERE normalize_email(email)=normalize_email(?) ORDER BY id DESC",
              Integer.class,
              email.toLowerCase());

      return userIds.stream().map(this::getUser).collect(Collectors.toList());
    }
  }

  public boolean canResetPassword(User user) {
    return !jdbcTemplate.queryForObject(
            "SELECT lostpwd>CURRENT_TIMESTAMP-'1 week'::interval as datecheck FROM users WHERE id=?",
            Boolean.class,
            user.getId()
    );
  }

  @CacheEvict(value="Users", key="#user.id")
  public void activateUser(User user) {
    jdbcTemplate.update("UPDATE users SET activated='t' WHERE id=?", user.getId());
  }

  public boolean updateName(User user, String name) {
    return jdbcTemplate.update("UPDATE users SET name=? WHERE id=? and name is distinct from ?", name, user.getId(), name) > 0;
  }

  public boolean updateUrl(User user, String url) {
    return jdbcTemplate.update("UPDATE users SET url=? WHERE id=? and url is distinct from ?", url, user.getId(), url) > 0;
  }

  public boolean updateTown(User user, String town) {
    return jdbcTemplate.update("UPDATE users SET town=? WHERE id=? AND town is distinct from ?", town, user.getId(), town) > 0;
  }

  public void setNewEmail(User user, String newEmail) {
    jdbcTemplate.update("UPDATE users SET new_email=? WHERE id=?", newEmail, user.getId());
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
  public int createUser(
          String name,
          String nick,
          String password,
          String url,
          InternetAddress mail,
          String town) {
    PasswordEncryptor encryptor = new BasicPasswordEncryptor();

    int userid = jdbcTemplate.queryForObject("select nextval('s_uid') as userid", Integer.class);

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

    return userid;
  }

  public boolean isUserExists(String nick) {
    int c = jdbcTemplate.queryForObject("SELECT count(*) as c FROM users WHERE nick=?", Integer.class, nick);

    return c>0;
  }

  public boolean hasSimilarUsers(String nick) {  
    int c = jdbcTemplate.queryForObject("SELECT count(*) FROM users WHERE " +
            "score>=200 AND lastlogin>CURRENT_TIMESTAMP-'3 years'::INTERVAL " +
            "AND levenshtein_less_equal(lower(nick), ?, 1)<=1", Integer.class, nick.toLowerCase());

    return nick.contains("cocucka") || c>0;
  }

  public String getNewEmail(@Nonnull User user) {
    return jdbcTemplate.queryForObject("SELECT new_email FROM users WHERE id=?", String.class, user.getId());
  }

  @CacheEvict(value="Users", key="#user.id")
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void acceptNewEmail(@Nonnull User user, @Nonnull String newEmail) {
    jdbcTemplate.update("UPDATE users SET email=?, new_email=null WHERE id=?", newEmail, user.getId());
    userLogDao.logAcceptNewEmail(user, newEmail);
  }

  /**
   * Update lastlogin time in database
   * @param user logged user
   * @throws SQLException on database failure
   */
  public void updateLastlogin(User user, boolean force) {
    if (force) {
      jdbcTemplate.update("UPDATE users SET lastlogin=CURRENT_TIMESTAMP WHERE id=?", user.getId());
    } else {
      jdbcTemplate.update("UPDATE users SET lastlogin=CURRENT_TIMESTAMP WHERE id=? AND CURRENT_TIMESTAMP-lastlogin > '1 hour'::interval", user.getId());
    }
  }

  /**
   * Sign out from all sessions
   * @param user logged user
   */
  public void unloginAllSessions(User user) {
    jdbcTemplate.update("UPDATE users SET token_generation=token_generation+1 WHERE id=?", user.getId());
  }

  public int getTokenGeneration(String nick) {
    return jdbcTemplate.queryForObject("SELECT token_generation FROM users WHERE nick=?", Integer.class, nick);
  }

  public int countUnactivated(String ip) {
    return jdbcTemplate.queryForObject(
            "select count(*) from users join user_log on users.id = user_log.userid " +
                    "where not activated and not blocked and regdate>CURRENT_TIMESTAMP-'1 day'::interval " +
                      "and action='register' and info->'ip'=?", Integer.class, ip);
  }
}
