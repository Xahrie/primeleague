package de.xeri.league.util.io.riot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.xeri.league.models.dynamic.Champion;
import de.xeri.league.models.dynamic.Item;
import de.xeri.league.models.dynamic.Rune;
import de.xeri.league.models.dynamic.Summonerspell;
import de.xeri.league.models.enums.DragonSoul;
import de.xeri.league.models.enums.EventTypes;
import de.xeri.league.models.enums.ItemType;
import de.xeri.league.models.enums.KillRole;
import de.xeri.league.models.enums.KillType;
import de.xeri.league.models.enums.Lane;
import de.xeri.league.models.enums.ObjectiveSubtype;
import de.xeri.league.models.enums.QueueType;
import de.xeri.league.models.enums.SelectionType;
import de.xeri.league.models.enums.StoredStat;
import de.xeri.league.models.enums.WardType;
import de.xeri.league.models.league.Account;
import de.xeri.league.models.match.ChampionSelection;
import de.xeri.league.models.match.Game;
import de.xeri.league.models.match.GamePause;
import de.xeri.league.models.match.Gametype;
import de.xeri.league.models.match.Playerperformance;
import de.xeri.league.models.match.PlayerperformanceInfo;
import de.xeri.league.models.match.PlayerperformanceKill;
import de.xeri.league.models.match.PlayerperformanceLevel;
import de.xeri.league.models.match.PlayerperformanceObjective;
import de.xeri.league.models.match.PlayerperformanceStats;
import de.xeri.league.models.match.ScheduledGame;
import de.xeri.league.models.match.Teamperformance;
import de.xeri.league.models.match.TeamperformanceBounty;
import de.xeri.league.models.match.location.Position;
import de.xeri.league.models.others.Kill;
import de.xeri.league.others.Duel;
import de.xeri.league.others.Fight;
import de.xeri.league.others.Gank;
import de.xeri.league.others.Pick;
import de.xeri.league.others.Skirmish;
import de.xeri.league.others.Teamfight;
import de.xeri.league.others.enums.Fighttype;
import de.xeri.league.util.Const;
import de.xeri.league.util.Data;
import de.xeri.league.util.Util;
import de.xeri.league.util.io.json.JSON;
import lombok.val;
import lombok.var;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Lara on 08.04.2022 for web
 */
public final class RiotGameRequester {
  private static List<JSONTeam> jsonTeams;

  private static List<JSONPlayer> getJSONPlayers() {
    return jsonTeams.stream()
        .flatMap(jsonTeam -> jsonTeam.getAllPlayers().stream())
        .collect(Collectors.toList());
  }

  private static List<JSONPlayer> findJSONPlayersExcept(String value, JSONPlayer jsonPlayer) {
    return findJSONPlayersWith(value).stream()
        .filter(player -> player.getId() != jsonPlayer.getId())
        .collect(Collectors.toList());
  }

  private static List<JSONPlayer> findJSONPlayersWith(String value) {
    return getJSONPlayers().stream()
        .filter(player -> player.get(StoredStat.LANE).equals(value))
        .collect(Collectors.toList());
  }

  public static void loadCompetitive(ScheduledGame scheduledGame) {
    loadGame(scheduledGame, QueueType.TOURNEY);
  }

  public static void loadClashGame(ScheduledGame scheduledGame) {
    loadGame(scheduledGame, QueueType.CLASH);
  }

  public static void loadMatchmade(ScheduledGame scheduledGame) {
    loadGame(scheduledGame, QueueType.OTHER);
  }

  private static void loadGame(ScheduledGame scheduledGame, QueueType queueType) {
    val matchGenerator = RiotURLGenerator.getMatch();
    val game = matchGenerator.getMatch(scheduledGame.getId());
    val timeline = matchGenerator.getTimeline(scheduledGame.getId());
    if (isValidGame(game, timeline, queueType))
      ScheduledGame.get().remove(scheduledGame);
    Data.getInstance().getSession().remove(scheduledGame);
    Data.getInstance().commit();
  }

  private static boolean isValidGame(JSON gameJson, JSON timelineJson, QueueType queueType) {
    val gameData = gameJson.getJSONObject();
    val info = gameData.getJSONObject("info");
    final int queueId = info.getInt("queueId");
    val participants = info.getJSONArray("participants");
    if ((queueId == 0 || queueId == 400 || queueId == 410 || queueId == 420 || queueId == 430 || queueId == 700) && participants.length() == 10) {

      val metadata = gameData.getJSONObject("metadata");
      val gameId = metadata.getString("matchId");
      val events = new ArrayList<JSONObject>();
      val playerInfo = new HashMap<Integer, JSONObject>();
      if (timelineJson.getJSONObject() != null) {
        loadTimeline(events, playerInfo, timelineJson.getJSONObject());
      }

      val gametype = Gametype.find((info.has("tournamentCode") && !info.isNull("tournamentCode")) ? (short) -1 : (short) queueId);
      val game = handleGame(info, gameId, gametype);
      gametype.addGame(game, gametype);
      val fights = handleGameEvents(events, game);

      jsonTeams = getJsonTeams(participants);
      for (int i = 0; i < jsonTeams.size(); i++) {
        val jsonTeam = jsonTeams.get(i);
        val teams = info.getJSONArray("teams");
        jsonTeam.setTeamObject(teams.getJSONObject(i));
        if (jsonTeam.doesExist()) {
          val teamperformance = handleTeam(jsonTeam);
          val team = jsonTeam.getMostUsedTeam(queueType);
          game.addTeamperformance(teamperformance, team);
          handleTeamEvents(events, teamperformance);

          val players = determinePlayers(queueType, jsonTeam);
          players.forEach(player -> handlePlayer(player, teamperformance, events, playerInfo, fights));
        }
        determineBansAndPicks(teams.getJSONObject(i), i, game, participants);
      }
      return true;
    }
    return false;
  }

  private static void determineBansAndPicks(JSONObject jsonObject, int id, Game game, JSONArray participants) {
    val bans = jsonObject.getJSONArray("bans");
    val pickTurns = new ArrayList<Integer>();
    for (int j = 0; j < bans.length(); j++) {
      val selectionObject = bans.getJSONObject(j);
      final int championId = selectionObject.getInt("championId");
      val champion = Champion.find(championId);
      final int pickTurn = selectionObject.getInt("pickTurn");
      pickTurns.add(pickTurn);
      game.addChampionSelection(new ChampionSelection(SelectionType.BAN, (byte) (j + 1 + id * 5)), champion);
    }

    val indexes = new ArrayList<Integer>();
    int iterator = 1;
    while (!pickTurns.isEmpty()) {
      final int lowestIndex = pickTurns.indexOf(Collections.min(pickTurns));
      indexes.set(lowestIndex, iterator);
      pickTurns.remove(lowestIndex);
      iterator++;
    }

    for (int i : indexes) {
      val championName = participants.getJSONObject(i).getString("championName");
      val champion = Champion.find(championName);
      game.addChampionSelection(new ChampionSelection(SelectionType.PICK, (byte) (i + 1 + id * 5)), champion);
    }

  }

  private static void handlePlayer(JSONPlayer player, Teamperformance teamperformance, List<JSONObject> events,
                                   Map<Integer, JSONObject> playerInfo, List<Fight> fights) {
    val enemyPlayer = getEnemyPlayer(player);
    val performance = handlePerformance(player, enemyPlayer);
    val account = (player.isListed()) ? player.getAccount() : Account.get(RiotAccountRequester.fromPuuid(player.get(StoredStat.PUUID)));
    if (account != null) {
      val playerperformance = teamperformance.addPlayerperformance(performance, account);
      handleSummonerspells(player, playerperformance);
      handleChampionsPicked(player, enemyPlayer, playerperformance);

      val styles = player.object(StoredStat.RUNES).getJSONArray("styles");
      for (int i = 0; i < styles.length(); i++) {
        val substyle = styles.getJSONObject(i);
        val runes = substyle.getJSONArray("selections");
        for (int j = 0; j < runes.length(); j++) {
          val runeObject = runes.getJSONObject(j);
          val perk = Rune.find((short) runeObject.getInt("perk"));
          playerperformance.addRune(perk);
        }
      }

      handlePlayerEvents(events, player, playerperformance);
      handlePlayerInfo(playerInfo, player, playerperformance);

      handlePlayerStats(playerperformance, teamperformance, jsonTeams, events, playerInfo, player, fights);
    }
  }

  private static void handlePlayerStats(Playerperformance playerperformance, Teamperformance teamperformance, List<JSONTeam> jsonTeams,
                                        List<JSONObject> events, Map<Integer, JSONObject> playerInfo, JSONPlayer player, List<Fight> fights) {
    final int pId = player.getId() + 1;
    val stats = new PlayerperformanceStats(playerperformance);
    final JSONPlayer enemyPlayer = getEnemyPlayer(player);

    val jsonTeam = jsonTeams.get(teamperformance.isFirstPick() ? 0 : 1);
    val enemyTeam = jsonTeams.get(teamperformance.isFirstPick() ? 1 : 0);
    byte allObjectivesAmount = determineAllObjectivesAmount(jsonTeams);
    stats.setObjectivesStolenAndContested(playerperformance, allObjectivesAmount);
    stats.setObjectivesKilledJunglerBefore(playerperformance, allObjectivesAmount);
    byte stolenBarons = determineStolenBarons(events, jsonTeam, enemyTeam);
    stats.setBaronTakedownsAttempts(playerperformance, stolenBarons);

    short firstControlWardTime = 0;
    final short firstWardTime = searchForFirstWardTime(events, pId, firstControlWardTime);
    stats.setFirstWardTime(firstWardTime);
    stats.setFirstControlwardTime(firstControlWardTime);

    final short firstTrinketSwap = searchForTrinketSwap(events, pId);
    stats.setFirstTrinketSwap(firstTrinketSwap);

    final List<Integer> yellowPlacementTimes = searchForTrinketPlacementsUntilSwap(events, pId, firstTrinketSwap);
    final int twoChargesUp = searchForRechargeTimes(playerperformance, firstTrinketSwap, yellowPlacementTimes);
    final double twoChargesUpPercentage = (twoChargesUp - 240_000) * 1d / (firstTrinketSwap * 1000);
    stats.setTrinketEfficiency(BigDecimal.valueOf(1 - twoChargesUpPercentage));

    val purchases = searchForControlPurchase(events, pId);
    val placements = searchForControlPlacements(events, pId);
    final short averageControlTime = (short) IntStream.range(0, placements.size())
        .filter(i -> purchases.size() > i)
        .map(i -> placements.get(i) - purchases.get(i))
        .boxed().collect(Collectors.toCollection(ArrayList::new))
        .stream().mapToInt(Integer::intValue).average()
        .orElse(0);
    stats.setControlWardInventoryTime(averageControlTime);

    final int totalMitigation = jsonTeam.getAllPlayers().stream()
        .mapToInt(p -> p.getMedium(StoredStat.DAMAGE_MITIGATED))
        .sum();
    stats.setTeamDamageMitigated(playerperformance, totalMitigation);

    boolean wasAhead = false;
    boolean wasBehind = false;
    int behindStartMillis = 0;
    int behindEndMillis = 0;
    boolean comeback = false;
    int endMinute = 0;
    int xpLead = 0;
    for (int i = 0; i < playerInfo.size(); i++) {
      final int infoMilli = new ArrayList<>(playerInfo.keySet()).get(i);
      endMinute = infoMilli / 60000;
      if (endMinute <= 15) {
        if (getLeadAt(playerInfo, player, endMinute) >= Const.AHEAD_XPGOLD && !wasAhead) {
          wasAhead = true;
          behindStartMillis = infoMilli;
        } else if (getLeadAt(playerInfo, player, endMinute) <= (Const.AHEAD_XPGOLD * -1) && !wasBehind) {
          wasBehind = true;
          behindStartMillis = infoMilli;
        }
      }
      if (wasAhead && getLeadAt(playerInfo, player, endMinute) < 0 ||
          wasBehind && getLeadAt(playerInfo, player, endMinute) > 0) {
        behindEndMillis = infoMilli;
        comeback = true;
      }

      xpLead = getXPLeadAt(playerInfo, player, endMinute);
    }

    if (behindStartMillis != 0 && behindEndMillis == 0) {
      behindEndMillis = endMinute * 60_000;
    }
    stats.setAhead(wasAhead);
    stats.setBehind(wasBehind);
    stats.setComeback(comeback);
    stats.setXpLead((short) xpLead);


    int deathsFromBehind = 0;
    short bountyDrop = 0;
    int deathsEarly = 0;
    int firstKillTime = 0;
    int firstDeathTime = 0;

    val killBounties = new ArrayList<Short>();
    val assistBounties = new ArrayList<Short>();

    for (JSONObject event : events) {
      if (event.has("victimId")) {
        val type = EventTypes.valueOf(event.getString("type"));
        if (type.equals(EventTypes.CHAMPION_KILL)) {
          final short shutdownBounty = (short) event.getInt("shutdownBounty");
          final short bounty = (short) event.getInt("bounty");
          final short totalBounty = (short) (bounty + shutdownBounty);

          if (enemyPlayer != null && isParticipant(enemyPlayer.getId() + 1, event, KillRole.VICTIM)) {
            int timestamp = event.getInt("timestamp");
            // from ahead
            if (timestamp > behindStartMillis && timestamp < behindEndMillis) {
              deathsFromBehind--;
            }
          }

          if (isParticipant(pId, event, KillRole.VICTIM)) {
            //handle First Death time
            if (firstDeathTime == 0) {
              int timestamp = event.getInt("timestamp");
              firstDeathTime = timestamp / 1000;
            }

            if (wasKilledByPlayer(event)) {
              int timestamp = event.getInt("timestamp");
              if (timestamp / 60_000 <= 14) {
                deathsEarly++;
              }

              // Bounties
              killBounties.add((short) (totalBounty * -1));
              bountyDrop += shutdownBounty;

              // from behind
              if (timestamp > behindStartMillis && timestamp < behindEndMillis) {
                deathsFromBehind++;
              }
            }

          } else if (isParticipant(pId, event, KillRole.KILLER)) {
            //handle First Kill time
            if (firstKillTime == 0) {
              int timestamp = event.getInt("timestamp");
              firstKillTime = timestamp / 1000;
            }

            // Bounties
            killBounties.add(totalBounty);

          } else if (isParticipant(pId, event, KillRole.ASSIST) && wasKilledByPlayer(event)) {
            // Bounties
            final int timestamp = event.getInt("timestamp");
            final double factor = determineAssistbountyFactor(timestamp);
            final int totalAssistBounty = bounty == Const.KILL_BOUNTY_FIRST_BLOOD ? Const.ASSIST_BOUNTY_FIRST_BLOOD :
                (int) (bounty * factor);
            final int participantAmount = event.getJSONArray("assistingParticipantIds").toList().size();
            final int assistBounty = totalAssistBounty / participantAmount;
            assistBounties.add((short) assistBounty);
          }
        }
      }

    }

    if (firstDeathTime < 300) {
      int deathMinute = firstDeathTime / 60;
      final int leadAt = getLeadAt(playerInfo, player, deathMinute);
      stats.setLeadDifferenceAfterDiedEarly((short) (getLeadAt(playerInfo, player, 15) - leadAt));
    }

    handleFromBehind(events, playerInfo, player, pId, stats, enemyPlayer, behindStartMillis, behindEndMillis, deathsFromBehind);

    stats.setFirstKillTime((short) firstKillTime, (short) firstDeathTime);


    short startItemSold = determineStartItem(events);
    stats.setStartItemSold(startItemSold);


    final double kills = killBounties.stream().filter(b -> b > 0).mapToInt(b -> b).sum() * 1d / Const.KILL_BOUNTY;
    final double deaths = killBounties.stream().filter(b -> b < 0).mapToInt(b -> b).sum() * 1d / Const.KILL_BOUNTY;
    final double assists = assistBounties.stream().mapToInt(b -> b).sum() * 1d / Const.ASSIST_BOUNTY;
    stats.setTrueKda(kills, deaths, assists);


    byte objectivesEarlyWe = 0;
    byte objectivesEarlyEnemy = 0;
    for (JSONObject event : events) {
      val type = EventTypes.valueOf(event.getString("type"));
      if (type.equals(EventTypes.ELITE_MONSTER_KILL) || type.equals(EventTypes.BUILDING_KILL)) {
        int timestamp = event.getInt("timestamp");
        if (timestamp <= 14 * 60_000) {
          final int killerTeamId = event.has("killerTeamId") ? event.getInt("killerTeamId") : event.getInt("teamId");
          if (killerTeamId == jsonTeam.getId() * 100) {
            objectivesEarlyWe++;
          } else {
            objectivesEarlyEnemy++;
          }
        }
      }
    }
    stats.setEarlyObjectiveRate(objectivesEarlyWe, objectivesEarlyEnemy);


    byte turretPlatesWe = 0;
    byte turretPlatesEnemy = 0;
    for (JSONObject event : events) {
      val type = EventTypes.valueOf(event.getString("type"));
      if (type.equals(EventTypes.TURRET_PLATE_DESTROYED) && playerperformance.getLane() != null) {
        final int killerTeamId = event.getInt("teamId");
        val laneString = event.getString("laneType");
        var laneType = playerperformance.getLane().getType();
        if (laneString.equals(laneType)) {
          if (killerTeamId == jsonTeam.getId() * 100) {
            turretPlatesWe++;
          } else {
            turretPlatesEnemy++;
          }
        }

      }
    }

    playerperformance.setTurretplates(turretPlatesWe);
    stats.setTurretplateAdvantage((byte) (turretPlatesWe - turretPlatesEnemy));


    val map = new TreeMap<>(playerInfo);
    val lastEntry = map.lastEntry();
    final int minute = lastEntry.getKey() / 60000;
    final int leadAt = getLeadAt(playerInfo, player, minute - 3);
    final boolean leadExtend = wasAhead && (leadAt - Const.AHEAD_XPGOLD > 1000);
    stats.setExtendingLead(leadExtend);
    stats.setDeathsEarly(playerperformance, (byte) deathsEarly);
    stats.setBountyDifference(playerperformance, bountyDrop);

    double csAt10 = 0;
    for (PlayerperformanceInfo info : playerperformance.getInfos()) {
      if (info.getMinute() == 10) {
        final int csAt = getCSAt(playerInfo, player, info.getMinute());
        csAt10 = csAt * 1d / info.getMinute();
      }
      if (info.getMinute() > 10) {
        final int csAt = getCSAt(playerInfo, player, info.getMinute());
        final double csPerMinute = csAt * 1d / minute;
        if (csAt10 > 0 && csPerMinute < csAt10 * 0.8) {
          stats.setCsDropAtMinute(info.getMinute());
          break;
        }
      }
    }

    final double csAt = getCSAt(playerInfo, player, 14) * 1d / 158;
    stats.setEarlyFarmEfficiency(BigDecimal.valueOf(csAt));
    stats.setEarlyGoldAdvantage((short) getGoldLeadAt(playerInfo, player, 10));

    getMidgameStats(playerInfo, player, stats, endMinute);
    handleControlled(playerInfo, player, minute, stats);

    if (enemyPlayer != null) {
      byte earlierLevelups = 0;
      final byte totalLevelups = determineLevelups(playerperformance, events, enemyPlayer, earlierLevelups);
      if (totalLevelups != 0) { // null division
        final double earlierLevelupsAdvantage = earlierLevelups * 1d / totalLevelups;
        stats.setLevelupEarlier(BigDecimal.valueOf(earlierLevelupsAdvantage));
      }
    }

    stats.setSpellDodge(playerperformance, enemyPlayer.getSmall(StoredStat.SPELL_LANDED), enemyPlayer.getSmall(StoredStat.SPELL_DODGE),
        enemyPlayer.getSmall(StoredStat.SPELL_DODGE_QUICK));

    val myGanks = new ArrayList<Gank>();
    val enemyGanks = new ArrayList<Gank>();
    int duelsWon = 0;
    int duelsLost = 0;
    short pickAdvantage = 0;
    val teamfights = new ArrayList<Teamfight>();
    val skirmishes = new ArrayList<Skirmish>();
    for (Fight fight : fights) {
      if (playerperformance.getLane() != null) {
        if (fight.isGankOf(playerperformance.getLane(), player, enemyPlayer)) {
          myGanks.add(new Gank(player, playerperformance, fight));
        }
        if (fight.isGankOf(playerperformance.getLane(), enemyPlayer, player)) {
          enemyGanks.add(new Gank(player, playerperformance, fight));
        }
      }

      if (fight instanceof Duel) {
        if (fight.getKills().get(0).getKiller() == pId) {
          duelsWon++;

        } else if (fight.getKills().get(0).getVictim() == pId) {
          duelsLost++;
        }

      } else if (fight instanceof Pick) {
        if (fight.getKills().get(0).getVictim() == pId) {
          pickAdvantage--;
        } else {
          pickAdvantage++;
        }

      } else if (fight instanceof Teamfight) {
        val teamfight = (Teamfight) fight;
        teamfights.add(teamfight);

      } else if (fight instanceof Skirmish) {
        val skirmish = (Skirmish) fight;
        skirmishes.add(skirmish);
      }
    }

    double deathOrder = teamfights.stream().filter(teamfight -> teamfight.getDeathOrder(pId) != 0)
        .mapToDouble(teamfight -> teamfight.getDeathOrder(pId)).average().orElse(0);
    stats.setAverageDeathOrder(BigDecimal.valueOf(deathOrder));

    double teamfightWins = teamfights.stream().mapToInt(teamfight -> teamfight.isWinner(pId) ? 1 : 0).average().orElse(0);
    stats.setTeamfightWinrate(BigDecimal.valueOf(teamfightWins));

    final int teamfightDamage = teamfights.stream().mapToInt(teamfight -> teamfight.getFightDamage(playerperformance, player)).sum();
    double teamfightDamagePercentage = teamfightDamage * 1d / playerperformance.getDamageTotal();
    stats.setTeamfightDamageRate(BigDecimal.valueOf(teamfightDamagePercentage));

    final int skirmishAmount = skirmishes.size();
    stats.setSkirmishesAmount((short) skirmishAmount);

    int skirmishKills = (int) skirmishes.stream()
        .flatMap(skirmish -> skirmish.getKills().stream())
        .filter(kill -> kill.getKiller() == pId).count();
    stats.setSkirmishKillsPerSkirmish(BigDecimal.valueOf(skirmishKills * 1d / skirmishAmount));

    final double skirmishWins = teamfights.stream().mapToInt(skirmish -> skirmish.isWinner(pId) ? 1 : 0).average().orElse(0);
    stats.setSkirmishWinrate(BigDecimal.valueOf(skirmishWins));

    final int skrimishDamage = skirmishes.stream().mapToInt(skirmish -> skirmish.getFightDamage(playerperformance, player)).sum();
    double skirmishDamagePercentage = skrimishDamage * 1d / playerperformance.getDamageTotal();
    stats.setSkirmishDamageRate(BigDecimal.valueOf(skirmishDamagePercentage));


    stats.setDuels(playerperformance, duelsWon, duelsLost);
    stats.setPickAdvantage(pickAdvantage);

    int roamSuccess = 0;
    int gold = 0;
    int xp = 0;
    int cs = 0;
    int plates = 0;
    for (Gank gank : myGanks) {
      final List<Integer> involvedPlayers = gank.getFight().getInvolvedPlayers();
      final int start = gank.start();
      int end = gank.end() + 60_000;

      final Predicate<Integer> integerPredicate = player.getId() < 5 ? id -> id < 6 : id -> id > 5;
      final List<Integer> teamPlayers = involvedPlayers.stream().filter(integerPredicate).collect(Collectors.toList());
      for (Integer teamPlayer : teamPlayers) {
        JSONPlayer searchedPlayer = jsonTeams.get(0).getPlayer(teamPlayer, jsonTeams.get(1));
        xp += getXPLeadAt(playerInfo, searchedPlayer, end / 60_000) -
            getXPLeadAt(playerInfo, searchedPlayer, start / 60_000);
        gold += getGoldLeadAt(playerInfo, searchedPlayer, end / 60_000) -
            getGoldLeadAt(playerInfo, searchedPlayer, start / 60_000);
        cs += getCSLeadAt(playerInfo, searchedPlayer, end / 60_000) -
            getCSLeadAt(playerInfo, searchedPlayer, start / 60_000);
        roamSuccess += getLeadAt(playerInfo, player, end / 60_000) -
            getLeadAt(playerInfo, player, start / 60_000);
      }

      for (JSONObject event : events) {
        final int timestamp = event.getInt("timestamp");
        if (timestamp < end && timestamp > start) {
          final EventTypes type = EventTypes.valueOf(event.getString("type"));
          if (type.equals(EventTypes.TURRET_PLATE_DESTROYED)) {
            final JSONObject positionObject = event.getJSONObject("position");
            final Position position = new Position(positionObject.getInt("x"), positionObject.getInt("y"));
            if (Util.distance(gank.getFight().getLastPosition(), position) < Const.DISTANCE_BETWEEN_FIGHTS ||
                event.getString("laneType").equals(playerperformance.getLane().getType())) {
              final int teamId = event.getInt("teamId");
              if (player.getId() < 5) {
                plates += teamId == 100 ? 1 : -1;
              } else {
                plates += teamId == 200 ? 1 : -1;
              }
            }
          }
        }
      }
    }

    stats.setRoamObjectiveDamageAdvantage((short) (plates * 200));
    stats.setRoamXPAdvantage((short) xp);
    stats.setRoamCreepScoreAdvantage((short) cs);
    stats.setRoamGoldAdvantage((short) gold);
    stats.setRoamSuccessScore((short) roamSuccess);


    playerperformance.setLaneLead((short) getLeadAt(playerInfo, player, 15));
    playerperformance.setEarlyLaneLead((short) getLeadAt(playerInfo, player, 10));


    playerperformance.setStats(stats);
  }

  private static byte determineLevelups(Playerperformance playerperformance, List<JSONObject> events, JSONPlayer enemyPlayer,
                                        byte earlierLevelups) {
    byte totalLevelups = 0;
    for (JSONObject event : events) {
      val type = EventTypes.valueOf(event.getString("type"));

      if (type.equals(EventTypes.LEVEL_UP)) {
        final int participantId = event.getInt("participantId");
        if (participantId == enemyPlayer.getId() + 1) {
          int level = event.getInt("level");

          final int levelupTime = getLevelupTime(playerperformance, level);
          if (levelupTime != 0) { // byte not nullable
            int timestamp = event.getInt("timestamp");
            if (levelupTime < timestamp) {
              earlierLevelups++;
            }
            totalLevelups++;
          }
        }
      }
    }
    return totalLevelups;
  }

  private static void handleFromBehind(List<JSONObject> events, Map<Integer, JSONObject> playerInfo, JSONPlayer player, int pId,
                                       PlayerperformanceStats stats, JSONPlayer enemyPlayer, int behindStartMillis, int behindEndMillis,
                                       int deathsFromBehind) {
    int wardsFromBehind = 0;
    for (JSONObject event : events) {
      val type = EventTypes.valueOf(event.getString("type"));
      if (type.equals(EventTypes.WARD_PLACED)) {
        int timestamp = event.getInt("timestamp");
        if (timestamp > behindStartMillis && timestamp < behindEndMillis) {
          final int creatorId = event.getInt("creatorId");
          if (pId == creatorId) {
            wardsFromBehind++;

          } else if (enemyPlayer != null && enemyPlayer.getId() + 1 == creatorId) {
            wardsFromBehind--;
          }
        }
      }
    }

    final int csLeadAtStart = getCSLeadAt(playerInfo, player, behindStartMillis / 60_000);
    final int csLeadAtEnd = getCSLeadAt(playerInfo, player, behindEndMillis / 60_000);
    final int creepScoreFromBehind = csLeadAtEnd - csLeadAtStart;

    final int goldLeadAtStart = getGoldLeadAt(playerInfo, player, behindStartMillis / 60_000);
    final int goldLeadAtEnd = getGoldLeadAt(playerInfo, player, behindEndMillis / 60_000);
    final int goldFromBehind = goldLeadAtEnd - goldLeadAtStart;

    final int xpLeadAtStart = getXPLeadAt(playerInfo, player, behindStartMillis / 60_000);
    final int xpLeadAtEnd = getXPLeadAt(playerInfo, player, behindEndMillis / 60_000);
    final int xpFromBehind = xpLeadAtEnd - xpLeadAtStart;

    stats.setBehaviourFromBehindAhead((short) creepScoreFromBehind, (short) wardsFromBehind, (short) deathsFromBehind, (short) goldFromBehind,
        (short) xpFromBehind);
  }

  private static int searchForRechargeTimes(Playerperformance playerperformance, short firstTrinketSwap, List<Integer> yellowPlacementTimes) {
    int twoTrinkets = 0;
    val yellows = new ArrayList<>(yellowPlacementTimes);
    int lastWardCharged = 0;
    int chargedTime = 0;
    int currentAmount = 0;
    int currentMilli = 0;
    int currentLevel = 1;
    boolean wasNextLevel = false;
    boolean wasNextWard = false;
    boolean wasNextCharge = true;
    while (currentMilli < firstTrinketSwap * 1000) {
      if (wasNextCharge) {
        currentAmount++;
        lastWardCharged = currentMilli;
        wasNextCharge = false;

      } else if (wasNextLevel) {
        currentLevel++;
        if (chargedTime - lastWardCharged > getRechargeTimeAtLevel(currentLevel)) {
          chargedTime = lastWardCharged + getRechargeTimeAtLevel(currentLevel);
        }
        wasNextLevel = false;

      } else if (wasNextWard && !yellows.isEmpty()) {
        if (!yellows.isEmpty()) {
          yellows.remove(0);
        }
        currentAmount--;
        wasNextWard = false;
      }

      int nextLevel = getLevelupTime(playerperformance, currentLevel + 1);
      int nextWardPlaced = yellows.isEmpty() ? Integer.MAX_VALUE : yellows.get(0);
      if (nextLevel > nextWardPlaced && nextLevel > yellows.get(chargedTime)) {
        wasNextLevel = true;
      } else if (nextWardPlaced > nextLevel && nextWardPlaced > yellows.get(chargedTime)) {
        wasNextWard = true;
      } else if (currentAmount < 2) {
        wasNextCharge = true;
      }
      int min = Math.min(Math.min(nextLevel, nextWardPlaced), chargedTime);
      if (currentAmount == 2) {
        twoTrinkets += min - currentMilli;
      }
      currentMilli = min;
    }

    return twoTrinkets / 1000;
  }

  private static void getMidgameStats(Map<Integer, JSONObject> playerInfo, JSONPlayer player, PlayerperformanceStats stats, int endMinute) {
    val infoAt14 = getPlayerperformanceInfo(playerInfo, player, 14);
    if (infoAt14 != null) {
      final int xpDifference;
      final int goldDifference;

      val infoAt27 = getPlayerperformanceInfo(playerInfo, player, 27);
      if (infoAt27 != null) {

        val enemyPlayer = getEnemyPlayer(player);
        if (enemyPlayer != null) {
          final int leadMidgame = getLeadAt(playerInfo, player, 27);
          final int leadLategame = getLeadAt(playerInfo, player, endMinute);
          final int leadDifference = leadLategame - leadMidgame;
          stats.setLategameLead((short) leadDifference);
        }

        xpDifference = infoAt27.getExperience() - infoAt14.getExperience();
        goldDifference = infoAt27.getTotalGold() - infoAt14.getTotalGold();

      } else {
        xpDifference = infoAt14.getPlayerperformance().getExperience() - infoAt14.getExperience();
        goldDifference = infoAt14.getPlayerperformance().getGoldTotal() - infoAt14.getTotalGold();
      }
      final double xpPercentage = xpDifference * 1d / Const.MIDGAME_XP;
      stats.setMidgameXPEfficiency(BigDecimal.valueOf(xpPercentage));

      final double goldPercentage = goldDifference * 1d / Const.MIDGAME_XP;
      stats.setMidgameGoldEfficiency(BigDecimal.valueOf(goldPercentage));
    }
  }

  private static int getRechargeTimeAtLevel(int level) {
    final int levelProgress = (level - 1) / 17;
    final int rechargeDifference = Const.YELLOW_TRINKET_RECHARGE_TIME_START - Const.YELLOW_TRINKET_RECHARGE_TIME_END;
    return (1 - levelProgress) * rechargeDifference + Const.YELLOW_TRINKET_RECHARGE_TIME_END;
  }

  private static int getLevelupTime(Playerperformance playerperformance, int level) {
    return playerperformance.getLevelups().stream()
        .map(PlayerperformanceLevel::getTime)
        .filter(lvlLevel -> lvlLevel == level)
        .findFirst().orElse(0);
  }

  private static double determineAssistbountyFactor(int timestamp) {
    final int second = timestamp / 1000;
    if (second <= Const.ASSIST_FACTOR_INCREASE_SECOND) {
      return Const.ASSIST_FACTOR_START_VALUE;

    } else if (second >= Const.ASSIST_FACTOR_ENDING_SECOND) {
      return Const.ASSIST_FACTOR_END_VALUE;

    } else {
      final int currentSecond = second - Const.ASSIST_FACTOR_INCREASE_SECOND;
      final int timespanDifference = Const.ASSIST_FACTOR_ENDING_SECOND - Const.ASSIST_FACTOR_INCREASE_SECOND;
      final double progressToMaxValue = currentSecond * 1d / timespanDifference;
      final double valueDifference = Const.ASSIST_FACTOR_END_VALUE - Const.ASSIST_FACTOR_START_VALUE;
      return valueDifference * progressToMaxValue + Const.ASSIST_FACTOR_START_VALUE;
    }
  }

  /**
   * Suche, inwiefer Spieler von anderem Spieler getötet wurde
   *
   * @param event Ereignis
   * @return Spieler wurde von anderem Spieler getötet
   */
  private static boolean wasKilledByPlayer(JSONObject event) {
    final int killerId = event.getInt("killerId");
    return killerId != 0;
  }

  /**
   * Suche, inwiefert Spieler am Ereignis beteiligt
   *
   * @param pId Id des gesuchten Spielers (bereits angepasst)
   * @param event Ereignis
   * @param role Rolle des Spielersd
   * @return Spieler war am Ereignis beteiligt
   */
  private static boolean isParticipant(int pId, JSONObject event, KillRole role) {
    if (role.equals(KillRole.VICTIM)) {
      final int victimId = event.getInt("victimId");
      return pId == victimId;

    } else if (role.equals(KillRole.KILLER)) {
      final int killerId = event.getInt("killerId");
      return pId == killerId;

    } else if (role.equals(KillRole.ASSIST) && event.has("assistingParticipantIds")) {
      val participantIds = event.getJSONArray("assistingParticipantIds").toList()
          .stream().map(id -> (Integer) id).collect(Collectors.toList());
      return participantIds.contains(pId);
    }

    return false;
  }

  /**
   * Bestimme das Startitem, dass ein Spieler gekauft hat
   *
   * @param events Ereignisse
   * @return Sekunden, wann es verkauft wurde
   */
  private static short determineStartItem(List<JSONObject> events) {
    for (JSONObject event : events) {
      val type = EventTypes.valueOf(event.getString("type"));
      if (type.equals(EventTypes.ITEM_SOLD)) {
        val itemId = event.getInt("itemId");
        val item = Item.find((short) itemId);
        if (item.getType().equals(ItemType.STARTING)) {
          return (short) (event.getInt("timestamp") / 1000);
        }
      }
    }
    return 0;
  }

  private static int getCSAt(Map<Integer, JSONObject> playerInfo, JSONPlayer player, int minute) {
    val playerperformanceInfoMeAt15 = getPlayerperformanceInfo(playerInfo, player, minute);
    if (playerperformanceInfoMeAt15 != null) {
      return playerperformanceInfoMeAt15.getCreepScore();
    }
    return 0;
  }

  private static int getCSLeadAt(Map<Integer, JSONObject> playerInfo, JSONPlayer player, int minute) {
    val enemyPlayer = getEnemyPlayer(player);
    if (enemyPlayer != null) {

      val playerperformanceInfoMe = getPlayerperformanceInfo(playerInfo, player, minute);
      if (playerperformanceInfoMe != null) {

        val playerperformanceInfoEnemy = getPlayerperformanceInfo(playerInfo, enemyPlayer, minute);
        if (playerperformanceInfoEnemy != null) {

          return playerperformanceInfoMe.getCreepScore() - playerperformanceInfoEnemy.getCreepScore();
        }
      }
    }
    return 0;
  }

  private static int getGoldLeadAt(Map<Integer, JSONObject> playerInfo, JSONPlayer player, int minute) {
    val enemyPlayer = getEnemyPlayer(player);
    if (enemyPlayer != null) {

      val playerperformanceInfoMe = getPlayerperformanceInfo(playerInfo, player, minute);
      if (playerperformanceInfoMe != null) {

        val playerperformanceInfoEnemy = getPlayerperformanceInfo(playerInfo, enemyPlayer, minute);
        if (playerperformanceInfoEnemy != null) {

          return playerperformanceInfoMe.getTotalGold() - playerperformanceInfoEnemy.getTotalGold();
        }
      }
    }
    return 0;
  }

  private static void handleControlled(Map<Integer, JSONObject> playerInfo, JSONPlayer player, int minute, PlayerperformanceStats stats) {
    final List<Double> controlValues = handleControlledAt(playerInfo, player, minute);
    if (controlValues != null) {
      stats.setEnemyControlAdvantage(controlValues.get(0), controlValues.get(1));
    }

    final List<Double> controlValuesAt15 = handleControlledAt(playerInfo, player, 15);
    if (controlValuesAt15 != null) {
      stats.setEnemyControlAdvantageEarly(controlValuesAt15.get(0), controlValuesAt15.get(1));
    }
  }

  private static List<Double> handleControlledAt(Map<Integer, JSONObject> playerInfo, JSONPlayer player, int minute) {
    val list = new ArrayList<Double>();
    val enemyPlayer = getEnemyPlayer(player);
    if (enemyPlayer != null) {

      val playerperformanceInfoMe = getPlayerperformanceInfo(playerInfo, player, minute);
      if (playerperformanceInfoMe != null) {
        final double enemyControlled = playerperformanceInfoMe.getEnemyControlled();
        list.add(enemyControlled);

        val playerperformanceInfoEnemy = getPlayerperformanceInfo(playerInfo, enemyPlayer, minute);
        if (playerperformanceInfoEnemy != null) {
          final double underControl = playerperformanceInfoEnemy.getEnemyControlled();
          list.add(underControl);
          return list;
        }
      }
    }
    return null;
  }

  private static int getLeadAt(Map<Integer, JSONObject> playerInfo, JSONPlayer player, int minute) {
    val enemyPlayer = getEnemyPlayer(player);
    if (enemyPlayer != null) {

      val playerperformanceInfoMeAt15 = getPlayerperformanceInfo(playerInfo, player, minute);
      if (playerperformanceInfoMeAt15 != null) {
        final int leadMe = playerperformanceInfoMeAt15.getExperience() + playerperformanceInfoMeAt15.getTotalGold();

        val playerperformanceInfoEnemyAt15 = getPlayerperformanceInfo(playerInfo, enemyPlayer, minute);
        if (playerperformanceInfoEnemyAt15 != null) {
          final int leadEnemy = playerperformanceInfoEnemyAt15.getExperience() + playerperformanceInfoEnemyAt15.getTotalGold();

          return leadMe - leadEnemy;
        }
      }
    }
    return 0;
  }

  private static int getXPLeadAt(Map<Integer, JSONObject> playerInfo, JSONPlayer player, int minute) {
    val enemyPlayer = getEnemyPlayer(player);
    if (enemyPlayer != null) {

      val playerperformanceInfoMeAt15 = getPlayerperformanceInfo(playerInfo, player, minute);
      if (playerperformanceInfoMeAt15 != null) {

        val playerperformanceInfoEnemyAt15 = getPlayerperformanceInfo(playerInfo, enemyPlayer, minute);
        if (playerperformanceInfoEnemyAt15 != null) {

          return playerperformanceInfoMeAt15.getExperience() - playerperformanceInfoEnemyAt15.getExperience();
        }
      }
    }
    return 0;
  }

  private static PlayerperformanceInfo getPlayerperformanceInfo(Map<Integer, JSONObject> playerInfo, JSONPlayer player, int minute) {
    for (int timestamp : playerInfo.keySet()) {
      if (timestamp / 60000 == minute) {
        val frame = playerInfo.get(timestamp);
        val infoStats = frame.getJSONObject(String.valueOf(player.getId() + 1));
        val positionObject = infoStats.getJSONObject("position");
        val position = new Position((short) positionObject.getInt("x"), (short) positionObject.getInt("y"));
        final int xp = infoStats.getInt("xp");
        final int totalGold = infoStats.getInt("totalGold");
        final double enemyControlled = infoStats.getInt("timeEnemySpentControlled") * 1d / 1000;
        final int lead = getLeadAt(playerInfo, player, minute);
        final short creepScore = (short) (infoStats.getInt("minionsKilled") + infoStats.getInt("jungleMinionsKilled"));
        return new PlayerperformanceInfo((short) minute, totalGold, (short) infoStats.getInt("currentGold"), enemyControlled, position,
            xp, (short) lead, creepScore, infoStats.getJSONObject("damageStats").getInt("totalDamageDoneToChampions"));
      }
    }
    return null;
  }

  private static byte determineAllObjectivesAmount(List<JSONTeam> jsonTeams) {
    byte allObjectivesAmount = 0;
    for (JSONTeam team : jsonTeams) {
      val objectives = team.getTeamObject().getJSONObject("objectives");
      val tower = objectives.getJSONObject("tower");
      allObjectivesAmount += tower.getInt("kills");
      val dragon = objectives.getJSONObject("dragon");
      allObjectivesAmount += dragon.getInt("kills");
      val riftHerald = objectives.getJSONObject("riftHerald");
      allObjectivesAmount += riftHerald.getInt("kills");
    }
    return allObjectivesAmount;
  }

  private static byte determineStolenBarons(List<JSONObject> events, JSONTeam jsonTeam, JSONTeam enemyTeam) {
    byte stolenBarons = 0;
    for (JSONObject event : events) {
      val type = EventTypes.valueOf(event.getString("type"));
      if (type.equals(EventTypes.ELITE_MONSTER_KILL) && event.getString("monsterType").equals("BARON_NASHOR")) {
        int t1 = 0;
        int t2 = 0;
        val participatingIds = new ArrayList<Integer>();
        if (event.has("assistingParticipantIds")) {
          participatingIds.addAll(event.getJSONArray("assistingParticipantIds").toList()
              .stream().map(id -> (Integer) id - 1).collect(Collectors.toList()));
        }

        for (int i : participatingIds) {
          if (jsonTeam.hasPlayer(i)) {
            t1++;
          } else if (enemyTeam.hasPlayer(i)) {
            t2++;
          }
        }
        if (t2 < t1) {
          int killerId = event.getInt("killerId");
          if (enemyTeam.hasPlayer(killerId - 1)) {
            stolenBarons++;
          }
        }

      }
    }
    return stolenBarons;
  }

  private static List<Short> searchForControlPurchase(List<JSONObject> events, int pId) {
    val returnList = new ArrayList<Short>();
    for (JSONObject event : events) {
      if (event.has("participantId")) {
        final int participantId = event.getInt("participantId");
        val type = EventTypes.valueOf(event.getString("type"));

        if (participantId == pId && type.equals(EventTypes.ITEM_PURCHASED)) {
          val itemId = event.getInt("itemId");
          val item = Item.find((short) itemId);
          if (item.getItemName().equals("Control Ward")) {
            final int timestamp = event.getInt("timestamp");
            returnList.add((short) (timestamp / 1000));
          }
        }
      }
    }

    return returnList;
  }

  private static List<Short> searchForControlPlacements(List<JSONObject> events, int pId) {
    val returnList = new ArrayList<Short>();
    for (JSONObject event : events) {
      if (event.has("creatorId")) {
        final int creatorId = event.getInt("creatorId");
        val type = EventTypes.valueOf(event.getString("type"));

        if (creatorId == pId && type.equals(EventTypes.WARD_PLACED)) {
          val wardTypeString = event.getString("wardType");
          val wardType = WardType.valueOf(wardTypeString);

          if (wardType.equals(WardType.CONTROL_WARD)) {
            final int timestamp = event.getInt("timestamp");
            returnList.add((short) (timestamp / 1000));
          }
        }
      }
    }

    return returnList;
  }

  private static short searchForTrinketSwap(List<JSONObject> events, int pId) {
    for (JSONObject event : events) {
      if (event.has("participantId")) {
        final int participantId = event.getInt("participantId");
        val type = EventTypes.valueOf(event.getString("type"));

        if (participantId == pId && type.equals(EventTypes.ITEM_PURCHASED)) {
          val itemId = event.getInt("itemId");
          val item = Item.find((short) itemId);
          if (item.getType().equals(ItemType.TRINKET) && !item.getItemName().equals("Stealth Ward")) {
            return (short) (event.getInt("timestamp") / 1000);
          }
        }
      }
    }

    return 0;
  }

  private static List<Integer> searchForTrinketPlacementsUntilSwap(List<JSONObject> events, int pId, short second) {
    val list = new ArrayList<Integer>();
    for (JSONObject event : events) {
      if (event.has("creatorId")) {
        final int creatorId = event.getInt("creatorId");
        val type = EventTypes.valueOf(event.getString("type"));
        final int timestamp = event.getInt("timestamp");

        if (creatorId == pId && type.equals(EventTypes.WARD_PLACED) && timestamp / 1000 <= second) {
          val wardTypeString = event.getString("wardType");
          val wardType = WardType.valueOf(wardTypeString);
          if (wardType.equals(WardType.YELLOW_TRINKET)) {
            list.add(timestamp);
          }
        }
      }
    }

    return list;
  }

  private static short searchForFirstWardTime(List<JSONObject> events, int pId, short controlTime) {
    short wardTime = 0;
    for (JSONObject event : events) {
      if (event.has("creatorId")) {
        final int creatorId = event.getInt("creatorId");
        val type = EventTypes.valueOf(event.getString("type"));

        if (creatorId == pId && type.equals(EventTypes.WARD_PLACED)) {
          val wardTypeString = event.getString("wardType");
          val wardType = WardType.valueOf(wardTypeString);
          final short timestamp = (short) (event.getInt("timestamp") / 1000);

          if (wardType.equals(WardType.CONTROL_WARD) && controlTime == 0) {
            controlTime = timestamp;
          } else {
            wardTime = timestamp;
          }
        }

        if (wardTime != 0 && controlTime != 0) {
          return wardTime;
        }
      }
    }

    return 0;
  }

  /**
   * Created by Lara on 09.04.2022 for web
   * <p>
   * TINYINT(3) : -/.///.///.128 → /.///.///.127 (/.///.///.255)
   * SMALLINT(5) : -/.///./32.768 → /.///./32.767 (/.///./65.535)
   * MEDIUMINT(7): -/.//8.388.608 → /.//8.388.607 (/./16.777.215)
   * INTEGER(10) : -2.147.483.648 → 2.147.483.647 (4.294.967.295)
   * <p>
   * Byte:
   * -TINYINT(3) : -128 → 127
   * Short:
   * -TINYINT(3) : (255)
   * -SMALLINT(5) : -32.768 → 32.767
   * Int:
   * -SMALLINT(5) : (/.///./65.535)
   * -MEDIUMINT(7): -/.//8.388.608 → /.//8.388.607 (/./16.777.215)
   * -INTEGER(10) : -2.147.483.648 → 2.147.483.647.
   */
  private static Playerperformance handlePerformance(JSONPlayer p, JSONPlayer e) {
    final int shiedling = p.getMedium(StoredStat.DAMAGE_HEALING_SHIELDING) != null ? p.getMedium(StoredStat.DAMAGE_HEALING_SHIELDING) :
        p.getMedium(StoredStat.DAMAGE_TEAM_HEAL) + p.getMedium(StoredStat.DAMAGE_TEAM_SHIELD);
    final byte stolen = (byte) (p.getTiny(StoredStat.OBJECTIVES_STOLEN) + p.getTiny(StoredStat.OBJECTIVES_STOLEN_TAKEDOWNS));
    final short creeps = (short) (p.getSmall(StoredStat.CREEP_SCORE_JUNGLE) + p.getSmall(StoredStat.CREEP_SCORE_LANE));
    final boolean firstBlood = p.getBool(StoredStat.FIRST_BLOOD) || p.getBool(StoredStat.FIRST_BLOOD_ASSIST);
    val visionScore = e != null ? (byte) (p.getSmall(StoredStat.VISION_SCORE) - e.getSmall(StoredStat.VISION_SCORE)) :
        null;
    final byte controlWards = p.getTiny(StoredStat.CONTROL_WARDS_PLACED, StoredStat.CONTROL_WARDS_BOUGHT);
    final byte wardClear = p.getTiny(StoredStat.WARDS_TAKEDOWN, StoredStat.WARDS_CLEARED);
    val playerperformance = new Playerperformance(Lane.valueOf(p.get(StoredStat.LANE)),
        p.getSmall(StoredStat.Q_USAGE), p.getSmall(StoredStat.W_USAGE), p.getSmall(StoredStat.E_USAGE),
        p.getSmall(StoredStat.R_USAGE), p.getMedium(StoredStat.DAMAGE_MAGICAL), p.getMedium(StoredStat.DAMAGE_PHYSICAL),
        p.getMedium(StoredStat.DAMAGE_TOTAL), p.getMedium(StoredStat.DAMAGE_TAKEN), p.getMedium(StoredStat.DAMAGE_MITIGATED),
        p.getMedium(StoredStat.DAMAGE_HEALED), shiedling, p.getTiny(StoredStat.KILLS), p.getTiny(StoredStat.DEATHS),
        p.getTiny(StoredStat.ASSISTS), p.getTiny(StoredStat.KILLS_DOUBLE), p.getTiny(StoredStat.KILLS_TRIPLE),
        p.getTiny(StoredStat.KILLS_QUADRA), p.getTiny(StoredStat.KILLS_PENTA), p.getSmall(StoredStat.TIME_ALIVE),
        p.getSmall(StoredStat.TIME_DEAD), p.getSmall(StoredStat.WARDS_PLACED), stolen, p.getMedium(StoredStat.OBJECTIVES_DAMAGE),
        p.getTiny(StoredStat.BARON_KILLS), p.getMedium(StoredStat.GOLD_TOTAL), p.getMedium(StoredStat.EXPERIENCE_TOTAL), creeps,
        p.getSmall(StoredStat.ITEMS_BOUGHT), firstBlood, controlWards, wardClear, p.getSmall(StoredStat.VISION_SCORE),
        p.getTiny(StoredStat.TOWERS_TAKEDOWNS));
    if (visionScore != null) playerperformance.setVisionscoreAdvantage(visionScore);
    playerperformance.setSpellsHit(p.getSmall(StoredStat.SPELL_LANDED));
    playerperformance.setSpellsDodged(p.getSmall(StoredStat.SPELL_DODGE));
    playerperformance.setQuickDodged(p.getSmall(StoredStat.SPELL_DODGE_QUICK));
    playerperformance.setSoloKills(p.getTiny(StoredStat.SOLO_KILLS));
    playerperformance.setLevelUpAllin(p.getTiny(StoredStat.LEVELUP_TAKEDOWNS));
    playerperformance.setFlashAggressive(p.getTiny(StoredStat.AGGRESSIVE_FLASH));
    playerperformance.setTeleportKills(p.getTiny(StoredStat.TELEPORT_KILLS));
    playerperformance.setImmobilizations(p.getSmall(StoredStat.IMMOBILIZATIONS));
    if (p.getSmall(StoredStat.CONTROL_WARDS_UPTIME) != null) {
      playerperformance.setControlWardUptime((short) (p.getSmall(StoredStat.CONTROL_WARDS_UPTIME) * 60));
    }
    playerperformance.setWardsGuarded(p.getTiny(StoredStat.WARDS_GUARDED));
    if (p.getSmall(StoredStat.FIRST_TOWER_TIME) != null &&
        (p.getBool(StoredStat.FIRST_TOWER) || p.getBool(StoredStat.FIRST_TOWER_ASSIST))) {
      playerperformance.setFirstturretAdvantage(p.getSmall(StoredStat.FIRST_TOWER_TIME));
    } else if (e != null && e.getSmall(StoredStat.FIRST_TOWER_TIME) != null &&
        (e.getBool(StoredStat.FIRST_TOWER) || e.getBool(StoredStat.FIRST_TOWER_ASSIST))) {
      playerperformance.setFirstturretAdvantage((short) (e.getSmall(StoredStat.FIRST_TOWER_TIME) * -1));
    }
    if (e != null) playerperformance.setBaronExecutes(e.getTiny(StoredStat.BARON_EXECUTES));
    playerperformance.setBuffsStolen(p.getTiny(StoredStat.BUFFS_STOLEN));
    playerperformance.setScuttlesInitial(p.getTiny(StoredStat.SCUTTLES_INITIAL));
    playerperformance.setScuttlesTotal(p.getTiny(StoredStat.SCUTTLES_TOTAL));
    playerperformance.setSplitpushedTurrets(p.getTiny(StoredStat.TOWERS_SPLITPUSHED));
    playerperformance.setTeamInvading(p.getTiny(StoredStat.INVADING_KILLS));
    playerperformance.setGanksEarly(p.getTiny(StoredStat.LANER_ROAMS, StoredStat.JUNGLER_ROAMS));
    playerperformance.setDivesDone(handleDives(p, e, StoredStat.DIVES_DONE, StoredStat.DIVES_PROTECTED));
    playerperformance.setDivesSuccessful(p.getTiny(StoredStat.DIVES_DONE));
    playerperformance.setDivesGotten(handleDives(p, e, StoredStat.DIVES_PROTECTED, StoredStat.DIVES_DONE));
    playerperformance.setDivesProtected(p.getTiny(StoredStat.DIVES_PROTECTED));
    playerperformance.setBountyGold((short) (p.getSmall(StoredStat.BOUNTY_GOLD) -
        (e != null && e.getSmall(StoredStat.BOUNTY_GOLD) == null ? 0 : p.getSmall(StoredStat.BOUNTY_GOLD))));
    playerperformance.setCreepsEarly(p.getTiny(StoredStat.CREEP_SCORE_LANE_EARLY, StoredStat.CREEP_SCORE_JUNGLE_EARLY));
    playerperformance.setCreepsInvade(p.getSmall(StoredStat.CREEP_INVADED));
    playerperformance.setTurretplates(p.getTiny(StoredStat.TOWERS_PLATES));
    playerperformance.setFlamehorizonAdvantage(p.getTiny(StoredStat.CREEP_SCORE_ADVANTAGE));
    final short mejaisCompleted = (short) ((p.getSmall(StoredStat.MEJAIS_TIME) == null ? 0 : p.getSmall(StoredStat.MEJAIS_TIME))
        - (e != null && e.getSmall(StoredStat.MEJAIS_TIME) != null ? e.getSmall(StoredStat.MEJAIS_TIME) : 0));
    if (mejaisCompleted != 0) playerperformance.setMejaisCompleted(mejaisCompleted);
    playerperformance.setOutplayed(p.getTiny(StoredStat.OUTPLAYED));
    playerperformance.setDragonTakedowns(p.getTiny(StoredStat.DRAGON_TAKEDOWNS));
    playerperformance.setFastestLegendary(p.getSmall(StoredStat.LEGENDARY_FASTEST));
    playerperformance.setGankSetups(p.getTiny(StoredStat.GANK_SETUP));
    playerperformance.setInitialBuffs(p.getTiny(StoredStat.BUFFS_INITIAL));
    playerperformance.setEarlyKills(p.getTiny(StoredStat.KILLS_EARLY_JUNGLER, StoredStat.KILLS_EARLY_LANER));
    playerperformance.setJunglerKillsAtObjective(p.getTiny(StoredStat.OBJECTIVES_JUNGLERKILL));
    playerperformance.setAmbush(p.getTiny(StoredStat.AMBUSH));
    playerperformance.setEarlyTurrets(p.getTiny(StoredStat.TOWERS_EARLY));
    playerperformance.setLevelLead(p.getTiny(StoredStat.EXPERIENCE_ADVANTAGE));
    playerperformance.setPicksMade(p.getTiny(StoredStat.PICK_KILL));
    playerperformance.setAssassinated(p.getTiny(StoredStat.ASSASSINATION));
    playerperformance.setSavedAlly(p.getTiny(StoredStat.GUARD_ALLY));
    playerperformance.setSurvivedClose((byte) (p.getTiny(StoredStat.SURVIVED_CLOSE) +
        p.getTiny(StoredStat.SURVIVED_HIGH_DAMAGE) + p.getTiny(StoredStat.SURVIVED_HIGH_CROWDCONTROL)));
    return playerperformance;
  }

  private static byte handleDives(JSONPlayer player, JSONPlayer enemy, StoredStat divesDone, StoredStat divesProtected) {
    return (byte) ((player.getTiny(divesDone) == null ? 0 : player.getTiny(divesDone)) +
        ((enemy != null && enemy.getTiny(divesProtected) != null) ? enemy.getTiny(divesProtected) : 0));
  }

  private static void handleChampionsPicked(JSONPlayer player, JSONPlayer enemy, Playerperformance playerperformance) {
    val championOwnName = player.get(StoredStat.CHAMPION);
    val championOwn = Champion.find(championOwnName);
    championOwn.addPlayerperformance(playerperformance, true);
    if (enemy != null) {
      val championEnemyName = enemy.get(StoredStat.CHAMPION);
      val championEnemy = Champion.find(championEnemyName);
      championEnemy.addPlayerperformance(playerperformance, false);
    }
  }

  private static JSONPlayer getEnemyPlayer(JSONPlayer player) {
    val jsonPlayers = findJSONPlayersExcept(player.get(StoredStat.LANE), player);
    return jsonPlayers.isEmpty() ? null : jsonPlayers.get(0);
  }

  private static List<JSONPlayer> determinePlayers(QueueType queueType, JSONTeam jsonTeam) {
    val players = new ArrayList<JSONPlayer>();
    if (queueType.equals(QueueType.TOURNEY)) {
      players.addAll(jsonTeam.getAllPlayers());
    } else if (queueType.equals(QueueType.CLASH)) {
      players.addAll(jsonTeam.getListedPlayers());
    } else if (queueType.equals(QueueType.OTHER)) {
      players.addAll(jsonTeam.getListedPlayers());
    }
    return players;
  }

  private static List<JSONTeam> getJsonTeams(JSONArray participants) {
    val jsonTeams = Arrays.asList(new JSONTeam(1), new JSONTeam(2));
    for (int i = 0; i < participants.length(); i++) {
      val participant = participants.getJSONObject(i);
      val puuid = participant.getString("puuid");
      val jsonPlayer = new JSONPlayer(i, participant, puuid);
      if (participant.getInt("teamid") == 100) {
        jsonTeams.get(0).addPlayer(jsonPlayer);
      } else {
        jsonTeams.get(1).addPlayer(jsonPlayer);
      }
    }
    return jsonTeams;
  }

  private static Teamperformance handleTeam(JSONTeam jsonTeam) {
    val jsonObject = jsonTeam.getTeamObject();
    val objectives = jsonObject.getJSONObject("objectives");
    val champion = objectives.getJSONObject("champion");
    val tower = objectives.getJSONObject("tower");
    val dragon = objectives.getJSONObject("dragon");
    val inhibitor = objectives.getJSONObject("inhibitor");
    val riftHerald = objectives.getJSONObject("riftHerald");
    val baron = objectives.getJSONObject("baron");
    final int teamId = jsonObject.getInt("teamId");
    final boolean win = jsonObject.getBoolean("win");

    final int totalDamage = jsonTeam.getAllPlayers().stream().mapToInt(player -> player.getMedium(StoredStat.DAMAGE_TOTAL)).sum();
    final int damageTaken = jsonTeam.getAllPlayers().stream().mapToInt(player -> player.getMedium(StoredStat.DAMAGE_TAKEN)).sum();
    final int totalGold = jsonTeam.getAllPlayers().stream().mapToInt(player -> player.getMedium(StoredStat.GOLD_TOTAL)).sum();
    final int totalCs = jsonTeam.getAllPlayers().stream()
        .mapToInt(player -> player.getMedium(StoredStat.CREEP_SCORE_LANE) + player.getMedium(StoredStat.CREEP_SCORE_JUNGLE)).sum();
    final short earliestDragon = (short) jsonTeam.getAllPlayers().stream().mapToInt(p -> p.getSmall(StoredStat.DRAGON_TIME)).min().orElse(-1);
    final byte atSpawn = (byte) jsonTeam.getAllPlayers().stream().mapToInt(player -> player.getSmall(StoredStat.OBJECTIVES_ON_SPAWN)).sum();
    final byte nearJgl = (byte) jsonTeam.getAllPlayers().stream().mapToInt(player -> player.getSmall(StoredStat.OBJECTIVES_50_50)).sum();
    final byte quest = (byte) jsonTeam.getAllPlayers().stream().filter(player -> player.getSmall(StoredStat.QUEST_FAST) == 1).count();
    final byte herald = (byte) jsonTeam.getAllPlayers().stream().mapToInt(player -> player.getTiny(StoredStat.RIFT_TURRETS_MULTI)).sum();
    final short acetime = (short) jsonTeam.getAllPlayers().stream().mapToInt(p -> p.getSmall(StoredStat.ACE_TIME)).min().orElse(-1);
    final byte killDeficit = (byte) jsonTeam.getAllPlayers().stream().mapToInt(p -> p.getTiny(StoredStat.KILLS_DISADVANTAGE)).sum();

    final Teamperformance teamperformance = new Teamperformance(teamId == 100, win, totalDamage, damageTaken,
        totalGold, totalCs, champion.getInt("kills"), tower.getInt("kills"), dragon.getInt("kills"), inhibitor.getInt("kills"),
        riftHerald.getInt("kills"), baron.getInt("kills"), tower.getBoolean("first"), dragon.getBoolean("first"));
    final JSONPlayer jsonPlayer = jsonTeam.getAllPlayers().get(0);
    if (jsonPlayer.getMedium(StoredStat.PERFECT_SOUL) != null)
      teamperformance.setPerfectSoul(jsonPlayer.getMedium(StoredStat.PERFECT_SOUL) == 1);
    teamperformance.setSurrendered(jsonPlayer.getMedium(StoredStat.SURRENDER) == 1);
    if (jsonPlayer.getSmall(StoredStat.RIFT_TURRETS) != null)
      teamperformance.setRiftTurrets(jsonPlayer.getSmall(StoredStat.RIFT_TURRETS) / 5d);
    if (jsonPlayer.getSmall(StoredStat.ELDER_TIME) != null) teamperformance.setElderTime(jsonPlayer.getSmall(StoredStat.ELDER_TIME));
    if (jsonPlayer.getSmall(StoredStat.BARON_POWERPLAY) != null)
      teamperformance.setElderTime(jsonPlayer.getSmall(StoredStat.BARON_POWERPLAY));
    teamperformance.setEarlyAces(jsonPlayer.getTiny(StoredStat.ACE_EARLY));
    teamperformance.setBaronTime(jsonPlayer.getTiny(StoredStat.BARON_TIME));
    if (earliestDragon != -1) teamperformance.setFirstDragonTime(earliestDragon);
    teamperformance.setObjectiveAtSpawn(atSpawn);
    teamperformance.setObjectiveContests(nearJgl);
    teamperformance.setQuestCompletedFirst(quest > 0);
    teamperformance.setInhibitorsTime(jsonPlayer.getSmall(StoredStat.INHIBITORS_TAKEN));
    teamperformance.setFlawlessAces(jsonPlayer.getTiny(StoredStat.ACE_FLAWLESS));
    teamperformance.setRiftOnMultipleTurrets(herald);
    if (acetime != -1) teamperformance.setFastestAcetime(acetime);
    if (killDeficit > 0) teamperformance.setKillDeficit(killDeficit);

    return teamperformance;
  }

  private static Game handleGame(JSONObject info, String gameId, Gametype gametype) {
    final long startMillis = info.getLong("gameStartTimestamp");
    val start = new Date(startMillis);
    final long endMillis = info.getLong("gameStartTimestamp");
    final short duration = (short) (endMillis - startMillis / 1000);
    return Game.get(new Game(gameId, start, duration), gametype);
  }

  private static void loadTimeline(List<JSONObject> events, Map<Integer, JSONObject> playerInfo, JSONObject timeLineObject) {
    val timelineInfo = timeLineObject.getJSONObject("info");
    val timelineFrames = timelineInfo.getJSONArray("frames");
    for (int i = 0; i < timelineFrames.length(); i++) {
      val frameObject = timelineFrames.getJSONObject(i);
      val eventArray = frameObject.getJSONArray("events");
      val event = timelineFrames.getJSONObject(0);
      if (Arrays.stream(EventTypes.values()).anyMatch(type2 -> type2.name().equals(event.getString("type")))) {
        IntStream.range(0, eventArray.length()).mapToObj(eventArray::getJSONObject).forEach(events::add);
      }
      if (frameObject.has("participantFrames") && !frameObject.isNull("participantFrames")) {
        playerInfo.put(frameObject.getInt("timestamp"), frameObject.getJSONObject("participantFrames"));
      }
    }
  }

  private static List<Fight> handleGameEvents(List<JSONObject> events, Game game) {
    val kills = new ArrayList<Kill>();
    for (JSONObject event : events) {
      val type = EventTypes.valueOf(event.getString("type"));
      final int timestamp = event.getInt("timestamp");
      if (type.equals(EventTypes.PAUSE_START)) {
        handlePauseStart(timestamp, game);

      } else if (type.equals(EventTypes.PAUSE_END)) {
        handlePauseEnd(timestamp, game);

      } else if (type.equals(EventTypes.CHAMPION_KILL)) {
        val positionObject = event.getJSONObject("position");
        final int xCoordinate = positionObject.getInt("x");
        final int yCoordinate = positionObject.getInt("y");
        val position = new Position((short) xCoordinate, (short) yCoordinate);

        final int victim = event.getInt("victimId");
        final int killer = event.getInt("killerId");
        final int gold = event.getInt("shutdownBounty") + event.getInt("bounty");
        final Map<Integer, Integer> participants = event.getJSONArray("assistingParticipantIds").toList()
            .stream().map(id -> (Integer) id).collect(Collectors.toMap(Function.identity(), e -> 0));
        val damageReceived = event.getJSONArray("victimDamageReceived");
        handleDamageValues(participants, damageReceived);
        val damageDealt = event.getJSONArray("victimDamageDealt");
        handleDamageValues(participants, damageDealt);


        if (killer != 0 || !participants.isEmpty()) {
          val kill = new Kill(timestamp, position, killer, victim, participants, gold);
          kills.add(kill);
        }

      }
    }

    kills.sort(Comparator.comparingInt(Kill::getTimestamp));

    val fights = new ArrayList<Fight>();
    for (Kill kill : kills) {
      val validFight = fights.stream().filter(fight -> fight.getLastTimestamp() >= kill.getTimestamp() - Const.TIME_BETWEEN_FIGHTS * 60_000)
          .filter(fight -> Util.distance(fight.getLastPosition(), kill.getPosition()) <= Const.DISTANCE_BETWEEN_FIGHTS)
          .findFirst().orElse(null);
      if (validFight == null) {
        fights.add(new Fight(kill));
      } else {
        validFight.addKill(kill);
      }
    }

    val returnFights = new ArrayList<Fight>();
    for (Fight fight : fights) {
      if (fight.getFighttype().equals(Fighttype.DUEL)) {
        returnFights.add(fight.getDuel());
      } else if (fight.getFighttype().equals(Fighttype.PICK)) {
        returnFights.add(fight.getPick());
      } else if (fight.getFighttype().equals(Fighttype.SKIRMISH)) {
        returnFights.add(fight.getSkirmish());
      } else if (fight.getFighttype().equals(Fighttype.TEAMFIGHT)) {
        returnFights.add(fight.getTeamfight());
      }
    }


    return returnFights;
  }

  private static void handleDamageValues(Map<Integer, Integer> participants, JSONArray damage) {
    for (int i = 0; i < damage.length(); i++) {
      val damageObject = damage.getJSONObject(i);
      final int magicalDamage = damageObject.getInt("magicDamage");
      final int physicalDamage = damageObject.getInt("physicalDamage");
      final int trueDamage = damageObject.getInt("trueDamage");
      final int totalDamage = magicalDamage + physicalDamage + trueDamage;

      final int partId = damageObject.getInt("participantId");

      if (participants.containsKey(partId)) {
        participants.put(partId, participants.get(partId + totalDamage));
      } else if (partId != 0) {
        participants.put(partId, totalDamage);
      }
    }
  }

  private static void handlePauseStart(int timestamp, Game game) {
    if (game.getNotOpened().isEmpty()) {
      game.addPause(new GamePause(timestamp, 0));
    } else {
      game.getNotOpened().get(0).setStart(timestamp);
    }
  }

  private static void handlePauseEnd(int timestamp, Game game) {
    if (game.getNotClosed().isEmpty()) {
      game.addPause(new GamePause(0, timestamp));
    } else {
      game.getNotClosed().get(0).setEnd(timestamp);
    }
  }

  private static void handleTeamEvents(List<JSONObject> events, Teamperformance teamperformance) {
    for (JSONObject event : events) {
      val type = EventTypes.valueOf(event.getString("type"));
      final int timestamp = event.getInt("timestamp");
      if (type.equals(EventTypes.OBJECTIVE_BOUNTY_PRESTART)) {
        handleBountyStart(timestamp, teamperformance);
      } else if (type.equals(EventTypes.OBJECTIVE_BOUNTY_FINISH)) {
        handleBountyEnd(timestamp, teamperformance);
      } else if (type.equals(EventTypes.DRAGON_SOUL_GIVEN)) {
        teamperformance.setSoul(DragonSoul.valueOf(event.getString("name").toUpperCase()));
      }
    }
  }

  private static void handleBountyStart(int timestamp, Teamperformance teamperformance) {
    if (teamperformance.getNotOpened().isEmpty()) {
      teamperformance.addBounty(new TeamperformanceBounty(timestamp, 0));
    } else {
      teamperformance.getNotOpened().get(0).setStart(timestamp);
    }
  }

  private static void handleBountyEnd(int timestamp, Teamperformance teamperformance) {
    if (teamperformance.getNotClosed().isEmpty()) {
      teamperformance.addBounty(new TeamperformanceBounty(0, timestamp));
    } else {
      teamperformance.getNotClosed().get(0).setEnd(timestamp);
    }
  }

  private static void handlePlayerEvents(List<JSONObject> events, JSONPlayer player, Playerperformance playerperformance) {
    val items = IntStream.range(1, 8).mapToObj(i -> player.getMedium(StoredStat.valueOf("ITEM_" + i)))
        .filter(itemId -> itemId != null && itemId != 0).collect(Collectors.toList());
    for (JSONObject event : events) {
      val type = EventTypes.valueOf(event.getString("type"));
      final int timestamp = event.getInt("timestamp");
      val role = handleEventOfPlayer(player, event);

      if (role != null) {
        if (type.equals(EventTypes.LEVEL_UP)) {
          playerperformance.addLevelup(new PlayerperformanceLevel((byte) event.getInt("level"), timestamp));
        } else if (type.equals(EventTypes.ITEM_PURCHASED)) {
          final int itemId = event.getInt("itemId");
          playerperformance.addItem(Item.find((short) itemId), items.contains(itemId), timestamp);
        } else if (type.equals(EventTypes.CHAMPION_KILL)) {
          playerperformance.addKill(handleChampionKills(playerperformance, event, timestamp, role));
        } else if (type.equals(EventTypes.TURRET_PLATE_DESTROYED) || type.equals(EventTypes.BUILDING_KILL) ||
            type.equals(EventTypes.ELITE_MONSTER_KILL)) {
          handleObjectives(playerperformance, event, type, timestamp, role);
        }
      }
    }

    for (JSONObject event : events) {
      val type = EventTypes.valueOf(event.getString("type"));
      val role = handleEventOfPlayer(player, event);
      if (role != null && type.equals(EventTypes.CHAMPION_SPECIAL_KILL)) {
        final int timestamp = event.getInt("timestamp");
        val kill = PlayerperformanceKill.find(playerperformance.getTeamperformance().getGame(), timestamp);
        kill.setType(KillType.valueOf(event.getString("killType").replace("KILL_", "")));
      }
    }
  }

  private static KillRole handleEventOfPlayer(JSONPlayer player, JSONObject event) {
    final int playerId = event.has("killerId") ? event.getInt("killerId") : event.getInt("participantId");
    val participatingIds = new ArrayList<Integer>();
    if (event.has("assistingParticipantIds")) {
      participatingIds.addAll(event.getJSONArray("assistingParticipantIds").toList()
          .stream().map(id -> (Integer) id).collect(Collectors.toList()));
    }
    final int victimId = event.has("victimId") ? event.getInt("victimId") : 0;
    if (playerId == player.getId() + 1) {
      return KillRole.KILLER;
    } else if (participatingIds.contains(playerId + 1)) {
      return KillRole.ASSIST;
    } else if (victimId == player.getId() + 1) {
      return KillRole.VICTIM;
    }
    return null;
  }

  private static PlayerperformanceKill handleChampionKills(Playerperformance playerperformance, JSONObject event, int timestamp,
                                                           KillRole role) {
    val positionObject = event.getJSONObject("position");
    final int xCoordinate = positionObject.getInt("x");
    final int yCoordinate = positionObject.getInt("y");
    val position = new Position((short) xCoordinate, (short) yCoordinate);

    val kill = playerperformance == null ? null : PlayerperformanceKill.find(playerperformance.getTeamperformance().getGame(), timestamp);
    final int killId = kill == null ? PlayerperformanceKill.lastId() + 1 : kill.getId();

    return new PlayerperformanceKill(killId, timestamp, position, (short) event.getInt("bounty"), role, KillType.NORMAL,
        (byte) event.getInt("killStreakLength"));
  }

  private static void handleObjectives(Playerperformance playerperformance, JSONObject event, EventTypes type, int timestamp, KillRole role) {
    val lane = event.has("laneType") ? Lane.findLane(event.getString("laneType")) : null;
    if (type.equals(EventTypes.TURRET_PLATE_DESTROYED)) {
      playerperformance.addObjective(new PlayerperformanceObjective(timestamp, ObjectiveSubtype.OUTER_TURRET, lane,
          (short) 160, role));
    } else {
      var query = event.has("monsterSubType") ? "monsterSubType" : "monsterType";
      if (type.equals(EventTypes.BUILDING_KILL)) {
        query = event.has("towerType") ? "towerType" : "buildingType";
      }
      val objectiveType = ObjectiveSubtype.valueOf(event.getString(query).replace("_BUILDING", ""));
      final short bounty = (short) event.getInt("bounty");
      playerperformance.addObjective(new PlayerperformanceObjective(timestamp, objectiveType, lane, bounty, role));
    }
  }

  private static void handlePlayerInfo(Map<Integer, JSONObject> infos, JSONPlayer player, Playerperformance playerperformance) {
    for (int timestamp : infos.keySet()) {
      final JSONObject frame = infos.get(timestamp);
      final JSONObject stats = frame.getJSONObject(String.valueOf(player.getId() + 1));
      final JSONObject positionObject = stats.getJSONObject("position");
      final Position position = new Position((short) positionObject.getInt("x"), (short) positionObject.getInt("y"));
      final short creepScore = (short) (stats.getInt("minionsKilled") + stats.getInt("jungleMinionsKilled"));
      final int minute = timestamp / 60000;
      final int xp = stats.getInt("xp");
      final int totalGold = stats.getInt("totalGold");
      final double enemyControlled = stats.getInt("timeEnemySpentControlled") * 1d / 1000;
      final int lead = getLeadAt(infos, player, minute);
      final PlayerperformanceInfo info = new PlayerperformanceInfo((short) minute, totalGold, (short) stats.getInt("currentGold"),
          enemyControlled, position, xp, (short) lead, creepScore,
          stats.getJSONObject("damageStats").getInt("totalDamageDoneToChampions"));
      playerperformance.addInfo(info);
    }
  }

  private static void handleSummonerspells(JSONPlayer player, Playerperformance performance) {
    performance.addSummonerspell(Summonerspell.find(player.getTiny(StoredStat.SUMMONER1_ID)), player.getTiny(StoredStat.SUMMONER1_AMOUNT));
    performance.addSummonerspell(Summonerspell.find(player.getTiny(StoredStat.SUMMONER2_ID)), player.getTiny(StoredStat.SUMMONER2_AMOUNT));
  }
}
