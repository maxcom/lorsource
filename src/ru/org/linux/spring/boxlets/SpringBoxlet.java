package ru.org.linux.spring.boxlets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * User: sreentenko
 * Date: 01.05.2009
 * Time: 0:55:16
 */
public abstract class SpringBoxlet extends AbstractController {

  protected abstract ModelAndView getData(HttpServletRequest request,
                                          HttpServletResponse response);

  protected ModelAndView handleRequestInternal(HttpServletRequest request,
                                               HttpServletResponse response) throws Exception {
    ModelAndView mav = getData(request, response);
    if (mav == null){
      mav = new ModelAndView();
    }

    if (request.getParameterMap().containsKey("edit")){
      mav.addObject("editMode", Boolean.TRUE);
    }
    return mav;
  }
}
