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

package ru.org.linux.spring.boxlets;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.Template;
import ru.org.linux.user.ProfileProperties;
import ru.org.linux.spring.commons.CacheProvider;
import ru.org.linux.topic.TagCloudDao;

@Controller
public class TagCloudBoxlet extends AbstractBoxlet {
  private CacheProvider cacheProvider;
  private TagCloudDao tagDao;

  public TagCloudDao getTagDao() {
    return tagDao;
  }
  @Autowired
  public void setTagDao(TagCloudDao tagDao) {
    this.tagDao = tagDao;
  }

  @Autowired
  public void setCacheProvider(CacheProvider cacheProvider) {
    this.cacheProvider = cacheProvider;
  }

  @Override
  @RequestMapping("/tagcloud.boxlet")
  protected ModelAndView getData(HttpServletRequest request) throws Exception {
    ProfileProperties profile = Template.getTemplate(request).getProf();
    final int i = profile.getTags();
    String key = getCacheKey() + "?count=" + i;

    List<TagCloudDao.TagDTO> list = getFromCache(cacheProvider, key, new GetCommand<List<TagCloudDao.TagDTO>>() {
      @Override
      public List<TagCloudDao.TagDTO> get() {
        return getTagDao().getTags(i);
      }
    });
    ModelAndView mav = new ModelAndView("boxlets/tagcloud", "tags", list);
    mav.addObject("count", i);
    return mav;
  }


  @Override
  public int getExpiryTime() {
    return super.getExpiryTime() * 10;
  }
}
