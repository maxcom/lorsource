/*
 * Copyright 1998-2024 Linux.org.ru
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
package ru.org.linux.user

import com.typesafe.scalalogging.StrictLogging
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.util.UriTemplate
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.spring.SiteConfig
import ru.org.linux.util.BadImageException

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.Random

object UserpicController {
  private val ProfileUriTemplate = new UriTemplate("/people/{nick}/profile")
}

@Controller
class UserpicController(userDao: UserDao, siteConfig: SiteConfig, userService: UserService) extends StrictLogging {
  @RequestMapping(value = Array("/addphoto.jsp"), method = Array(RequestMethod.GET))
  def showForm: ModelAndView = AuthorizedOnly { currentUser =>
    if (userService.canLoadUserpic(currentUser.user)) {
      new ModelAndView("addphoto")
    } else {
      new ModelAndView("errors/code403")
    }
  }

  @RequestMapping(value = Array("/addphoto.jsp"), method = Array(RequestMethod.POST))
  def addPhoto(@RequestParam("file") file: MultipartFile, response: HttpServletResponse): ModelAndView = AuthorizedOnly { currentUser =>
    if (!userService.canLoadUserpic(currentUser.user)) {
      throw new AccessViolationException("Forbidden")
    }

    if (file == null || file.isEmpty) {
      new ModelAndView("addphoto", "error", "изображение не задано")
    } else {
      val uploadedFile = Files.createTempFile("userpic-", "")

      try {
        file.transferTo(uploadedFile)

        val extension = userService.checkUserPic(uploadedFile.toFile).getExtension

        val random = new Random

        var photoname: String = null
        var photofile: File = null

        do {
          photoname = s"${currentUser.user.getId}:${random.nextInt}.$extension"
          photofile = new File(siteConfig.getUploadPath + "/photos", photoname)
        } while (photofile.exists)

        Files.move(uploadedFile, photofile.toPath)
        photofile.setReadable(true, true)

        userDao.setPhoto(currentUser.user, photoname)

        logger.info("Установлена фотография пользователем {}", currentUser.user.getNick)

        val profileUri =
          UriComponentsBuilder
            .fromUri(UserpicController.ProfileUriTemplate.expand(currentUser.user.getNick))
            .queryParam("nocache", Integer.toString(random.nextInt))
            .build
            .encode
            .toString

        new ModelAndView(new RedirectView(profileUri))
      } catch {
        case ex@(_: IOException | _: BadImageException | _: UserErrorException) =>
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
          new ModelAndView("addphoto", "error", ex.getMessage)
      } finally {
        Files.deleteIfExists(uploadedFile)
      }
    }
  }
}