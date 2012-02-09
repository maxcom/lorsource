package ru.org.linux.topic;

/**
 * <Short description here>.
 *
 * @author Viachaslau Zanko
 * @version 2012-02-09
 *          created 09.02.12 16:13
 */
public class TopicListForm {
  private Integer section;
  private Integer group;
  private String tag;
  private Integer offset;
  private String output;
  private Integer month;
  private Integer year;
  private String filter;


  public Integer getSection() {
    return section;
  }

  public void setSection(Integer section) {
    this.section = section;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public Integer getOffset() {
    return offset;
  }

  public void setOffset(Integer offset) {
    this.offset = offset;
  }

  public String getOutput() {
    return output;
  }

  public void setOutput(String output) {
    this.output = output;
  }

  public Integer getGroup() {
    return group;
  }

  public void setGroup(Integer group) {
    this.group = group;
  }

  public Integer getMonth() {
    return month;
  }

  public void setMonth(Integer month) {
    this.month = month;
  }

  public Integer getYear() {
    return year;
  }

  public void setYear(Integer year) {
    this.year = year;
  }

  public String getFilter() {
    return filter;
  }

  public void setFilter(String filter) {
    this.filter = filter;
  }
}
