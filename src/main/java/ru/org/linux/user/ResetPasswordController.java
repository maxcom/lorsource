package ru.org.linux.user;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.spring.Configuration;
import ru.org.linux.util.StringUtil;

import java.sql.Timestamp;

@Controller
@RequestMapping(value="/reset-password")
public class ResetPasswordController {
  @Autowired
  private UserDao userDao;

  @Autowired
  private Configuration configuration;

  @RequestMapping(method=RequestMethod.GET)
  public ModelAndView showCodeForm() {
    return new ModelAndView("reset-password-form");
  }

  @RequestMapping(method=RequestMethod.POST)
  public ModelAndView resetPassword(
    @RequestParam("nick") String nick,
    @RequestParam("code") String formCode
  ) throws Exception {
    User user = userDao.getUser(nick);

    user.checkBlocked();
    user.checkAnonymous();

    if (user.isAdministrator()) {
      throw new AccessViolationException("this feature is not for you, ask me directly");
    }

    Timestamp resetDate = userDao.getResetDate(user);

    String resetCode = UserService.getResetCode(configuration.getSecret(), user.getNick(), user.getEmail(), resetDate);

    if (!resetCode.equals(formCode)) {
      throw new UserErrorException("Код не совпадает");
    }

    String password = userDao.resetPassword(user);

    return new ModelAndView(
            "action-done",
            ImmutableMap.of(
                    "message", "Установлен новый пароль",
                    "bigMessage", "Ваш новый пароль: " + StringUtil.escapeHtml(password)
            )
    );
  }
}
