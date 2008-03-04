package ru.org.linux.spring;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class SectionController implements Controller {
  public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
    return new ModelAndView("section");
  }
}
