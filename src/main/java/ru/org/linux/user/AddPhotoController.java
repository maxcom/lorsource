/*
 * Copyright 1998-2012 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.Template;
import ru.org.linux.site.*;
import ru.org.linux.spring.Configuration;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;

import javax.servlet.ServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Random;

@Controller
public class AddPhotoController extends ApplicationObjectSupport {
  @Autowired
  private UserDao userDao;

  @Autowired
  private Configuration configuration;

  @RequestMapping(value = "/addphoto.jsp", method = RequestMethod.GET)
  public ModelAndView showForm(ServletRequest request) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    return new ModelAndView("addphoto");
  }

  @RequestMapping(value = "/addphoto.jsp", method = RequestMethod.POST)
  public ModelAndView addPhoto(@RequestParam("file") MultipartFile file, ServletRequest request) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    if (file==null || file.isEmpty()) {
      return new ModelAndView("addphoto", "error", "изображение не задано");      
    }

    try {
      File uploadedFile = File.createTempFile("userpic", "", new File(configuration.getPathPrefix() + "/linux-storage/tmp/"));

      file.transferTo(uploadedFile);

      Userpic.checkUserpic(uploadedFile);
      String extension = ImageInfo.detectImageType(uploadedFile);

      User user = tmpl.getCurrentUser();
      user.checkAnonymous();

      Random random = new Random();

      String photoname;
      File photofile;

      do {
        photoname = Integer.toString(user.getId()) + ':' + random.nextInt() + '.' + extension;
        photofile = new File(configuration.getHTMLPathPrefix() + "/photos", photoname);
      } while (photofile.exists());

      if (!uploadedFile.renameTo(photofile)) {
        logger.warn("Can't move photo to " + photofile);
        throw new ScriptErrorException("Can't move photo: internal error");
      }

      userDao.setPhoto(user, photoname);

      logger.info("Установлена фотография пользователем " + user.getNick());

      return new ModelAndView(new RedirectView("/people/" + URLEncoder.encode(user.getNick(), "UTF-8") + "/profile?nocache=" + random.nextInt()));
    } catch (IOException ex){
      return new ModelAndView("addphoto", "error", ex.getMessage());
    } catch (BadImageException ex){
      return new ModelAndView("addphoto", "error", ex.getMessage());
    } catch (UserErrorException ex) {
      return new ModelAndView("addphoto", "error", ex.getMessage());
    }
  }
}
