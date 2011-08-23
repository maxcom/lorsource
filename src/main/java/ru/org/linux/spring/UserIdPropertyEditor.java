package ru.org.linux.spring;

import ru.org.linux.site.User;
import ru.org.linux.site.UserNotFoundException;
import ru.org.linux.spring.dao.UserDao;

import java.beans.PropertyEditorSupport;

class UserIdPropertyEditor extends PropertyEditorSupport {
  private final UserDao userDao;

  public UserIdPropertyEditor(UserDao userDao) {
    this.userDao = userDao;
  }

  @Override
  public void setAsText(String s) throws IllegalArgumentException {
    if (s.isEmpty()) {
      setValue(null);
      return;
    }

    try {
      setValue(userDao.getUserCached(Integer.parseInt(s)));
    } catch (UserNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public String getAsText() {
    if (getValue() == null) {
      return "";
    }

    return Integer.toString(((User) getValue()).getId());
  }
}
