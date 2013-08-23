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

package ru.org.linux.search;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSortedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.search.SearchViewer.SearchInterval;
import ru.org.linux.search.SearchViewer.SearchOrder;
import ru.org.linux.search.SearchViewer.SearchRange;
import ru.org.linux.section.SectionNotFoundException;
import ru.org.linux.section.SectionService;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserPropertyEditor;
import ru.org.linux.util.ExceptionBindingErrorProcessor;
import ru.org.linux.util.bbcode.LorCodeService;

import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Controller
public class SearchController {
  private static final Log logger = LogFactory.getLog(SearchQueueListener.class);
  private SolrServer solrServer;
  @Autowired
  private SectionService sectionService;
  private UserDao userDao;
  private GroupDao groupDao;
  private LorCodeService lorCodeService;
  
  @Autowired
  private MsgbaseDao msgbaseDao;

  @Autowired
  public void setSolrServer(SolrServer solrServer) {
    this.solrServer = solrServer;
  }

  @Autowired
  public void setUserDao(UserDao userDao) {
    this.userDao = userDao;
  }

  @Autowired
  public void setGroupDao(GroupDao groupDao) {
    this.groupDao = groupDao;
  }

  @Autowired
  public void setLorCodeService(LorCodeService lorCodeService) {
    this.lorCodeService = lorCodeService;
  }

  @ModelAttribute("sorts")
  public static Map<SearchOrder, String> getSorts() {
    Builder<SearchOrder, String> builder = ImmutableSortedMap.naturalOrder();

    for (SearchOrder value : SearchOrder.values()) {
      builder.put(value, value.getName());
    }

    return builder.build();
  }

  @ModelAttribute("intervals")
  public static Map<SearchInterval, String> getIntervals() {
    Builder<SearchInterval, String> builder = ImmutableSortedMap.naturalOrder();

    for (SearchInterval value : SearchInterval.values()) {
      builder.put(value, value.getTitle());
    }

    return builder.build();
  }

  @ModelAttribute("ranges")
  public static Map<SearchRange, String> getRanges() {
    Builder<SearchRange, String> builder = ImmutableSortedMap.naturalOrder();

    for (SearchRange value : SearchRange.values()) {
      builder.put(value, value.getTitle());
    }

    return builder.build();
  }

  @RequestMapping(value = "/search.jsp", method = {RequestMethod.GET, RequestMethod.HEAD})
  public String search(
          HttpServletRequest request,
          Model model,
          @ModelAttribute("query") SearchRequest query,
          BindingResult bindingResult
  ) throws Exception {
    Map<String, Object> params = model.asMap();

    boolean initial = query.isInitial();

    if (!initial && !bindingResult.hasErrors()) {
      if (!query.getQ().equals(query.getOldQ())) {
        query.setSection(null);
        query.setGroup(0);
      }

      query.setOldQ(query.getQ());

      if (query.getQ().trim().isEmpty()) {
        return "redirect:/search.jsp";
      }

      SearchViewer sv = new SearchViewer(query);

      if (query.getGroup() != 0) {
        Group group = groupDao.getGroup(query.getGroup());

        if ("wiki".equals(query.getSection()) || group.getSectionId() != Integer.valueOf(query.getSection())) {
          query.setGroup(0);
        }
      }

      QueryResponse response = sv.performSearch(solrServer);

      long current = System.currentTimeMillis();

      SolrDocumentList list = response.getResults();
      Collection<SearchItem> res = new ArrayList<>(list.size());

      for (SolrDocument doc : list) {
        res.add(new SearchItem(doc, userDao, msgbaseDao, lorCodeService, request.isSecure()));
      }

      FacetField sectionFacet = response.getFacetField("section");

      if (sectionFacet != null && sectionFacet.getValueCount() > 1) {
        params.put("sectionFacet", buildSectionFacet(sectionFacet));
      } else if (sectionFacet != null && sectionFacet.getValueCount() == 1) {
        Count first = sectionFacet.getValues().get(0);

        query.setSection(first.getName());
      }

      FacetField groupFacet = response.getFacetField("group_id");

      if (groupFacet != null && groupFacet.getValueCount() > 1) {
        params.put("groupFacet", buildGroupFacet(query.getSection(), groupFacet));
      }

      long time = System.currentTimeMillis() - current;

      params.put("result", res);
      params.put("searchTime", response.getElapsedTime());
      params.put("numFound", list.getNumFound());

      if (list.getNumFound() > query.getOffset() + SearchViewer.SEARCH_ROWS) {
        params.put("nextLink", "/search.jsp?" + query.getQuery(query.getOffset() + SearchViewer.SEARCH_ROWS));
      }

      if (query.getOffset() - SearchViewer.SEARCH_ROWS >= 0) {
        params.put("prevLink", "/search.jsp?" + query.getQuery(query.getOffset() - SearchViewer.SEARCH_ROWS));
      }

      params.put("time", time);
    }

    return "search";
  }

  private Map<String, String> buildSectionFacet(FacetField sectionFacet) throws SectionNotFoundException {
    Builder<String, String> builder = ImmutableSortedMap.naturalOrder();

    int totalCount = 0;

    for (Count count : sectionFacet.getValues()) {
      if("wiki".equals(count.getName())) {
        builder.put(count.getName(), count.getName() + " (" + count.getCount() + ')');
      } else {
        int sectionId = Integer.parseInt(count.getName());
        String name = sectionService.getSection(sectionId).getName().toLowerCase();
        builder.put(count.getName(), name + " (" + count.getCount() + ')');
      }

      totalCount += count.getCount();
    }

    builder.put("0", "все (" + Integer.toString(totalCount) + ')');

    return builder.build();
  }

  private Map<Integer, String> buildGroupFacet(String section, FacetField groupFacet) {
    Builder<Integer, String> builder = ImmutableSortedMap.naturalOrder();
    if (section == null || section.isEmpty() || "wiki".equals(section)) {
      return null;
    }

    int totalCount = 0;

    for (Count count : groupFacet.getValues()) {
      if("0".equals(count.getName())) {
        continue;
      }

      int groupId = Integer.parseInt(count.getName());

      Group group = groupDao.getGroup(groupId);

      if (group.getSectionId() != Integer.valueOf(section)) {
        continue;
      }

      String name = group.getTitle().toLowerCase();

      builder.put(groupId, name + " (" + count.getCount() + ')');

      totalCount += count.getCount();
    }

    builder.put(0, "все (" + Integer.toString(totalCount) + ')');

    ImmutableMap<Integer, String> r = builder.build();

    if (r.size() <= 2) {
      return null;
    } else {
      return r;
    }
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(SearchOrder.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String s) throws IllegalArgumentException {
        if ("1".equals(s)) { // for old links
          setValue(SearchOrder.RELEVANCE);
        } else if ("2".equals(s)) {
          setValue(SearchOrder.DATE);
        } else {
          setValue(SearchOrder.valueOf(s));
        }
      }
    });

    binder.registerCustomEditor(SearchInterval.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String s) throws IllegalArgumentException {
        setValue(SearchInterval.valueOf(s.toUpperCase()));
      }
    });

    binder.registerCustomEditor(SearchRange.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String s) throws IllegalArgumentException {
        setValue(SearchRange.valueOf(s.toUpperCase()));
      }
    });

    binder.registerCustomEditor(User.class, new UserPropertyEditor(userDao));

    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }
}
