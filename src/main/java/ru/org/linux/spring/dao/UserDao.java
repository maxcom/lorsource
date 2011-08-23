package ru.org.linux.spring.dao;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jasypt.util.password.PasswordEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.site.User;
import ru.org.linux.site.UserNotFoundException;
import ru.org.linux.util.StringUtil;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class UserDao {
  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setJdbcTemplate(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
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

  public String getUserInfo(User user) {
    return jdbcTemplate.queryForObject("SELECT userinfo FROM users where id=?",
        new Object[] {user.getId()}, String.class);
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void removeUserInfo(User user) {
    String userInfo = getUserInfo(user);
    if(userInfo == null || userInfo.trim().isEmpty()) return;
    setUserInfo(user, null);
    changeScore(user, -10);
  }

  /**
   * Отчистка userpicture пользователя, с обрезанием шкворца если удляет моедратор
   * @param user пользовтель у которого чистят
   * @param cleaner пользователь который чистит
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void removePhoto(User user, User cleaner) {
    setPhoto(user, null);
    if(cleaner.canModerate() && cleaner.getId() != user.getId()){
      changeScore(user, -10);
    }
  }

  /**
   * Обновление userpic-а пользовтаеля
   * @param user пользователь
   * @param photo userpick
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void setPhoto(User user, String photo){
    jdbcTemplate.update("UPDATE users SET photo=? WHERE id=?", photo, user.getId());
  }

  /**
   * Обновление дополнительной информации пользователя
   * @param user пользователь
   * @param text текст дополнительной информации
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void setUserInfo(User user, String text){
    jdbcTemplate.update("UPDATE users SET userinfo=? where id=?", text, user.getId());
  }

  /**
   * Изменение шкворца пользовтаеля, принимает отрицательные и положительные значения
   * не накладывает никаких ограничений на параметры
   * @param user пользователь
   * @param delta дельта на которую меняется шкворец
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void changeScore(User user, int delta) {
    jdbcTemplate.update("UPDATE users SET score=score+? WHERE id=?", delta, user.getId());
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
   * Смена пароля пользователю
   * @param user пользователь которому меняется пароль
   * @param password пароль в открытом виде
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void setPassword(User user, String password){
    setPasswordWithoutTransaction(user, password);
  }

  public void setPasswordWithoutTransaction(User user, String password) {
    PasswordEncryptor encryptor = new BasicPasswordEncryptor();
    String encryptedPassword = encryptor.encryptPassword(password);
    jdbcTemplate.update("UPDATE users SET passwd=?, lostpwd = 'epoch' WHERE id=?",
        encryptedPassword, user.getId());
  }

  /**
   * Сброс пороля на случайный
   * @param user пользователь которому сбрасывается пароль
   * @return новый пароь в открытом виде
   */
  public String resetPassword(User user){
    String password = StringUtil.generatePassword();
    setPassword(user, password);
    return password;
  }

  public String resetPasswordWithoutTransaction(User user) {
    String password = StringUtil.generatePassword();
    setPasswordWithoutTransaction(user, password);
    return password;
  }

  /**
   * Блокирование пользователя без транзакации(используется в CommentDao для массового удаления с блокировкой)
   * @param user пользователь которого блокируем
   * @param moderator моджератор который блокирует
   * @param reason причина блокировки
   * @throws UserNotFoundException если пользовтаеля нет, генерируем это исклюучение
   */
  public void blockWithoutTransaction(User user, User moderator, String reason) throws UserNotFoundException {
    jdbcTemplate.update("UPDATE users SET blocked='t' WHERE id=?", user.getId());
    jdbcTemplate.update("INSERT INTO ban_info (userid, reason, ban_by) VALUES (?, ?, ?)",
        user.getId(), reason, moderator.getId());
    // Update cache
    getUser(user.getId());
  }

  /**
   * Блокировка пользователя и сброс пароля одной транзикацией
   * @param user блокируемый пользователь
   * @param moderator модератор который блокирует пользователя
   * @param reason причина блокировки
   * @throws UserNotFoundException исключение, если отсутстсвует пользователь
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void blockWithResetPassword(User user, User moderator, String reason) throws UserNotFoundException {

    jdbcTemplate.update("UPDATE users SET blocked='t' WHERE id=?", user.getId());
    jdbcTemplate.update("INSERT INTO ban_info (userid, reason, ban_by) VALUES (?, ?, ?)",
        user.getId(), reason, moderator.getId());
    PasswordEncryptor encryptor = new BasicPasswordEncryptor();
    String password = encryptor.encryptPassword(StringUtil.generatePassword());
    jdbcTemplate.update("UPDATE users SET passwd=?, lostpwd = 'epoch' WHERE id=?",
        password, user.getId());
    // Update cache
    getUser(user.getId());
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
}
