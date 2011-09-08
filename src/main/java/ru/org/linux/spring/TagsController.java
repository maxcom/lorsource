/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.org.linux.spring.dao.TagDao;

import java.util.Map;

@Controller
public class TagsController  {

  @Autowired
  TagDao tagDao;

  @ModelAttribute("tags")
  public Map<String, Integer> getTags() {
    return tagDao.getAllTags();
  }

  @RequestMapping("/tags")
  public String tags() throws Exception {
    return "tags";
  }

  @RequestMapping("/tags.jsp")  
  public String oldTags() throws Exception {
    return "redirect:/tags";
  }
}
