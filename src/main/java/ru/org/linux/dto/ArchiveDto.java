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

package ru.org.linux.dto;

public class ArchiveDto {
  private int year;
  private int month;
  private int count;
  private SectionDto sectionDto;
  private GroupDto groupDto;

  private static final long serialVersionUID = 5862774559965251295L;

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }

  public int getMonth() {
    return month;
  }

  public void setMonth(int month) {
    this.month = month;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public SectionDto getSectionDto() {
    return sectionDto;
  }

  public void setSectionDto(SectionDto sectionDto) {
    this.sectionDto = sectionDto;
  }

  public GroupDto getGroupDto() {
    return groupDto;
  }

  public void setGroupDto(GroupDto groupDto) {
    this.groupDto = groupDto;
  }

  public String getLink() {
    if (groupDto != null) {
      return groupDto.getArchiveLink(year, month);
    }
    return sectionDto.getArchiveLink(year, month);
  }
}
