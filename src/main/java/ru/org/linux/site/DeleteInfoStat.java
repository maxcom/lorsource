package ru.org.linux.site;

public class DeleteInfoStat {
  private final String reason;
  private final int count;
  private final int sum;

  public DeleteInfoStat(String reason, int count, int sum) {
    this.reason = reason;
    this.count = count;
    this.sum = sum;
  }

  public String getReason() {
    return reason;
  }

  public int getCount() {
    return count;
  }

  public int getSum() {
    return sum;
  }

  public double getAvg() {
    if (count==0) {
      return 0;
    } else {
      return sum/(double) count;
    }
  }
}
