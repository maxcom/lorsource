package ru.org.linux.spring;

import ru.org.linux.dto.UserDto;
import ru.org.linux.site.UserNotFoundException;
import ru.org.linux.spring.dao.UserDao;

import java.beans.PropertyEditorSupport;

public class UserPropertyEditor extends PropertyEditorSupport {
  private final UserDao userDao;

  public UserPropertyEditor(UserDao userDao) {
    this.userDao = userDao;
  }

  @Override
  public void setAsText(String s) throws IllegalArgumentException {
    if (s.isEmpty()) {
      setValue(null);
      return;
    }

    try {
      setValue(userDao.getUser(s));
    } catch (UserNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public String getAsText() {
    if (getValue() == null) {
      return "";
    }

    return ((UserDto) getValue()).getNick();
  }
}
