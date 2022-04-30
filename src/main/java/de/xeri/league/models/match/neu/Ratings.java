package de.xeri.league.models.match.neu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import de.xeri.league.models.enums.DragonSoul;
import de.xeri.league.models.match.Playerperformance;
import de.xeri.league.models.match.PlayerperformanceInfo;
import lombok.val;

/**
 * Created by Lara on 26.04.2022 for web
 */
public class Ratings {
  private final List<Playerperformance> playerperformances = new ArrayList<>();

  public double get(DisplaystatType type, DisplaystatSubtype subtype) {
    return Arrays.stream(StatCategory.values()).mapToDouble(category -> get(category, type, subtype)).sum();
  }

  public double get(StatCategory category, DisplaystatType type, DisplaystatSubtype subtype) {
    double total = 0;
    for (StatSubcategory subcategory : StatSubcategory.values()) {
      if (subcategory.getCategory().equals(category)) {
        val rating = Rating.find(subcategory, type, subtype).getValue();
        final double v = get(subcategory);
        total += rating * v;
      }
    }
    return total * 100;
  }

  public double get(StatSubcategory subcategory) {
    if (subcategory.equals(StatSubcategory.OBJECTIVE_PRESSURE)) {
      return handleValues(objectiveAfterSpawn, stolenObjectivesAndContestRate, damageAgainstObjectives,
          scuttleControlOverall, junglerTakedownsBeforeObjective);

    } else if (subcategory.equals(StatSubcategory.TOPSIDE_OBJECTIVES)) {
      return handleValues(baronTime, baronTakedownsAttempts, baronPowerPlay, heraldTurrets, heraldMulticharge);

    } else if (subcategory.equals(StatSubcategory.BOTSIDE_OBJECTIVES)) {
      return handleValues(dragonTime, dragonTakedowns, elderTime, firstDrake, soulrateAndPerfect);

    } else if (subcategory.equals(StatSubcategory.WARDING)) {
      return handleValues(visionScoreAdvantage, trinketEfficiency, firstWardTime, wardsCleared, trinketSwapTime);

    } else if (subcategory.equals(StatSubcategory.CONTROLWARDS)) {
      return handleValues(controlWardsPlaced, controlWardsProtected, controlWardsEnemyJungle, firstControlWardTime, averageControlWardTime);

    } else if (subcategory.equals(StatSubcategory.TURRET_PRESSURE)) {
      return handleValues(firstTowerAdvantage, turretPlatings, turretTakedownsEarly, turretsSplitpushed, turretParticipation);

    } else if (subcategory.equals(StatSubcategory.MACRO)) {
      return handleValues(teleportKills, jungleCampsStolen, midgameXPEfficiency, midgameGoldEfficiency, lateXPGoldLead);

    } else if (subcategory.equals(StatSubcategory.ROAMING)) {
      return handleValues(minionAvantagePerRoam, xpEfficiency, goldEfficiency, roamSuccess, objectiveDamageWhileRoaming);

    } else if (subcategory.equals(StatSubcategory.GANKING)) {
      return handleValues(teamInvadesAndBuffsTaken, ganksEarlygame, proximity, gankPriority, gankSetups);

    } else if (subcategory.equals(StatSubcategory.DIVING)) {
      return handleValues(divingSuccessrate, divingDisengagerate, divesDied);

    } else if (subcategory.equals(StatSubcategory.DAMAGE)) {
      return handleValues(teamDamage, teamTankyness, teamDurability, healing, timeInCombat);

    } else if (subcategory.equals(StatSubcategory.PLAYMAKING)) {
      return handleValues(aggressiveFlash, levelupAllins, soloKillDiff, outplays, firstBloodParticipation);

    } else if (subcategory.equals(StatSubcategory.CATCHING)) {
      return handleValues(bountyGotten, assassinations, picksMade, ambushes, duelWinrate);

    } else if (subcategory.equals(StatSubcategory.SNOWBALLING)) {
      return handleValues(killsDeathsEarlygame, winsIfAhead, leadExtending);

    } else if (subcategory.equals(StatSubcategory.STRONG_PHASE)) {
      return handleValues(highestLeadMinute, lowestLeadMinute, allowComebacks, xpLead);

    } else if (subcategory.equals(StatSubcategory.TEAMFIGHTING)) {
      return handleValues(multikills, deathOrder, teamfightSuccessRate, acesEarlyAndCleanFights, teamfightDamagePercentage);

    } else if (subcategory.equals(StatSubcategory.SKIRMISHING)) {
      return handleValues(skirmishesAmount, skirmishKillBilance, skirmishSuccessRate, damagePerSkirmish, skirmishDamagePercentage);

    } else if (subcategory.equals(StatSubcategory.EARLY_INCOME)) {
      return handleValues(earlyLaneLead, laneLead, firstFullItem, earlyCreepScore, farmSupportitemEfficiency);

    } else if (subcategory.equals(StatSubcategory.INCOME)) {
      return handleValues(creepsPerMinute, xpPerMinute, goldPerMinute, creepAdvantage, trueKDA);

    } else if (subcategory.equals(StatSubcategory.ITEMIZATION)) {
      return handleValues(legendaryItems, itemsBought, mejaisTime, grievousWoundsAndPenetrationTime, startitemSold);

    } else if (subcategory.equals(StatSubcategory.SURVIVAL)) {
      return handleValues(playtimeLive, timeWithoutDying, survivedClose, deathPositioning);

    } else if (subcategory.equals(StatSubcategory.EARLY_SURVIVAL)) {
      return handleValues(firstKillDeath, firstBaseThroughRecall, laneLeadDeficitThroughDeaths, laneLeadDeficitWithoutDeaths);

    } else if (subcategory.equals(StatSubcategory.TEAM_UTILITY)) {
      return handleValues(damageShielded, crowdControl, enemiesControlled, teammatesSaved, utilityScore);

    } else if (subcategory.equals(StatSubcategory.GANK_SURVIVAL_TIMEWASTING)) {
      return handleValues(jungleTimewaste, roamsScouted);

    } else if (subcategory.equals(StatSubcategory.ISOLATION)) {
      return handleValues(minionEfficiency, isolationXPEfficiency, wardsUsed, damageTrading, resetAmount);

    } else if (subcategory.equals(StatSubcategory.PRE_FIRST_BASE)) {
      return handleValues(firstReset, preFirstBaseEnemyUnderControl, buffsAndScuttlesInitial, xpGoldLead, goldOnReset);

    } else if (subcategory.equals(StatSubcategory.POST_FIRST_BASE)) {
      return handleValues(resourceUsage, consumablesUsed, secondResetTime, postFirstBaseEnemyUnderControl, earlyDamagePercentage);

    } else if (subcategory.equals(StatSubcategory.LANE_BILANCE)) {
      return handleValues(earlyXPGoldLead, laneObjectiveAdvantage, turretplateAdvantage, laneEnemyUnderControlAdvantage);

    } else if (subcategory.equals(StatSubcategory.PLAYSTYLE)) {
      return handleValues(killsPosition, deathsPosition, keyspellsUsed, spellBilance, reactions);

    } else if (subcategory.equals(StatSubcategory.RESETS)) {
      return handleValues(resetsThroughDeaths, averageResetTime, averageResetGold, goldLostThroughResets, resetsWithTeam);

    } else if (subcategory.equals(StatSubcategory.GIVING_UP)) {
      return handleValues(ffRate, laneLeadAfterDiedEarly, farmingFromBehind, wardingFromBehind, deathsFromBehind);

    } else if (subcategory.equals(StatSubcategory.CONSISTENCY)) {
      return handleValues(earlyLevelupLead, farmAdvantageFromAhead, farmAdvantageFromBehind, xpAdvantageFromAhead, xpAdvantageFromBehind);

    } else if (subcategory.equals(StatSubcategory.VERSATILTITY)) {
      return handleValues(visionValue, roamingValue, aggressionValue, fightingValue, survivabilityValue);

    } else if (subcategory.equals(StatSubcategory.ADAPTION)) {
      return handleValues(antiHealing, penetration, damageBuild, resistanceBuild, farmstop);

    } else if (subcategory.equals(StatSubcategory.STATS)) {
      return handleValues(winrate, killParticipation, blueWinrate, redWinrate, kDA);

    }
    return 0;
  }

  private double handleValues(Stat... stats) {
    final List<Stat> stats1 = Arrays.asList(stats);
    return stats1.stream().filter(Stat::isRelevant).mapToDouble(Stat::value).average().orElse(0) * 5;
  }

  //TODO (Abgie) 28.04.2022: Ueberpruefe nullable danach nocheinmal
  //<editor-fold desc="Kategorie 1.1: OBJECTIVE_PRESSURE">
  public Stat objectiveAfterSpawn = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(p -> p.getTeamperformance().getObjectiveAtSpawn())
      .nullable();

  public Stat stolenObjectivesAndContestRate = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getStats().getObjectivesStolenAndContested())
      .nullable()
      .sub("Objectives gestohlen", Playerperformance::getObjectivesStolen)
      .sub("Objectives contestet", p -> p.getTeamperformance().getObjectiveContests());

  public Stat damageAgainstObjectives = new Stat(playerperformances, OutputType.NUMBER, 4)
      .map(Playerperformance::getObjectivesDamage)
      .nullable();

  public Stat scuttleControlOverall = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getScuttlesTotal)
      .nullable();

  public Stat junglerTakedownsBeforeObjective = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getStats().getObjectivesKilledJunglerBefore())
      .nullable()
      .sub("Jungler-Takedowns", Playerperformance::getJunglerKillsAtObjective)
      .sub("Objectives Gesamt", p -> p.getJunglerKillsAtObjective() * p.getStats().getObjectivesKilledJunglerBefore());

  //</editor-fold>
  //<editor-fold desc="Kategorie 1.2: TOPSIDE_OBJECTIVES">
  public Stat baronTime = new Stat(playerperformances, OutputType.TIME, 2)
      .map(p -> p.getTeamperformance().getBaronTime())
      .reverse();

  public Stat baronTakedownsAttempts = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getStats().getBaronTakedownsAttempts())
      .nullable()
      .sub("Baron-Kills", Playerperformance::getBaronKills)
      .sub("Baron-Executes", Playerperformance::getBaronExecutes);

  public Stat baronPowerPlay = new Stat(playerperformances, OutputType.NUMBER, 4)
      .map(p -> p.getTeamperformance().getBaronPowerplay());

  public Stat heraldTurrets = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(p -> p.getTeamperformance().getRiftTurrets());

  public Stat heraldMulticharge = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(p -> p.getTeamperformance().getRiftOnMultipleTurrets());

  //</editor-fold>
  //<editor-fold desc="Kategorie 1.3: BOTSIDE_OBJECTIVES">
  public Stat dragonTime = new Stat(playerperformances, OutputType.TIME, 2)
      .map(p -> p.getTeamperformance().getFirstDragonTime())
      .reverse();

  public Stat dragonTakedowns = new Stat(playerperformances, OutputType.NUMBER, 3)
      .map(Playerperformance::getDragonTakedowns)
      .nullable()
      .sub("Keine", p -> p.getTeamperformance().getDrakes() == 0 ? 1 : 0)
      .sub("1 Mal", p -> p.getTeamperformance().getDrakes() == 1 ? 1 : 0)
      .sub("2 Mal", p -> p.getTeamperformance().getDrakes() == 2 ? 1 : 0)
      .sub("3 Mal", p -> p.getTeamperformance().getDrakes() == 3 ? 1 : 0)
      .sub("4 Mal", p -> p.getTeamperformance().getDrakes() == 4 ? 1 : 0)
      .sub("öfter", p -> p.getTeamperformance().getDrakes() > 4 ? 1 : 0);

  public Stat elderTime = new Stat(playerperformances, OutputType.TIME, 2)
      .map(p -> p.getTeamperformance().getElderTime())
      .reverse();

  public Stat firstDrake = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getTeamperformance().isFirstDrake() ? 1 : 0)
      .nullable();

  public Stat soulrateAndPerfect = new Stat(playerperformances, OutputType.PERCENT, 3) {
    @Override
    public double calculate() {
      return playerperformances.stream().mapToDouble(p -> p.getTeamperformance().getSoul() == null ? 0 : 1).average().orElse(0) +
          playerperformances.stream().mapToDouble(p -> p.getTeamperformance().isPerfectSoul() ? 1 : 0).average().orElse(0) * 5;
    }

    @Override
    public double average() {
      return Playerperformance.get().stream().mapToDouble(p -> p.getTeamperformance().getSoul() == null ? 0 : 1).average().orElse(0) +
          Playerperformance.get().stream().mapToDouble(p -> p.getTeamperformance().isPerfectSoul() ? 1 : 0).average().orElse(0) * 5;
    }

    @Override
    public double maximum() {
      return 6;
    }

    @Override
    public double minimum() {
      return 0;
    }
  }.nullable()
      .sub("Cloud-Soul", p -> p.getTeamperformance().getSoul().equals(DragonSoul.CLOUD) ? 1 : 0)
      .sub("Hextech-Soul", p -> p.getTeamperformance().getSoul().equals(DragonSoul.HEXTECH) ? 1 : 0)
      .sub("Infernal-Soul", p -> p.getTeamperformance().getSoul().equals(DragonSoul.INFERNAL) ? 1 : 0)
      .sub("Mountain-Soul", p -> p.getTeamperformance().getSoul().equals(DragonSoul.MOUNTAIN) ? 1 : 0)
      .sub("Ocean-Soul", p -> p.getTeamperformance().getSoul().equals(DragonSoul.OCEAN) ? 1 : 0);

  //</editor-fold>
  //<editor-fold desc="Kategorie 1.4: WARDING (open)">
  public Stat visionScoreAdvantage = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getVisionscoreAdvantage)
      .nullable();

  //TODO (Abgie) 27.04.2022: Later
  public Stat trinketEfficiency = new Stat(playerperformances, OutputType.PERCENT, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  }.nullable();

  public Stat firstWardTime = new Stat(playerperformances, OutputType.TIME, 2)
      .map(p -> p.getStats().getFirstWardTime())
      .reverse();

  public Stat wardsCleared = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getWardsCleared)
      .nullable();

  public Stat trinketSwapTime = new Stat(playerperformances, OutputType.TIME, 2)
      .map(p -> p.getStats().getFirstTrinketSwap())
      .reverse();

  //</editor-fold>
  //<editor-fold desc="Kategorie 1.5: CONTROLWARDS">
  public Stat controlWardsPlaced = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getControlWards)
      .nullable();

  public Stat controlWardsProtected = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getWardsGuarded)
      .nullable();

  public Stat controlWardsEnemyJungle = new Stat(playerperformances, OutputType.TIME, 2)
      .map(Playerperformance::getControlWardUptime);

  public Stat firstControlWardTime = new Stat(playerperformances, OutputType.TIME, 2)
      .map(p -> p.getStats().getFirstControlwardTime())
      .reverse();

  public Stat averageControlWardTime = new Stat(playerperformances, OutputType.TIME, 2)
      .map(p -> p.getStats().getControlWardInventoryTime())
      .reverse();
  //</editor-fold>

  //<editor-fold desc="Kategorie 2.1: TURRET_PRESSURE">
  public Stat firstTowerAdvantage = new Stat(playerperformances, OutputType.TIME, 2)
      .map(Playerperformance::getFirstturretAdvantage)
      .nullable();

  public Stat turretPlatings = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getTurretplates)
      .nullable();

  public Stat turretTakedownsEarly = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getEarlyTurrets)
      .nullable();

  public Stat turretsSplitpushed = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getSplitpushedTurrets)
      .nullable();

  public Stat turretParticipation = new Stat(playerperformances, OutputType.PERCENT, 2)
      .map(p -> p.getStats().getTurretParticipation())
      .nullable()
      .sub("beteiligt an", Playerperformance::getTurretTakedowns)
      .sub("gesamt", p -> p.getTeamperformance().getTowers());

  //</editor-fold>
  //<editor-fold desc="Kategorie 2.2: MACRO (open)">
  public Stat teleportKills = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getTeleportKills)
      .nullable();

  public Stat jungleCampsStolen = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getCreepsInvade)
      .nullable();

  //TODO (Abgie) 27.04.2022:
  public Stat midgameXPEfficiency = new Stat(playerperformances, OutputType.PERCENT, 4) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 27.04.2022:
  public Stat midgameGoldEfficiency = new Stat(playerperformances, OutputType.PERCENT, 4) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 27.04.2022:
  public Stat lateXPGoldLead = new Stat(playerperformances, OutputType.NUMBER, 4) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  //</editor-fold>
  //<editor-fold desc="Kategorie 2.3: ROAMING (open)">
  //TODO (Abgie) 27.04.2022: Maybe Roams Scouted
  public Stat minionAvantagePerRoam = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 27.04.2022:
  public Stat xpEfficiency = new Stat(playerperformances, OutputType.NUMBER, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 27.04.2022:
  public Stat goldEfficiency = new Stat(playerperformances, OutputType.NUMBER, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 27.04.2022: Eingliedern von gankDeaths
  public Stat roamSuccess = new Stat(playerperformances, OutputType.PERCENT, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 27.04.2022:
  public Stat objectiveDamageWhileRoaming = new Stat(playerperformances, OutputType.NUMBER, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  //</editor-fold>
  //<editor-fold desc="Kategorie 2.4: GANKING (open)">
  public Stat teamInvadesAndBuffsTaken = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(p -> p.getStats().getInvadingAndBuffs())
      .nullable();

  public Stat ganksEarlygame = new Stat(playerperformances, OutputType.NUMBER, 3)
      .map(Playerperformance::getGanksEarly)
      .nullable();

  //TODO (Abgie) 27.04.2022: Time Farming, Time fighting, Time roaming -> Move Speed and Position
  public Stat proximity = new Stat(playerperformances, OutputType.PERCENT, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  public Stat gankPriority = new Stat(playerperformances, OutputType.TEXT, 12) {
    private final int ganksTop = playerperformances.stream().mapToInt(Playerperformance::getGanksTop).sum();
    private final int ganksMid = playerperformances.stream().mapToInt(Playerperformance::getGanksMid).sum();
    private final int ganksBot = playerperformances.stream().mapToInt(Playerperformance::getGanksBot).sum();

    @Override
    public double calculate() {
      if (ganksTop + ganksMid + ganksBot == 0) {
        return 0;
      }
      return (ganksTop - ganksBot) * 1d / (ganksTop + ganksMid + ganksBot);
    }

    @Override
    public String display() {
      final String str;
      if (ganksTop > ganksMid && calculate() > 0) {
        str = "Topside ";
      } else if (ganksBot > ganksMid && calculate() < 0) {
        str = "Botside ";
      } else {
        str = "Midlane ";
      }
      final Stream<Integer> ganks = Stream.of(ganksBot, ganksMid, ganksTop);
      final String suffix;
      if (ganks.mapToInt(Integer::new).sum() == 0) {
        suffix = "";
      } else {
        final int max = ganks.mapToInt(Integer::new).max().orElse(0) * 100 / ganks.mapToInt(Integer::new).sum();
        suffix = max + " %";
      }
      return str + suffix;
    }

    @Override
    public double average() {
      final Stream<Playerperformance> playerperformanceStream = Playerperformance.get().stream();
      final int allGanksTop = playerperformanceStream.mapToInt(Playerperformance::getGanksTop).sum();
      final int allGanksMid = playerperformanceStream.mapToInt(Playerperformance::getGanksMid).sum();
      final int allGanksBot = playerperformanceStream.mapToInt(Playerperformance::getGanksBot).sum();
      return (allGanksTop - allGanksBot) * 1d / (allGanksTop + allGanksMid + allGanksBot);
    }

    @Override
    public double maximum() {
      return 1;
    }

    @Override
    public double minimum() {
      return -1;
    }
  };

  public Stat gankSetups = new Stat(playerperformances, OutputType.NUMBER, 3)
      .map(Playerperformance::getGankSetups)
      .nullable();

  //</editor-fold>
  //<editor-fold desc="Kategorie 2.5: DIVING">
  public Stat divingSuccessrate = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getStats().getDivesOwn())
      .nullable()
      .sub("Dives erfolgreich", Playerperformance::getDivesSuccessful)
      .sub("Dives gescheitert", p -> p.getDivesDone() - p.getDivesSuccessful());

  public Stat divingDisengagerate = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getStats().getDivesEnemy())
      .nullable()
      .sub("Dives verhindert", Playerperformance::getDivesProtected)
      .sub("Dive-Tode", p -> p.getStats().getDivesDied());

  public Stat divesDied = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  }
      .map(p -> p.getStats().getDivesDied())
      .nullable()
      .reverse();
  //</editor-fold>

  //<editor-fold desc="Kategorie 3.1: DAMAGE">
  public Stat teamDamage = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getStats().getTeamDamage())
      .nullable()
      .sub("Totaler Schaden: ", Playerperformance::getDamageTotal)
      .sub("Teamschaden", p -> p.getDamageTotal() * 1d / p.getTeamperformance().getTotalDamage());

  public Stat teamTankyness = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getStats().getTeamDamageTaken())
      .nullable()
      .sub("Tankyness: ", Playerperformance::getDamageTaken)
      .sub("Team Tankyness", p -> p.getDamageTaken() * 1d / p.getTeamperformance().getTotalDamageTaken());

  public Stat teamDurability = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getStats().getTeamDamageMitigated())
      .nullable()
      .sub("Durability: ", Playerperformance::getDamageMitigated)
      .sub("Team Durability", p -> p.getDamageMitigated() * 1d * p.getStats().getTeamDamageMitigated());

  public Stat healing = new Stat(playerperformances, OutputType.NUMBER, 3)
      .map(Playerperformance::getDamageHealed)
      .nullable();

  //TODO (Abgie) 28.04.2022:
  public Stat timeInCombat = new Stat(playerperformances, OutputType.TIME, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  //</editor-fold>
  //<editor-fold desc="Kategorie 3.2: PLAYMAKING">
  public Stat aggressiveFlash = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getFlashAggressive)
      .nullable();

  public Stat levelupAllins = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getLevelUpAllin)
      .nullable();

  public Stat soloKillDiff = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(p -> p.getStats().getSoloKillAdvantage())
      .nullable()
      .sub("Solo Kills", Playerperformance::getSoloKills)
      .sub("Solo Deaths", p -> p.getSoloKills() - p.getStats().getSoloKillAdvantage());

  public Stat outplays = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getOutplayed)
      .nullable();

  public Stat firstBloodParticipation = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.isFirstBlood() ? 1 : 0)
      .nullable();

  //</editor-fold>
  //<editor-fold desc="Kategorie 3.3: CATCHING">
  public Stat bountyGotten = new Stat(playerperformances, OutputType.NUMBER, 3)
      .map(p -> p.getStats().getBountyDifference())
      .nullable()
      .sub("Bounty erhalten", Playerperformance::getBountyGold)
      .sub("Bounty gegeben", p -> p.getStats().getBountyDifference() - p.getBountyGold());

  //TODO (Abgie) 28.04.2022:
  public Stat assassinations = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getAssassinated)
      .nullable();

  //TODO (Abgie) 28.04.2022:
  public Stat picksMade = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getPicksMade)
      .nullable();

  public Stat ambushes = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getAmbush);

  public Stat duelWinrate = new Stat(playerperformances, OutputType.PERCENT, 2)
      .map(p -> p.getStats().getDuelWinrate())
      .nullable()
      .sub("Duelle gewonnen", p -> p.getStats().getDuelWins())
      .sub("Duelle verloren", p -> p.getStats().getDuelWins() * (1 - p.getStats().getDuelWinrate()));
  //</editor-fold>
  //<editor-fold desc="Kategorie 3.4: SNOWBALLING">
  public Stat killsDeathsEarlygame = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(p -> p.getStats().getKdEarly())
      .nullable()
      .sub("Kills Earlygame", Playerperformance::getEarlyKills)
      .sub("Deaths Earlygame", p -> p.getEarlyKills() - p.getStats().getKdEarly());

  public Stat winsIfAhead = new Stat(playerperformances, OutputType.PERCENT, 3) {
    @Override
    public double calculate() {
      return playerperformances.stream()
          .filter(p -> p.getStats().isAhead())
          .mapToDouble(p -> p.getTeamperformance().isWin() ? 1 : 0)
          .average().orElse(0);
    }

    @Override
    public double average() {
      return Playerperformance.get().stream()
          .filter(p -> p.getStats().isAhead())
          .mapToDouble(p -> p.getTeamperformance().isWin() ? 1 : 0)
          .average().orElse(0);
    }

    @Override
    public double maximum() {
      return 1;
    }

    @Override
    public double minimum() {
      return 0;
    }
  }.nullable()
      .sub("Ahead", p -> p.getStats().isAhead() ? 1 : 0);

  public Stat leadExtending = new Stat(playerperformances, OutputType.PERCENT, 3) {
    @Override
    public double calculate() {
      return playerperformances.stream()
          .filter(p -> p.getStats().isAhead())
          .mapToDouble(p -> p.getStats().isExtendingLead() ? 1 : 0)
          .average().orElse(0);
    }

    @Override
    public double average() {
      return Playerperformance.get().stream()
          .filter(p -> p.getStats().isAhead())
          .mapToDouble(p -> p.getStats().isExtendingLead() ? 1 : 0)
          .average().orElse(0);
    }

    @Override
    public double maximum() {
      return 1;
    }

    @Override
    public double minimum() {
      return 0;
    }
  }.nullable()
      .sub("Lane gesnowballt", p -> p.getStats().isExtendingLead() ? 1 : 0);

  //</editor-fold>
  //<editor-fold desc="Kategorie 3.5: STRONG_PHASE">
  public Stat highestLeadMinute = new Stat(playerperformances, OutputType.NUMBER, 2) {
    @Override
    public double calculate() {
      double leadAtMax = Double.MIN_VALUE;
      int leadAtMaxMinute = 0;
      for (int i = 1; i < 101; i++) {
        final int finalI = i;
        double leadAt = playerperformances.stream()
            .flatMap(playerperformance -> playerperformance.getInfos().stream())
            .filter(info -> info.getMinute() == finalI)
            .mapToInt(PlayerperformanceInfo::getLead)
            .average().orElse(0);
        if (leadAtMax < leadAt) {
          leadAtMax = leadAt;
          leadAtMaxMinute = i;
        }
      }
      return leadAtMaxMinute;
    }
  }.ignore();

  public Stat lowestLeadMinute = new Stat(playerperformances, OutputType.NUMBER, 2) {
    @Override
    public double calculate() {
      double leadAtMin = Double.MAX_VALUE;
      int leadAtMinMinute = 0;
      for (int i = 1; i < 101; i++) {
        final int finalI = i;
        double leadAt = playerperformances.stream()
            .flatMap(playerperformance -> playerperformance.getInfos().stream())
            .filter(info -> info.getMinute() == finalI)
            .mapToInt(PlayerperformanceInfo::getLead)
            .average().orElse(0);
        if (leadAtMin > leadAt) {
          leadAtMin = leadAt;
          leadAtMinMinute = i;
        }
      }
      return leadAtMinMinute;
    }
  }.ignore();

  public Stat allowComebacks = new Stat(playerperformances, OutputType.PERCENT, 2) {
    @Override
    public double calculate() {
      return playerperformances.stream()
          .filter(p -> p.getStats().isBehind())
          .mapToDouble(p -> p.getStats().isComeback() ? 1 : 0)
          .average().orElse(0) +
          playerperformances.stream()
              .filter(p -> p.getStats().isAhead())
              .mapToDouble(p -> p.getStats().isComeback() ? -1 : 0)
              .average().orElse(0);
    }

    @Override
    public double average() {
      return Playerperformance.get().stream()
          .filter(p -> p.getStats().isBehind())
          .mapToDouble(p -> p.getStats().isComeback() ? 1 : 0)
          .average().orElse(0) +
          Playerperformance.get().stream()
              .filter(p -> p.getStats().isAhead())
              .mapToDouble(p -> p.getStats().isComeback() ? -1 : 0)
              .average().orElse(0);
    }

    @Override
    public double maximum() {
      return 1;
    }

    @Override
    public double minimum() {
      return -1;
    }
  }.nullable()
      .sub("Vorsprung abgebaut", p -> p.getStats().isAhead() && p.getStats().isComeback() ? 1 : 0)
      .sub("Rückstand aufgeholt", p -> p.getStats().isBehind() && p.getStats().isComeback() ? 1 : 0);

  public Stat xpLead = new Stat(playerperformances, OutputType.NUMBER, 3)
      .map(p -> p.getStats().getXpLead())
      .nullable();
  //</editor-fold>

  //<editor-fold desc="Kategorie 4.1: TEAMFIGHTING">
  public Stat multikills = new Stat(playerperformances, OutputType.TEXT, 15) {
    @Override
    public String display() {
      return calculate() + " -> " + playerperformances.stream().mapToDouble(Playerperformance::getDoubleKills).average().orElse(0) + "-" +
          playerperformances.stream().mapToDouble(Playerperformance::getTripleKills).average().orElse(0) + "-" +
          playerperformances.stream().mapToDouble(Playerperformance::getQuadraKills).average().orElse(0) + "-" +
          playerperformances.stream().mapToDouble(Playerperformance::getPentaKills).average().orElse(0);
    }
  }.map(p -> p.getDoubleKills() + p.getTripleKills() * 2 + p.getQuadraKills() * 6 + p.getPentaKills() * 24)
      .nullable();

  //TODO (Abgie) 28.04.2022:
  public Stat deathOrder = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 28.04.2022:
  public Stat teamfightSuccessRate = new Stat(playerperformances, OutputType.PERCENT, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  public Stat acesEarlyAndCleanFights = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(p -> p.getStats().getAcesAndClean())
      .nullable()
      .sub("Earlygame Aces", p -> p.getTeamperformance().getEarlyAces())
      .sub("Flawless Aces", p -> p.getTeamperformance().getFlawlessAces());

  //TODO (Abgie) 28.04.2022:
  public Stat teamfightDamagePercentage = new Stat(playerperformances, OutputType.PERCENT, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //</editor-fold>
  //<editor-fold desc="Kategorie 4.2: SKIRMISHING">
  //TODO (Abgie) 28.04.2022:
  public Stat skirmishesAmount = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 28.04.2022:
  public Stat skirmishKillBilance = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 28.04.2022:
  public Stat skirmishSuccessRate = new Stat(playerperformances, OutputType.PERCENT, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 28.04.2022:
  public Stat damagePerSkirmish = new Stat(playerperformances, OutputType.NUMBER, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 28.04.2022:
  public Stat skirmishDamagePercentage = new Stat(playerperformances, OutputType.PERCENT, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //</editor-fold>
  //<editor-fold desc="Kategorie 4.3: EARLY_INCOME">
  public Stat earlyLaneLead = new Stat(playerperformances, OutputType.NUMBER, 3)
      .map(p -> p.getStats().getEarlyGoldAdvantage())
      .nullable();

  public Stat laneLead = new Stat(playerperformances, OutputType.NUMBER, 3)
      .map(Playerperformance::getLaneLead)
      .nullable();

  public Stat firstFullItem = new Stat(playerperformances, OutputType.TIME, 2)
      .map(p -> p.getStats().getFirstFullItem());

  public Stat earlyCreepScore = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getCreepsEarly);

  public Stat farmSupportitemEfficiency = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getStats().getEarlyFarmEfficiency())
      .nullable()
      .sub("CS@15", p -> p.getStats().getEarlyFarmEfficiency() * 158);

  //</editor-fold>
  //<editor-fold desc="Kategorie 4.4: INCOME">
  public Stat creepsPerMinute = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(p -> p.getStats().getCsPerMinute())
      .nullable()
      .sub("Total CS", Playerperformance::getCreepsTotal)
      .sub("Minuten", p -> p.getTeamperformance().getGame().getDuration() * 1d / 60);

  public Stat xpPerMinute = new Stat(playerperformances, OutputType.NUMBER, 3)
      .map(p -> p.getStats().getXpPerMinute())
      .nullable()
      .sub("Total XP", Playerperformance::getExperience)
      .sub("Minuten", p -> p.getTeamperformance().getGame().getDuration() * 1d / 60);

  public Stat goldPerMinute = new Stat(playerperformances, OutputType.NUMBER, 3)
      .map(p -> p.getStats().getGoldPerMinute())
      .nullable()
      .sub("Total Gold", Playerperformance::getGoldTotal)
      .sub("Minuten", p -> p.getTeamperformance().getGame().getDuration() * 1d / 60);

  public Stat creepAdvantage = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getFlamehorizonAdvantage)
      .nullable()
      .sub("Total CS", Playerperformance::getCreepsTotal);

  //TODO (Abgie) 28.04.2022:
  public Stat trueKDA = new Stat(playerperformances, OutputType.TEXT, 12) {
    @Override
    public double calculate() {
      return playerperformances.stream().mapToDouble(p -> p.getStats().getTrueKdaValue()).average().orElse(0);
    }

    @Override
    public String display() {
      return playerperformances.stream().mapToInt(Playerperformance::getKills).sum() + " / " +
          playerperformances.stream().mapToInt(Playerperformance::getDeaths).sum() + " / " +
          playerperformances.stream().mapToInt(Playerperformance::getAssists).sum() + " / ";
    }

    @Override
    public double average() {
      return (Playerperformance.get().stream().mapToInt(Playerperformance::getKills).sum() +
          Playerperformance.get().stream().mapToInt(Playerperformance::getAssists).sum()) * 1d /
          Playerperformance.get().stream().mapToInt(Playerperformance::getDeaths).sum();
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  //</editor-fold>
  //<editor-fold desc="Kategorie 4.5: ITEMIZATION">
  public Stat legendaryItems = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(p -> p.getStats().getLegendaryItems())
      .nullable();

  public Stat itemsBought = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getItemsAmount)
      .ignore();

  public Stat mejaisTime = new Stat(playerperformances, OutputType.TIME, 2)
      .map(Playerperformance::getMejaisCompleted)
      .reverse();

  public Stat grievousWoundsAndPenetrationTime = new Stat(playerperformances, OutputType.TIME, 2)
      .map(p -> p.getStats().getSituationalTime())
      .reverse()
      .sub("Grievous-Wounds", p -> p.getStats().getAntiHealTime())
      .sub("Penetration", p -> p.getStats().getPenetrationTime())
      .sub("Amplifier", p -> p.getStats().getAmplifierTime());

  public Stat startitemSold = new Stat(playerperformances, OutputType.TIME, 2)
      .map(p -> p.getStats().getStartItemSold());
  //</editor-fold>

  //<editor-fold desc="Kategorie 5.1: SURVIVAL">
  public Stat playtimeLive = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getStats().getTimeAlive())
      .sub("Todeszeit", Playerperformance::getTimeDead)
      .sub("Minuten", p -> p.getTeamperformance().getGame().getDuration() * 1d / 60);

  public Stat timeWithoutDying = new Stat(playerperformances, OutputType.TIME, 2)
      .map(Playerperformance::getTimeAlive);

  public Stat survivedClose = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getSurvivedClose);

  //TODO (Abgie) 29.04.2022:
  public Stat deathPositioning = new Stat(playerperformances, OutputType.PERCENT, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  //</editor-fold>
  //<editor-fold desc="Kategorie 5.2: EARLY_SURVIVAL">
  public Stat firstKillDeath = new Stat(playerperformances, OutputType.TIME, 2)
      .map(p -> p.getStats().getFirstKillDeathTime())
      .nullable()
      .sub("1. Kill", p -> p.getStats().getFirstKillTime())
      .sub("1. Death", p -> p.getStats().getFirstKillTime() - p.getStats().getFirstKillDeathTime());

  //TODO (Abgie) 29.04.2022:
  public Stat firstBaseThroughRecall = new Stat(playerperformances, OutputType.PERCENT, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 29.04.2022:
  public Stat laneLeadDeficitThroughDeaths = new Stat(playerperformances, OutputType.NUMBER, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 29.04.2022:
  public Stat laneLeadDeficitWithoutDeaths = new Stat(playerperformances, OutputType.NUMBER, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //</editor-fold>
  //<editor-fold desc="Kategorie 5.3: TEAM_UTILITY">
  public Stat damageShielded = new Stat(playerperformances, OutputType.NUMBER, 3)
      .map(Playerperformance::getDamageShielded)
      .nullable();

  public Stat crowdControl = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(Playerperformance::getImmobilizations)
      .nullable();

  public Stat enemiesControlled = new Stat(playerperformances, OutputType.NUMBER, 4) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat teammatesSaved = new Stat(playerperformances, OutputType.NUMBER, 3)
      .map(Playerperformance::getSavedAlly)
      .nullable();

  //TODO (Abgie) 29.04.2022:
  public Stat utilityScore = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  //</editor-fold>
  //<editor-fold desc="Kategorie 5.4: GANK_SURVIVAL_TIMEWASTING">
  //TODO (Abgie) 29.04.2022:
  public Stat jungleTimewaste = new Stat(playerperformances, OutputType.TIME, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  @Deprecated
  //TODO (Abgie) 29.04.2022:
  public Stat roamsScouted = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  //</editor-fold>
  //<editor-fold desc="Kategorie 5.5: ISOLATION">
  //TODO (Abgie) 29.04.2022: Isolationen
  public Stat minionEfficiency = new Stat(playerperformances, OutputType.PERCENT, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 29.04.2022: Isolationen
  public Stat isolationXPEfficiency = new Stat(playerperformances, OutputType.PERCENT, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 29.04.2022: Isolationen
  public Stat wardsUsed = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 29.04.2022: Isolationen
  public Stat damageTrading = new Stat(playerperformances, OutputType.NUMBER, 4) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 29.04.2022: Isolationen
  public Stat resetAmount = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //</editor-fold>

  //TODO (Abgie) 29.04.2022: Resets?? 15 TODOs
  //<editor-fold desc="Kategorie 6.1: PRE_FIRST_BASE">
  public Stat firstReset = new Stat(playerperformances, OutputType.TIME, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat preFirstBaseEnemyUnderControl = new Stat(playerperformances, OutputType.TIME, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat buffsAndScuttlesInitial = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat xpGoldLead = new Stat(playerperformances, OutputType.NUMBER, 4) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat goldOnReset = new Stat(playerperformances, OutputType.NUMBER, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  //</editor-fold>
  //<editor-fold desc="Kategorie 6.2: POST_FIRST_BASE">
  public Stat resourceUsage = new Stat(playerperformances, OutputType.NUMBER, 4) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat consumablesUsed = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat secondResetTime = new Stat(playerperformances, OutputType.TIME, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat postFirstBaseEnemyUnderControl = new Stat(playerperformances, OutputType.TIME, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat earlyDamagePercentage = new Stat(playerperformances, OutputType.PERCENT, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  //</editor-fold>
  //<editor-fold desc="Kategorie 6.3: LANE_BILANCE">
  public Stat earlyXPGoldLead = new Stat(playerperformances, OutputType.NUMBER, 4)
      .map(Playerperformance::getEarlyLaneLead)
      .nullable();

  public Stat laneObjectiveAdvantage = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(p -> p.getStats().getEarlyObjectiveAdvantage())
      .nullable()
      .sub("Objectives erhalten", p -> p.getStats().getEarlyObjectives())
      .sub("Objectives abgegeben", p -> p.getStats().getEarlyObjectives() - p.getStats().getEarlyObjectiveAdvantage());

  public Stat turretplateAdvantage = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(p -> p.getStats().getTurretplateAdvantage())
      .nullable()
      .sub("Turretplates zerstört", Playerperformance::getTurretplates)
      .sub("Turretplates verloren", p -> p.getTurretplates() - p.getStats().getTurretplateAdvantage());

  public Stat laneEnemyUnderControlAdvantage = new Stat(playerperformances, OutputType.TIME, 2)
      .map(p -> p.getStats().getEnemyControlAdvantage())
      .nullable()
      .sub("Gegner kontrolliert", p -> p.getStats().getEnemyControlled())
      .sub("Spieler kontrolliert", p -> p.getStats().getEnemyControlled() - p.getStats().getEnemyControlAdvantage());

  //</editor-fold>
  //<editor-fold desc="Kategorie 6.4: PLAYSTYLE">
  //TODO (Abgie) 29.04.2022:
  public Stat killsPosition = new Stat(playerperformances, OutputType.PERCENT, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  //TODO (Abgie) 29.04.2022:
  public Stat deathsPosition = new Stat(playerperformances, OutputType.PERCENT, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  public Stat keyspellsUsed = new Stat(playerperformances, OutputType.NUMBER, 2)
      .map(p -> p.getStats().getKeyspellsUsed())
      .nullable()
      .sub("Q genutzt", Playerperformance::getQUsages)
      .sub("W genutzt", Playerperformance::getWUsages)
      .sub("E genutzt", Playerperformance::getEUsages)
      .sub("R genutzt", Playerperformance::getRUsages);

  public Stat spellBilance = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getStats().getTotalSpellBilance())
      .nullable()
      .sub("Spell Bilanz", p -> p.getStats().getHitBilance())
      .sub("Dodge Bilanz", p -> p.getStats().getDodgeBilance())
      .sub("Spells getroffen",
          p -> (p.getSpellsHit() * -1 * p.getStats().getHitBilance() + p.getSpellsHit()) / p.getStats().getHitBilance())
      .sub("Spells gedodged",
          p -> (p.getSpellsDodged() * -1 * p.getStats().getDodgeBilance() + p.getSpellsDodged()) / p.getStats().getDodgeBilance());

  public Stat reactions = new Stat(playerperformances, OutputType.NUMBER, 3)
      .map(p -> p.getStats().getReactionBilance())
      .nullable()
      .sub("schnelle Reaktionen", Playerperformance::getQuickDodged)
      .sub("gegnerische Reaktionen", p -> p.getStats().getReactionBilance() - p.getQuickDodged());

  //</editor-fold>
  //<editor-fold desc="Kategorie 6.5: RESETS">
  public Stat resetsThroughDeaths = new Stat(playerperformances, OutputType.PERCENT, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat averageResetTime = new Stat(playerperformances, OutputType.TIME, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat averageResetGold = new Stat(playerperformances, OutputType.NUMBER, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat goldLostThroughResets = new Stat(playerperformances, OutputType.NUMBER, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat resetsWithTeam = new Stat(playerperformances, OutputType.PERCENT, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  //</editor-fold>

  //<editor-fold desc="Kategorie 7.1: GIVING_UP">
  public Stat ffRate = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getTeamperformance().isSurrendered() ? 1 : 0)
      .nullable();

  public Stat laneLeadAfterDiedEarly = new Stat(playerperformances, OutputType.NUMBER, 4)
      .map(p -> p.getStats().getLeadDifferenceAfterDiedEarly());

  public Stat farmingFromBehind = new Stat(playerperformances, OutputType.NUMBER, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat wardingFromBehind = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat deathsFromBehind = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  //</editor-fold>
  //<editor-fold desc="Kategorie 7.2: CONSISTENCY">
  public Stat earlyLevelupLead = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat farmAdvantageFromAhead = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat farmAdvantageFromBehind = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat xpAdvantageFromAhead = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  public Stat xpAdvantageFromBehind = new Stat(playerperformances, OutputType.NUMBER, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  //</editor-fold>
  //<editor-fold desc="Kategorie 7.3: VERSATILTITY">
  public Stat visionValue = new Stat(playerperformances, OutputType.NUMBER, 4) {

    @Override
    public double calculate() {
      return handleValues(objectiveAfterSpawn, stolenObjectivesAndContestRate, damageAgainstObjectives, scuttleControlOverall,
          junglerTakedownsBeforeObjective, baronTime, baronTakedownsAttempts, baronPowerPlay, heraldTurrets, heraldMulticharge,
          dragonTime, dragonTakedowns, elderTime, firstDrake, soulrateAndPerfect, visionScoreAdvantage, trinketEfficiency, firstWardTime,
          wardsCleared, trinketSwapTime, controlWardsPlaced, controlWardsProtected, controlWardsEnemyJungle, firstControlWardTime,
          averageControlWardTime);
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 10;
    }

    @Override
    public double minimum() {
      return -10;
    }
  }.nullable();

  public Stat roamingValue = new Stat(playerperformances, OutputType.NUMBER, 4) {

    @Override
    public double calculate() {
      return handleValues(firstTowerAdvantage, turretPlatings, turretTakedownsEarly, turretsSplitpushed, turretParticipation,
          teleportKills, jungleCampsStolen, midgameXPEfficiency, midgameGoldEfficiency, lateXPGoldLead, minionAvantagePerRoam, xpEfficiency, goldEfficiency, roamSuccess, objectiveDamageWhileRoaming, teamInvadesAndBuffsTaken, ganksEarlygame, proximity, gankPriority, gankSetups, divingSuccessrate, divingDisengagerate, divesDied);
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 10;
    }

    @Override
    public double minimum() {
      return -10;
    }
  }.nullable();

  public Stat aggressionValue = new Stat(playerperformances, OutputType.NUMBER, 4) {

    @Override
    public double calculate() {
      return handleValues(teamDamage, teamTankyness, teamDurability, healing, timeInCombat, aggressiveFlash, levelupAllins, soloKillDiff,
          outplays, firstBloodParticipation, bountyGotten, assassinations, picksMade, ambushes, duelWinrate, killsDeathsEarlygame,
          winsIfAhead, leadExtending, highestLeadMinute, lowestLeadMinute, allowComebacks, xpLead);
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 10;
    }

    @Override
    public double minimum() {
      return -10;
    }
  }.nullable();

  public Stat fightingValue = new Stat(playerperformances, OutputType.NUMBER, 4) {

    @Override
    public double calculate() {
      return handleValues(multikills, deathOrder, teamfightSuccessRate, acesEarlyAndCleanFights, teamfightDamagePercentage,
          skirmishesAmount, skirmishKillBilance, skirmishSuccessRate, damagePerSkirmish, skirmishDamagePercentage, earlyLaneLead,
          laneLead, firstFullItem, earlyCreepScore, farmSupportitemEfficiency, creepsPerMinute, xpPerMinute, goldPerMinute,
          creepAdvantage, trueKDA, legendaryItems, itemsBought, mejaisTime, grievousWoundsAndPenetrationTime, startitemSold);
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 10;
    }

    @Override
    public double minimum() {
      return -10;
    }
  }.nullable();

  public Stat survivabilityValue = new Stat(playerperformances, OutputType.NUMBER, 4) {

    @Override
    public double calculate() {
      return handleValues(playtimeLive, timeWithoutDying, survivedClose, deathPositioning, firstKillDeath, firstBaseThroughRecall,
          laneLeadDeficitThroughDeaths, laneLeadDeficitWithoutDeaths, damageShielded, crowdControl, enemiesControlled, teammatesSaved,
          utilityScore, jungleTimewaste, roamsScouted, minionEfficiency, isolationXPEfficiency, wardsUsed, damageTrading, resetAmount);
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 10;
    }

    @Override
    public double minimum() {
      return -10;
    }
  }.nullable();
  //</editor-fold>
  //<editor-fold desc="Kategorie 7.4: ADAPTION">
  public Stat antiHealing = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getStats().getAntiHealTime() != 0 ? 1 : 0)
      .nullable();

  public Stat penetration = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getStats().getPenetrationTime() != 0 ? 1 : 0)
      .nullable();

  public Stat damageBuild = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getStats().getAmplifierTime() != 0 ? 1 : 0)
      .nullable();

  public Stat resistanceBuild = new Stat(playerperformances, OutputType.PERCENT, 3) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };

  public Stat farmstop = new Stat(playerperformances, OutputType.TIME, 2) {

    @Override
    public double calculate() {
      return 0;
    }

    @Override
    public String display() {
      return null;
    }

    @Override
    public double average() {
      return 0;
    }

    @Override
    public double maximum() {
      return 0;
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  //</editor-fold>
  //<editor-fold desc="Kategorie 7.5: STATS">
  public Stat winrate = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getTeamperformance().isWin() ? 1 : 0)
      .nullable();

  public Stat killParticipation = new Stat(playerperformances, OutputType.PERCENT, 3)
      .map(p -> p.getStats().getKillParticipation())
      .nullable();

  public Stat blueWinrate = new Stat(playerperformances, OutputType.PERCENT, 3) {
    @Override
    public double calculate() {
      return playerperformances.stream()
          .filter(p -> p.getTeamperformance().isFirstPick())
          .mapToDouble(p -> p.getTeamperformance().isWin() ? 1 : 0).average().orElse(0);
    }

    @Override
    public double average() {
      return .5;
    }

    @Override
    public double maximum() {
      return 1;
    }

    @Override
    public double minimum() {
      return 0;
    }
  }.nullable();

  public Stat redWinrate = new Stat(playerperformances, OutputType.PERCENT, 3) {
    @Override
    public double calculate() {
      return playerperformances.stream()
          .filter(p -> !p.getTeamperformance().isFirstPick())
          .mapToDouble(p -> p.getTeamperformance().isWin() ? 1 : 0).average().orElse(0);
    }

    @Override
    public double average() {
      return .5;
    }

    @Override
    public double maximum() {
      return 1;
    }

    @Override
    public double minimum() {
      return 0;
    }
  }.nullable();

  public Stat kDA = new Stat(playerperformances, OutputType.TEXT, 12) {
    @Override
    public double calculate() {
      return (playerperformances.stream().mapToInt(Playerperformance::getKills).sum() +
          playerperformances.stream().mapToInt(Playerperformance::getAssists).sum()) * 1d /
          playerperformances.stream().mapToInt(Playerperformance::getDeaths).sum();
    }

    @Override
    public String display() {
      return playerperformances.stream().mapToInt(Playerperformance::getKills).sum() + " / " +
          playerperformances.stream().mapToInt(Playerperformance::getDeaths).sum() + " / " +
          playerperformances.stream().mapToInt(Playerperformance::getAssists).sum() + " / ";
    }

    @Override
    public double average() {
      return (Playerperformance.get().stream().mapToInt(Playerperformance::getKills).sum() +
          Playerperformance.get().stream().mapToInt(Playerperformance::getAssists).sum()) * 1d /
          Playerperformance.get().stream().mapToInt(Playerperformance::getDeaths).sum();
    }

    @Override
    public double maximum() {
      return (Playerperformance.get().stream().mapToInt(Playerperformance::getKills).max().orElse(0) +
          Playerperformance.get().stream().mapToInt(Playerperformance::getAssists).max().orElse(0)) * 1d /
          Playerperformance.get().stream().mapToInt(Playerperformance::getDeaths).max().orElse(0);
    }

    @Override
    public double minimum() {
      return 0;
    }
  };
  //</editor-fold>
}