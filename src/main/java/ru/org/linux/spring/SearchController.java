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

import java.beans.PropertyEditorSupport;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import ru.org.linux.site.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.PropertyAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DefaultBindingErrorProcessor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
public class SearchController {
  private SolrServer solrServer;
  private SectionStore sectionStore;

  @Autowired
  @Required
  public void setSolrServer(SolrServer solrServer) {
    this.solrServer = solrServer;
  }

  @Autowired
  public void setSectionStore(SectionStore sectionStore) {
    this.sectionStore = sectionStore;
  }

  @ModelAttribute("sorts")
  public static Map<SearchViewer.SearchOrder, String> getSorts() {
    ImmutableMap.Builder<SearchViewer.SearchOrder, String> builder = ImmutableSortedMap.naturalOrder();

    for (SearchViewer.SearchOrder value : SearchViewer.SearchOrder.values()) {
      builder.put(value, value.getName());
    }

    return builder.build();
  }

  @ModelAttribute("intervals")
  public static Map<SearchViewer.SearchInterval, String> getIntervals() {
    ImmutableMap.Builder<SearchViewer.SearchInterval, String> builder = ImmutableSortedMap.naturalOrder();

    for (SearchViewer.SearchInterval value : SearchViewer.SearchInterval.values()) {
      builder.put(value, value.getTitle());
    }

    return builder.build();
  }

  @ModelAttribute("ranges")
  public static Map<SearchViewer.SearchRange, String> getRanges() {
    ImmutableMap.Builder<SearchViewer.SearchRange, String> builder = ImmutableSortedMap.naturalOrder();

    for (SearchViewer.SearchRange value : SearchViewer.SearchRange.values()) {
      builder.put(value, value.getTitle());
    }

    return builder.build();
  }

  @SuppressWarnings({"SameReturnValue"})
  @RequestMapping(value="/search.jsp", method={RequestMethod.GET, RequestMethod.HEAD})
  public String search(
    Model model,
    @ModelAttribute("query") SearchRequest query,
    BindingResult bindingResult
  ) throws Exception {
    Map<String, Object> params = model.asMap();

    boolean initial = query.isInitial();

    if (!initial && !bindingResult.hasErrors()) {
      if (!query.getQ().equals(query.getOldQ())) {
        query.setSection(0);
        query.setGroup(0);
      }

      query.setOldQ(query.getQ());

      SearchViewer sv = new SearchViewer(query);

      Connection db = null;
      try {
        long current = System.currentTimeMillis();
        db = LorDataSource.getConnection();

        if (query.getGroup()!=0) {
          Group group = new Group(db, query.getGroup());

          if (group.getSectionId()!=query.getSection()) {
            query.setGroup(0);
          }
        }

        QueryResponse response = sv.performSearch(solrServer);

        SolrDocumentList list = response.getResults();
        Collection<SearchItem> res = new ArrayList<SearchItem>(list.size());
        for (SolrDocument doc : list) {
          res.add(new SearchItem(db, doc));
        }

        FacetField sectionFacet = response.getFacetField("section_id");

        if (sectionFacet!=null && sectionFacet.getValueCount()>1) {
          params.put("sectionFacet", buildSectionFacet(sectionFacet));
        } else if (sectionFacet!=null && sectionFacet.getValueCount()==1) {
          FacetField.Count first = sectionFacet.getValues().get(0);

          query.setSection(Integer.parseInt(first.getName()));
        }

        FacetField groupFacet = response.getFacetField("group_id");

        if (groupFacet!=null && groupFacet.getValueCount()>1) {
          params.put("groupFacet", buildGroupFacet(db, query.getSection(), groupFacet));
        }

        long time = System.currentTimeMillis() - current;

        params.put("result", res);
        params.put("searchTime", response.getElapsedTime());
        params.put("numFound", list.getNumFound());

        if (list.getNumFound()>query.getOffset() + SearchViewer.SEARCH_ROWS) {
          params.put("nextLink", "/search.jsp?"+query.getQuery(query.getOffset()+SearchViewer.SEARCH_ROWS));
        }

        if (query.getOffset() - SearchViewer.SEARCH_ROWS >= 0) {
          params.put("prevLink", "/search.jsp?"+query.getQuery(query.getOffset() - SearchViewer.SEARCH_ROWS));
        }

        params.put("time", time);
      } finally {
        JdbcUtils.closeConnection(db);
      }
    }

    return "search";
  }

  private Map<Integer, String> buildSectionFacet(FacetField sectionFacet) throws SectionNotFoundException {
    ImmutableMap.Builder<Integer, String> builder = ImmutableSortedMap.naturalOrder();

    int totalCount = 0;

    for (FacetField.Count count : sectionFacet.getValues()) {
      int sectionId = Integer.parseInt(count.getName());

      String name = sectionStore.getSection(sectionId).getName().toLowerCase();

      builder.put(sectionId, name+" ("+count.getCount()+ ')');

      totalCount += count.getCount();
    }

    builder.put(0, "все ("+Integer.toString(totalCount)+ ')');

    return builder.build();
  }

  private static Map<Integer, String> buildGroupFacet(Connection db, int sectionId, FacetField groupFacet) throws  BadGroupException, SQLException {
    ImmutableMap.Builder<Integer, String> builder = ImmutableSortedMap.naturalOrder();

    int totalCount = 0;

    for (FacetField.Count count : groupFacet.getValues()) {
      int groupId = Integer.parseInt(count.getName());

      Group group = new Group(db, groupId);

      if (group.getSectionId()!=sectionId) {
        continue;
      }

      String name = group.getTitle().toLowerCase();

      builder.put(groupId, name+" ("+count.getCount()+ ')');

      totalCount += count.getCount();
    }

    builder.put(0, "все ("+Integer.toString(totalCount)+ ')');

    ImmutableMap<Integer, String> r = builder.build();
    
    if (r.size()<=2) {
      return null;
    } else {
      return r;
    }
  }

  @InitBinder
  public static void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(SearchViewer.SearchOrder.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String s) throws IllegalArgumentException {
        if ("1".equals(s)) { // for old links
          setValue(SearchViewer.SearchOrder.RELEVANCE);
        } else if ("2".equals(s)) {
          setValue(SearchViewer.SearchOrder.DATE);
        } else {
          setValue(SearchViewer.SearchOrder.valueOf(s));
        }
      }
    });

    binder.registerCustomEditor(SearchViewer.SearchInterval.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String s) throws IllegalArgumentException {
        setValue(SearchViewer.SearchInterval.valueOf(s.toUpperCase()));
      }
    });

    binder.registerCustomEditor(SearchViewer.SearchRange.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String s) throws IllegalArgumentException {
        setValue(SearchViewer.SearchRange.valueOf(s.toUpperCase()));
      }
    });

    binder.registerCustomEditor(User.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String s) throws IllegalArgumentException {
        if (s.isEmpty()) {
          setValue(null);
          return;
        }

        Connection db = null;

        try {
          db = LorDataSource.getConnection();
          setValue(User.getUser(db, s));
        } catch (SQLException e) {
          throw new RuntimeException(e);
        } catch (UserNotFoundException e) {
          throw new IllegalArgumentException(e);
        } finally {
          JdbcUtils.closeConnection(db);
        }
      }

      @Override
      public String getAsText() {
        if (getValue()==null) {
          return "";
        }

        return ((User) getValue()).getNick();
      }
    });

    binder.setBindingErrorProcessor(new DefaultBindingErrorProcessor() {
      @Override
      public void processPropertyAccessException(PropertyAccessException e, BindingResult bindingResult) {
        if (e.getCause() instanceof IllegalArgumentException &&
            e.getCause().getCause() instanceof UserNotFoundException) {
          bindingResult.rejectValue(
            e.getPropertyChangeEvent().getPropertyName(),
            null,
            e.getCause().getCause().getMessage()
          );
        } else {
          super.processPropertyAccessException(e, bindingResult);
        }
      }
    });
  }
}
