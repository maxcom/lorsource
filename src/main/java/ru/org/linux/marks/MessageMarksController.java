package ru.org.linux.marks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.Template;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

@Controller
public class MessageMarksController {
  @Autowired
  private MessageMarksDao marksDao;
  
  @RequestMapping(value = "/mark", method = RequestMethod.POST)
  public @ResponseBody int mark(
          @RequestParam int msgid,
          @RequestParam int mark,
          HttpServletRequest request
  ) throws AccessViolationException {
    Template tmpl = Template.getTemplate(request);
    
    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("not authorized");
    }

    MessageMark messageMark = MessageMark.getById(mark);
    marksDao.mark(msgid, tmpl.getCurrentUser().getId(), messageMark);

    ImmutableMap.Builder<Integer, Integer> builder = ImmutableMap.builder();

    Map<MessageMark, Integer> marks = marksDao.getMessageMarks(msgid);

    return marks.get(messageMark);
  }
}
