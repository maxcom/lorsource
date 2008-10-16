package ru.org.linux.spring;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

public class AddMessageController extends AbstractController {
  @Override
  protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();
    
    return new ModelAndView("add", params);
  }
}
