package ru.org.linux.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.Template;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/notifications-count")
public class UserEventApiController {
  @ResponseBody
  @RequestMapping(method= RequestMethod.GET)
  public int getEventsCount(HttpServletRequest request, HttpServletResponse response) throws Exception {
    Template tmpl = Template.getTemplate(request);
    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("not authorized");
    }

    response.setHeader("Cache-control", "no-cache");

    return tmpl.getCurrentUser().getUnreadEvents();
  }
}
