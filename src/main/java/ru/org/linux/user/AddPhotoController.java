/*
 * Copyright 1998-2013 Linux.org.ru
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.image.ImageParam;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Random;

@Controller
public class AddPhotoController {
  private static final Logger logger = LoggerFactory.getLogger(AddPhotoController.class);

  public static final UriTemplate PROFILE_NOCACHE_URI_TEMPLATE = new UriTemplate("/people/{nick}/profile");

  @Autowired
  private UserDao userDao;

  @Autowired
  private SiteConfig siteConfig;

  @Autowired
  private UserService userService;

  @RequestMapping(value = "/addphoto.jsp", method = RequestMethod.GET)
  @PreAuthorize("hasRole('ROLE_ANONYMOUS')")
  public ModelAndView showForm() {
    return new ModelAndView("addphoto");
  }

  @RequestMapping(value = "/addphoto.jsp", method = RequestMethod.POST)
  @PreAuthorize("hasRole('ROLE_ANONYMOUS')")
  public ModelAndView addPhoto(@RequestParam("file") MultipartFile file, HttpServletResponse response) throws Exception {

    if (file==null || file.isEmpty()) {
      return new ModelAndView("addphoto", "error", "изображение не задано");      
    }

    try {
      File uploadedFile = File.createTempFile("userpic", "", new File(siteConfig.getPathPrefix() + "/linux-storage/tmp/"));

      file.transferTo(uploadedFile);

      ImageParam param = userService.checkUserPic(uploadedFile);
      String extension = param.getExtension();

      Random random = new Random();

      String photoname;
      File photofile;

      do {
        photoname = Integer.toString(AuthUtil.getCurrentUser().getId()) + ':' + random.nextInt() + '.' + extension;
        photofile = new File(siteConfig.getHTMLPathPrefix() + "/photos", photoname);
      } while (photofile.exists());

      if (!uploadedFile.renameTo(photofile)) {
        logger.warn("Can't move photo to " + photofile);
        throw new ScriptErrorException("Can't move photo: internal error");
      }

      userDao.setPhoto(AuthUtil.getCurrentUser(), photoname);

      logger.info("Установлена фотография пользователем " + AuthUtil.getCurrentUser().getNick());

      return new ModelAndView(new RedirectView(UriComponentsBuilder.fromUri(PROFILE_NOCACHE_URI_TEMPLATE.expand(AuthUtil.getCurrentUser().getNick())).queryParam("nocache", Integer.toString(random.nextInt()) + '=').build().encode().toString()));
    } catch (IOException ex){
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return new ModelAndView("addphoto", "error", ex.getMessage());
    } catch (BadImageException ex){
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return new ModelAndView("addphoto", "error", ex.getMessage());
    } catch (UserErrorException ex) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return new ModelAndView("addphoto", "error", ex.getMessage());
    }
  }
}
