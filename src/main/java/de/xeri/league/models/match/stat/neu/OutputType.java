package de.xeri.league.models.match.stat.neu;

/**
 * Created by Lara on 22.04.2022 for web
 */
public enum OutputType {
  MAX ("max("),
  AVG("avg("),
  SUM("sum("),
  MIN("min("),
  COUNT("count(");

  private final String query;

  OutputType(String query) {
    this.query = query;
  }

  public String getQuery() {
    return query;
  }
}
