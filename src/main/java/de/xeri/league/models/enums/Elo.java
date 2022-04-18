package de.xeri.league.models.enums;

/**
 * Created by Lara on 29.03.2022 for TRUES
 */
public enum Elo {
  UNRANKED(50),
  IRON_IV(100),
  IRON_III(200),
  IRON_II(300),
  IRON_I(400),
  BRONZE_IV(600),
  BRONZE_III(700),
  BRONZE_II(800),
  BRONZE_I(900),
  SILVER_IV(1100),
  SILVER_III(1200),
  SILVER_II(1300),
  SILVER_I(1400),
  GOLD_IV(1600),
  GOLD_III(1700),
  GOLD_II(1800),
  GOLD_I(1900),
  PLATIN_IV(2100),
  PLATIN_III(2200),
  PLATIN_II(2300),
  PLATIN_I(2400),
  DIAMOND_IV(2600),
  DIAMOND_III(2700),
  DIAMOND_II(2800),
  DIAMOND_I(2900),
  MASTER(3100),
  GRANDMASTER(3600),
  CHALLENGER(4100);

  private final int mmr;

  Elo(int mmr) {
    this.mmr = mmr;
  }

  public int getMmr() {
    return mmr;
  }

  public static Elo getDivision(int mmr) {
    Elo selected = Elo.UNRANKED;
    for (Elo elo : Elo.values()) {
      if (elo.getMmr() > mmr) {
        return selected;
      }
      selected = elo;
    }
    return Elo.UNRANKED;
  }
}