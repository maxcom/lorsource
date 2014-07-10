package ru.org.linux.topic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.Template;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping(value = "/view-all.jsp", method = {RequestMethod.GET, RequestMethod.HEAD})
public class UncommitedTopicsController {
  @Autowired
  private SectionService sectionService;

  @Autowired
  private TopicListService topicListService;

  @Autowired
  private TopicPrepareService prepareService;

  @RequestMapping
  public ModelAndView viewAll(
          @RequestParam(value = "section", required = false, defaultValue = "0") int sectionId,
          HttpServletRequest request,
          HttpServletResponse response
  ) {
    Template tmpl = Template.getTemplate(request);

    ModelAndView modelAndView = new ModelAndView("view-all");

    Section section = null;
    if (sectionId != 0) {
      section = sectionService.getSection(sectionId);
      modelAndView.addObject("section", section);
      modelAndView.addObject("addlink", AddTopicController.getAddUrl(section));
    }

    response.setDateHeader("Expires", new Date(System.currentTimeMillis() - 20 * 3600 * 1000).getTime());
    response.setDateHeader("Last-Modified", new Date(System.currentTimeMillis() - 120 * 1000).getTime());

    String title;

    switch (sectionId) {
      case Section.SECTION_NEWS:
        title = "Неподтвержденные новости";
        break;
      case Section.SECTION_POLLS:
        title = "Неподтвержденные опросы";
        break;
      case Section.SECTION_GALLERY:
        title = "Неподтвержденные изображения";
        break;
      case 0:
        title = "Просмотр неподтвержденных сообщений";
        break;
      default:
        title = "Неподтвержденные: "+section.getName();
        break;
    }

    modelAndView.addObject("title", title);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -3);

    List<Topic> messages = topicListService.getAllTopicsFeed(section, calendar.getTime());
    modelAndView.addObject(
            "messages",
            prepareService.prepareMessagesForUser(
                    messages,
                    request.isSecure(),
                    tmpl.getCurrentUser(),
                    tmpl.getProf(),
                    false
            )
    );

    List<TopicListDto.DeletedTopic> deleted = topicListService.getDeletedTopics(sectionId);

    modelAndView.addObject("deletedTopics", deleted);
    modelAndView.addObject("sections", sectionService.getSectionList());

    return modelAndView;
  }
}
