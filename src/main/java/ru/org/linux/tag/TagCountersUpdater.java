package ru.org.linux.tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TagCountersUpdater {
  @Autowired
  private TagDao tagDao;
  
  @Scheduled(fixedDelay = 60*60*1000)
  public void recalcTagsCounters() {
    tagDao.recalcAllCounters();
  }
}
