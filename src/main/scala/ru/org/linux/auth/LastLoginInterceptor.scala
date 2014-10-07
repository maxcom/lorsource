package ru.org.linux.auth

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import ru.org.linux.user.UserDao

class LastLoginInterceptor @Autowired() (userDao:UserDao) extends HandlerInterceptorAdapter {
  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any) = {
    if (AuthUtil.isSessionAuthorized) {
      userDao.updateLastlogin(AuthUtil.getCurrentUser, false)
    }

    true
  }
}
