package ru.org.linux.tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TagCountersUpdater {
  private static final int HOUR = 60*60*1000;
  private static final int FIVE_MINS = 5 * 60 * 1000;

  @Autowired
  private TagService tagService;
  
  @Scheduled(fixedDelay = HOUR, initialDelay = FIVE_MINS)
  public void recalcTagsCounters() {
    tagService.reCalculateAllCounters();
  }
}
