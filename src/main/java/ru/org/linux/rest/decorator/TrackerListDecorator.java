/*
 * Copyright 1998-2012 Linux.org.ru
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
package ru.org.linux.rest.decorator;

import ru.org.linux.tracker.TrackerItem;

import java.util.LinkedList;
import java.util.List;

/**
 * REST-API декоратор для вывода информации по списку строк трекера.
 */
public class TrackerListDecorator extends LinkedList<TrackerItemDecorator> implements List<TrackerItemDecorator>{
  public TrackerListDecorator(List<TrackerItem> trackerItems) {
    for (TrackerItem trackerItem : trackerItems) {
      add(new TrackerItemDecorator(trackerItem));
    }
  }
}
