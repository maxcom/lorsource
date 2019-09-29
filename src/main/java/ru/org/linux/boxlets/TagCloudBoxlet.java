/*
 * Copyright 1998-2017 Linux.org.ru
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

package ru.org.linux.boxlets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.tag.TagCloudDao;
import ru.org.linux.tag.TagCloudDao.TagDTO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class TagCloudBoxlet extends AbstractBoxlet {
  private static final int TAGS_IN_CLOUD = 75;

  @Autowired
  private TagCloudDao tagDao;

  @Override
  @RequestMapping("/tagcloud.boxlet")
  protected ModelAndView getData(HttpServletRequest request) {

    List<TagDTO> list = tagDao.getTags(TAGS_IN_CLOUD);
    return new ModelAndView("boxlets/tagcloud", "tags", list);
  }
}
