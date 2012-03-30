package ru.org.linux.tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TagCountersUpdater {
  private final static int HOUR = 60*60*1000;

  @Autowired
  private TagService tagService;
  
  @Scheduled(fixedDelay = HOUR)
  public void recalcTagsCounters() {
    tagService.reCalculateAllCounters();
  }
}
