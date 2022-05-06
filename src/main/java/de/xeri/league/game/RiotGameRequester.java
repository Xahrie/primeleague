package de.xeri.league.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.xeri.league.game.events.fight.Fight;
import de.xeri.league.game.events.fight.Kill;
import de.xeri.league.game.events.fight.enums.Fighttype;
import de.xeri.league.game.events.location.Position;
import de.xeri.league.game.models.JSONPlayer;
import de.xeri.league.game.models.JSONTeam;
import de.xeri.league.game.models.TimelineStat;
import de.xeri.league.models.dynamic.Champion;
import de.xeri.league.models.dynamic.Item;
import de.xeri.league.models.dynamic.Rune;
import de.xeri.league.models.dynamic.Summonerspell;
import de.xeri.league.models.enums.DragonSoul;
import de.xeri.league.models.enums.EventTypes;
import de.xeri.league.models.enums.KillRole;
import de.xeri.league.models.enums.KillType;
import de.xeri.league.models.enums.Lane;
import de.xeri.league.models.enums.ObjectiveSubtype;
import de.xeri.league.models.enums.QueueType;
import de.xeri.league.models.enums.SelectionType;
import de.xeri.league.models.enums.StoredStat;
import de.xeri.league.models.league.Account;
import de.xeri.league.models.match.ChampionSelection;
import de.xeri.league.models.match.Game;
import de.xeri.league.models.match.GamePause;
import de.xeri.league.models.match.Gametype;
import de.xeri.league.models.match.ScheduledGame;
import de.xeri.league.models.match.Teamperformance;
import de.xeri.league.models.match.TeamperformanceBounty;
import de.xeri.league.models.match.playerperformance.Playerperformance;
import de.xeri.league.models.match.playerperformance.PlayerperformanceInfo;
import de.xeri.league.models.match.playerperformance.PlayerperformanceKill;
import de.xeri.league.models.match.playerperformance.PlayerperformanceLevel;
import de.xeri.league.models.match.playerperformance.PlayerperformanceObjective;
import de.xeri.league.util.Const;
import de.xeri.league.util.Data;
import de.xeri.league.util.Util;
import de.xeri.league.util.io.json.JSON;
import de.xeri.league.util.io.riot.RiotAccountURLGenerator;
import de.xeri.league.util.io.riot.RiotURLGenerator;
import lombok.val;
import lombok.var;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Lara on 08.04.2022 for web
 */
public final class RiotGameRequester {
  private static int highestMinute = 0;
  public static List<JSONTeam> jsonTeams;
  static final List<JSONObject> allEvents = new ArrayList<>();

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
    //Data.getInstance().commit();
  }

  private static boolean isValidGame(JSON gameJson, JSON timelineJson, QueueType queueType) {
    val gameData = gameJson.getJSONObject();
    val info = gameData.getJSONObject("info");
    final int queueId = info.getInt("queueId");
    val participants = info.getJSONArray("participants");
    if ((queueId == 0 || queueId == 400 || queueId == 410 || queueId == 420 || queueId == 430 || queueId == 700) && participants.length() == 10) {

      val metadata = gameData.getJSONObject("metadata");
      val gameId = metadata.getString("matchId");
      final JSONObject timelineObject = timelineJson.getJSONObject();
      final Map<Integer, JSONObject> playerInfo = timelineObject != null ? loadTimeline(timelineObject) : new HashMap<>();

      val gametype = Gametype.find((info.has("tournamentCode") && !info.isNull("tournamentCode")) ? (short) -1 : (short) queueId);
      val game = handleGame(info, gameId, gametype);
      gametype.addGame(game, gametype);

      createJsonTeams(participants, playerInfo);

      val fights = handleGameEvents(game);
      handleEventsForTeams();
      for (int i = 0; i < jsonTeams.size(); i++) {
        val jsonTeam = jsonTeams.get(i);
        val teams = info.getJSONArray("teams");
        jsonTeam.setTeamObject(teams.getJSONObject(i));
        if (jsonTeam.doesExist()) {
          val teamperformance = handleTeam(jsonTeam);
          val team = jsonTeam.getMostUsedTeam(queueType);
          game.addTeamperformance(teamperformance, team);
          handleTeamEvents(teamperformance);

          val players = determinePlayers(queueType, jsonTeam);
          players.forEach(player -> handlePlayer(player, teamperformance, fights));
        }
        determineBansAndPicks(teams.getJSONObject(i), i, game, participants);
      }
      System.out.println("Match erstellt!");
      return true;
    }
    System.err.println("Match entfernt");
    return false;
  }

  private static void createJsonTeams(JSONArray participants, Map<Integer, JSONObject> playerInfo) {
    jsonTeams = Arrays.asList(new JSONTeam(1), new JSONTeam(2));
    for (int i = 0; i < participants.length(); i++) {
      val participant = participants.getJSONObject(i);
      val puuid = participant.getString("puuid");
      final int teamId = participant.getInt("teamId");
      val jsonPlayer = new JSONPlayer(i, participant, puuid, teamId == 100, highestMinute);
      val team = JSONTeam.getTeam(teamId);
      for (int timestamp : playerInfo.keySet()) {
        int minute = timestamp / 60_000;
        val frame = playerInfo.get(timestamp);
        val infoStats = frame.getJSONObject(String.valueOf(jsonPlayer.getId() + 1));
        jsonPlayer.addInfo(infoStats, minute);
      }

      if (team != null) {
        team.addPlayer(jsonPlayer);
      }
    }
  }

  private static void handleEventsForTeams() {
    for (JSONObject event : allEvents) {
      for (int pId : getPlayersOfEvent(event)) {
        final JSONPlayer player = JSONPlayer.getPlayer(pId - 1);
        if (player != null) {
          player.addEvent(event);
        }
      }

      final int tId = getTeamOfEvent(event);
      val team = JSONTeam.getTeam(tId);
      if (team != null) {
        team.addEvent(event);
      }
    }
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

  private static void handlePlayer(JSONPlayer player, Teamperformance teamperformance, List<Fight> fights) {
    val enemyPlayer = player.getEnemy();
    val performance = handlePerformance(player, enemyPlayer);
    val account = (player.isListed()) ? player.getAccount() : Account.get(RiotAccountURLGenerator.fromPuuid(player.get(StoredStat.PUUID)));
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

      handlePlayerEvents(player, playerperformance);
      handlePlayerInfo(player, playerperformance);

      RiotStatRequester.handlePlayerStats(playerperformance, teamperformance, jsonTeams, player, fights);
    }
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
    final Byte visionScore = e != null ? (byte) (p.getSmall(StoredStat.VISION_SCORE) - e.getSmall(StoredStat.VISION_SCORE)) :
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
    if (p.has(StoredStat.SPELL_LANDED)) playerperformance.setSpellsHit(p.getSmall(StoredStat.SPELL_LANDED));
    if (p.has(StoredStat.SPELL_DODGE)) playerperformance.setSpellsDodged(p.getSmall(StoredStat.SPELL_DODGE));
    if (p.has(StoredStat.SPELL_DODGE_QUICK)) playerperformance.setQuickDodged(p.getSmall(StoredStat.SPELL_DODGE_QUICK));
    if (p.has(StoredStat.SOLO_KILLS)) playerperformance.setSoloKills(p.getTiny(StoredStat.SOLO_KILLS));
    if (p.has(StoredStat.LEVELUP_TAKEDOWNS)) playerperformance.setLevelUpAllin(p.getTiny(StoredStat.LEVELUP_TAKEDOWNS));
    if (p.has(StoredStat.AGGRESSIVE_FLASH)) playerperformance.setFlashAggressive(p.getTiny(StoredStat.AGGRESSIVE_FLASH));
    if (p.has(StoredStat.TELEPORT_KILLS)) playerperformance.setTeleportKills(p.getTiny(StoredStat.TELEPORT_KILLS));
    if (p.has(StoredStat.IMMOBILIZATIONS)) playerperformance.setImmobilizations(p.getSmall(StoredStat.IMMOBILIZATIONS));
    if (p.has(StoredStat.CONTROL_WARDS_UPTIME))
      playerperformance.setControlWardUptime((short) (p.getSmall(StoredStat.CONTROL_WARDS_UPTIME) * 60));
    if (p.has(StoredStat.WARDS_GUARDED)) playerperformance.setWardsGuarded(p.getTiny(StoredStat.WARDS_GUARDED));
    if (p.has(StoredStat.FIRST_TOWER_TIME) && (p.getBool(StoredStat.FIRST_TOWER) || p.getBool(StoredStat.FIRST_TOWER_ASSIST))) {
      playerperformance.setFirstturretAdvantage(p.getSmall(StoredStat.FIRST_TOWER_TIME));
    } else if (e != null && e.has(StoredStat.FIRST_TOWER_TIME) && (e.getBool(StoredStat.FIRST_TOWER) || e.getBool(StoredStat.FIRST_TOWER_ASSIST))) {
      playerperformance.setFirstturretAdvantage((short) (e.getSmall(StoredStat.FIRST_TOWER_TIME) * -1));
    }
    if (e != null && e.has(StoredStat.BARON_EXECUTES)) playerperformance.setBaronExecutes(e.getTiny(StoredStat.BARON_EXECUTES));
    if (p.has(StoredStat.BUFFS_STOLEN)) playerperformance.setBuffsStolen(p.getTiny(StoredStat.BUFFS_STOLEN));
    if (p.has(StoredStat.SCUTTLES_INITIAL)) playerperformance.setInitialScuttles(p.getTiny(StoredStat.SCUTTLES_INITIAL));
    if (p.has(StoredStat.SCUTTLES_TOTAL)) playerperformance.setTotalScuttles(p.getTiny(StoredStat.SCUTTLES_TOTAL));
    if (p.has(StoredStat.TOWERS_SPLITPUSHED)) playerperformance.setSplitpushedTurrets(p.getTiny(StoredStat.TOWERS_SPLITPUSHED));
    if (p.has(StoredStat.INVADING_KILLS)) playerperformance.setTeamInvading(p.getTiny(StoredStat.INVADING_KILLS));
    if (p.has(StoredStat.JUNGLER_ROAMS)) playerperformance.setGanksEarly(p.getTiny(StoredStat.LANER_ROAMS, StoredStat.JUNGLER_ROAMS));
    if (p.has(StoredStat.DIVES_PROTECTED)) playerperformance.setDivesDone(handleDives(p, e, StoredStat.DIVES_DONE, StoredStat.DIVES_PROTECTED));
    if (p.has(StoredStat.DIVES_DONE)) {
      playerperformance.setDivesSuccessful(p.getTiny(StoredStat.DIVES_DONE));
      playerperformance.setDivesGotten(handleDives(p, e, StoredStat.DIVES_PROTECTED, StoredStat.DIVES_DONE));
    }
    if (p.has(StoredStat.DIVES_PROTECTED)) playerperformance.setDivesProtected(p.getTiny(StoredStat.DIVES_PROTECTED));
    if (p.has(StoredStat.BOUNTY_GOLD)) {
      playerperformance.setBountyGold((short) (p.getSmall(StoredStat.BOUNTY_GOLD) -
          (e != null && e.getSmall(StoredStat.BOUNTY_GOLD) == null ? 0 : p.getSmall(StoredStat.BOUNTY_GOLD))));
    }
    if (p.has(StoredStat.CREEP_SCORE_LANE_EARLY) || p.has(StoredStat.CREEP_SCORE_JUNGLE_EARLY))  {
      playerperformance.setCreepsEarly(p.getTiny(StoredStat.CREEP_SCORE_LANE_EARLY, StoredStat.CREEP_SCORE_JUNGLE_EARLY));
    }
    if (p.has(StoredStat.CREEP_INVADED)) playerperformance.setCreepsInvade(p.getSmall(StoredStat.CREEP_INVADED));
    if (p.has(StoredStat.TOWERS_PLATES)) playerperformance.setTurretplates(p.getTiny(StoredStat.TOWERS_PLATES));
    if (p.has(StoredStat.CREEP_SCORE_ADVANTAGE)) playerperformance.setFlamehorizonAdvantage(p.getTiny(StoredStat.CREEP_SCORE_ADVANTAGE));
    final short mejaisCompleted = (short) ((p.getSmall(StoredStat.MEJAIS_TIME) == null ? 0 : p.getSmall(StoredStat.MEJAIS_TIME))
        - (e != null && e.getSmall(StoredStat.MEJAIS_TIME) != null ? e.getSmall(StoredStat.MEJAIS_TIME) : 0));
    if (mejaisCompleted != 0) playerperformance.setMejaisCompleted(mejaisCompleted);
    if (p.has(StoredStat.OUTPLAYED)) playerperformance.setOutplayed(p.getTiny(StoredStat.OUTPLAYED));
    if (p.has(StoredStat.DRAGON_TAKEDOWNS)) playerperformance.setDragonTakedowns(p.getTiny(StoredStat.DRAGON_TAKEDOWNS));
    if (p.has(StoredStat.LEGENDARY_FASTEST)) playerperformance.setFastestLegendary(p.getSmall(StoredStat.LEGENDARY_FASTEST));
    if (p.has(StoredStat.GANK_SETUP)) playerperformance.setGankSetups(p.getTiny(StoredStat.GANK_SETUP));
    if (p.has(StoredStat.BUFFS_INITIAL)) playerperformance.setInitialBuffs(p.getTiny(StoredStat.BUFFS_INITIAL));
    if (p.has(StoredStat.KILLS_EARLY_JUNGLER) || p.has(StoredStat.KILLS_EARLY_LANER)) {
      playerperformance.setEarlyKills(p.getTiny(StoredStat.KILLS_EARLY_JUNGLER, StoredStat.KILLS_EARLY_LANER));
    }
    if (p.has(StoredStat.OBJECTIVES_JUNGLERKILL)) playerperformance.setJunglerKillsAtObjective(p.getTiny(StoredStat.OBJECTIVES_JUNGLERKILL));
    if (p.has(StoredStat.AMBUSH)) playerperformance.setAmbush(p.getTiny(StoredStat.AMBUSH));
    if (p.has(StoredStat.TOWERS_EARLY)) playerperformance.setEarlyTurrets(p.getTiny(StoredStat.TOWERS_EARLY));
    if (p.has(StoredStat.EXPERIENCE_ADVANTAGE)) playerperformance.setLevelLead(p.getTiny(StoredStat.EXPERIENCE_ADVANTAGE));
    if (p.has(StoredStat.PICK_KILL)) playerperformance.setPicksMade(p.getTiny(StoredStat.PICK_KILL));
    if (p.has(StoredStat.ASSASSINATION)) playerperformance.setAssassinated(p.getTiny(StoredStat.ASSASSINATION));
    if (p.has(StoredStat.GUARD_ALLY)) playerperformance.setSavedAlly(p.getTiny(StoredStat.GUARD_ALLY));
    byte survived = 0;
    if (p.has(StoredStat.SURVIVED_CLOSE)) survived += p.getTiny(StoredStat.SURVIVED_CLOSE);
    if (p.has(StoredStat.SURVIVED_HIGH_DAMAGE)) survived += p.getTiny(StoredStat.SURVIVED_HIGH_DAMAGE);
    if (p.has(StoredStat.SURVIVED_HIGH_CROWDCONTROL)) survived += p.getTiny(StoredStat.SURVIVED_HIGH_CROWDCONTROL);
    playerperformance.setSurvivedClose(survived);
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

  @NotNull
  private static List<Integer> getPlayersOfEvent(JSONObject allEvent) {
    val partIds = new ArrayList<Integer>();
    if (allEvent.has("participantId")) {
      partIds.add(allEvent.getInt("participantId"));
    }
    if (allEvent.has("killerId")) {
      partIds.add(allEvent.getInt("killerId"));
    }
    if (allEvent.has("victimId")) {
      partIds.add(allEvent.getInt("victimId"));
    }
    if (allEvent.has("assistingParticipantIds")) {
      partIds.addAll(allEvent.getJSONArray("assistingParticipantIds").toList().stream()
          .map(id -> (Integer) id).collect(Collectors.toList()));
    }
    if (allEvent.has("creatorId")) {
      partIds.add(allEvent.getInt("creatorId"));
    }
    return partIds;
  }

  private static int getTeamOfEvent(JSONObject allEvent) {
    if (allEvent.has("teamId")) {
      return allEvent.getInt("teamId");

    } else if (allEvent.has("killerTeamId")) {
      return allEvent.getInt("killerTeamId");

    }
    return 0;
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

    final int totalDamage = jsonTeam.getSum(StoredStat.DAMAGE_TOTAL);
    final int damageTaken = jsonTeam.getSum(StoredStat.DAMAGE_TAKEN);
    final int damageMitigated = jsonTeam.getSum(StoredStat.DAMAGE_MITIGATED);
    final Integer immobilizations = jsonTeam.getSum(StoredStat.IMMOBILIZATIONS);
    final int vision = jsonTeam.getSum(StoredStat.VISION_SCORE);
    final int totalGold = jsonTeam.getSum(StoredStat.GOLD_TOTAL);
    final int totalCs = jsonTeam.getSum(StoredStat.CREEP_SCORE_LANE) + jsonTeam.getSum(StoredStat.CREEP_SCORE_JUNGLE);
    final Integer earliestDragon = jsonTeam.getSum(StoredStat.DRAGON_TIME);
    final Integer atSpawn = jsonTeam.getSum(StoredStat.OBJECTIVES_ON_SPAWN);
    final Integer nearJgl = jsonTeam.getSum(StoredStat.OBJECTIVES_50_50);
    final Integer quest = jsonTeam.getSum(StoredStat.QUEST_FAST);
    final Integer herald = jsonTeam.getSum(StoredStat.RIFT_TURRETS_MULTI);
    final Integer acetime = jsonTeam.getMin(StoredStat.ACE_TIME);
    final Integer killDeficit = jsonTeam.getSum(StoredStat.KILLS_DISADVANTAGE);

    final Teamperformance teamperformance = new Teamperformance(teamId == 100, win, totalDamage, damageTaken,
        totalGold, totalCs, champion.getInt("kills"), tower.getInt("kills"), dragon.getInt("kills"), inhibitor.getInt("kills"),
        riftHerald.getInt("kills"), baron.getInt("kills"), tower.getBoolean("first"), dragon.getBoolean("first"));
    final JSONPlayer jsonPlayer = jsonTeam.getAllPlayers().get(0);
    if (jsonPlayer.has(StoredStat.PERFECT_SOUL)) teamperformance.setPerfectSoul(jsonPlayer.getMedium(StoredStat.PERFECT_SOUL) == 1);
    if (jsonPlayer.has(StoredStat.SURRENDER)) teamperformance.setSurrendered(jsonPlayer.getBool(StoredStat.SURRENDER));
    if (jsonPlayer.has(StoredStat.RIFT_TURRETS)) teamperformance.setRiftTurrets(jsonPlayer.getSmall(StoredStat.RIFT_TURRETS) / 5.0);
    if (jsonPlayer.has(StoredStat.ELDER_TIME)) teamperformance.setElderTime(jsonPlayer.getSmall(StoredStat.ELDER_TIME));
    if (jsonPlayer.has(StoredStat.BARON_POWERPLAY)) teamperformance.setElderTime(jsonPlayer.getSmall(StoredStat.BARON_POWERPLAY));
    if (jsonPlayer.has(StoredStat.ACE_EARLY)) teamperformance.setEarlyAces(jsonPlayer.getTiny(StoredStat.ACE_EARLY));
    if (jsonPlayer.has(StoredStat.BARON_TIME)) teamperformance.setBaronTime(jsonPlayer.getTiny(StoredStat.BARON_TIME));
    if (earliestDragon != null) teamperformance.setFirstDragonTime((short) (int) earliestDragon);
    if (atSpawn != null) teamperformance.setObjectiveAtSpawn((byte) (int) atSpawn);
    if (nearJgl != null) teamperformance.setObjectiveContests((byte) (int) nearJgl);
    if (quest != null) teamperformance.setQuestCompletedFirst(quest > 0);
    if (jsonPlayer.has(StoredStat.INHIBITORS_TAKEN)) teamperformance.setInhibitorsTime(jsonPlayer.getSmall(StoredStat.INHIBITORS_TAKEN));
    if (jsonPlayer.has(StoredStat.ACE_FLAWLESS)) teamperformance.setFlawlessAces(jsonPlayer.getTiny(StoredStat.ACE_FLAWLESS));
    if (herald != null) teamperformance.setRiftOnMultipleTurrets((byte) (int) herald);
    if (acetime != null) teamperformance.setFastestAcetime((short) (int) acetime);
    if (killDeficit != null) teamperformance.setKillDeficit((byte) (int) killDeficit);
    teamperformance.setVision((short) vision);
    if (immobilizations != null) teamperformance.setImmobilizations((short) (int) immobilizations);
    teamperformance.setDamageMitigated(damageMitigated);


    return teamperformance;
  }

  private static Game handleGame(JSONObject info, String gameId, Gametype gametype) {
    final long startMillis = info.getLong("gameStartTimestamp");
    val start = new Date(startMillis);
    final long endMillis = info.getLong("gameStartTimestamp");
    final short duration = (short) (endMillis - startMillis / 1000);
    return Game.get(new Game(gameId, start, duration), gametype);
  }

  private static HashMap<Integer, JSONObject> loadTimeline(JSONObject timeLineObject) {
    val playerInfo = new HashMap<Integer, JSONObject>();
    val timelineInfo = timeLineObject.getJSONObject("info");
    val timelineContent = timelineInfo.getJSONArray("frames");
    // Each minute
    for (int i = 0; i < timelineContent.length(); i++) {
      val frameObject = timelineContent.getJSONObject(i);
      val eventArray = frameObject.getJSONArray("events");
      for (int j = 0; j < eventArray.length(); j++) {
        val event = eventArray.getJSONObject(j);
        final String typeString = event.getString("type");
        if (Arrays.stream(EventTypes.values()).anyMatch(type2 -> type2.name().equals(typeString))) {
          allEvents.add(event);

        }
      }

      if (frameObject.has("participantFrames") && !frameObject.isNull("participantFrames")) {
        final int timestamp = frameObject.getInt("timestamp");
        playerInfo.put(timestamp, frameObject.getJSONObject("participantFrames"));
        final int minute = timestamp / 60_000;
        if (highestMinute < minute) {
          highestMinute = minute;
        }
      }
    }

    return playerInfo;
  }

  private static List<Fight> handleGameEvents(Game game) {
    for (JSONObject event : allEvents) {
      val type = EventTypes.valueOf(event.getString("type"));
      final int timestamp = event.getInt("timestamp");
      if (type.equals(EventTypes.PAUSE_START)) {
        handlePauseStart(timestamp, game);

      } else if (type.equals(EventTypes.PAUSE_END)) {
        handlePauseEnd(timestamp, game);

      }
    }
    val kills = allEvents.stream()
        .map(Kill::getKillFromEvent)
        .filter(Objects::nonNull)
        .sorted(Comparator.comparingInt(Kill::getTimestamp))
        .collect(Collectors.toCollection(ArrayList::new));

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

  private static void handlePauseStart(int timestamp, Game game) {
    if (game.getNotOpened().isEmpty()) {
      game.addPause(new GamePause(timestamp, 0));
    } else {
      game.getNotOpened().get(0).setStart(timestamp);
    }
  }

  private static void handlePauseEnd(int timestamp, Game game) {
    if (timestamp > 0) {
      if (game.getNotClosed().isEmpty()) {
        game.addPause(new GamePause(0, timestamp));
      } else {
        game.getNotClosed().get(0).setEnd(timestamp);
      }
    }
  }

  private static void handleTeamEvents(Teamperformance teamperformance) {
    for (JSONObject event : allEvents) {
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

  private static void handlePlayerEvents(JSONPlayer player, Playerperformance playerperformance) {
    val items = IntStream.range(1, 8).mapToObj(i -> player.getMedium(StoredStat.valueOf("ITEM_" + i)))
        .filter(itemId -> itemId != null && itemId != 0).collect(Collectors.toList());
    for (JSONObject event : allEvents) {
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

    for (JSONObject event : player.getEvents(EventTypes.CHAMPION_SPECIAL_KILL)) {
      val role = handleEventOfPlayer(player, event);
      if (role != null) {
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

  private static void handlePlayerInfo(JSONPlayer player, Playerperformance playerperformance) {
    player.getInfos().stream()
        .filter(Objects::nonNull)
        .mapToInt(object -> player.getInfos().indexOf(object))
        .mapToObj(minute -> getPlayerperformanceInfo(player, minute))
        .forEach(playerperformance::addInfo);
  }

  private static PlayerperformanceInfo getPlayerperformanceInfo(JSONPlayer player, int minute) {
    final JSONObject infoStats = getEventObject(player, minute);
    if (infoStats == null) return null;

    val positionObject = infoStats.getJSONObject("position");
    val position = new Position(positionObject.getInt("x"), positionObject.getInt("y"));
    final int xp = player.getStatAt(minute, TimelineStat.EXPERIENCE);
    final int totalGold = player.getStatAt(minute, TimelineStat.TOTAL_GOLD);
    final int currentGold = player.getStatAt(minute, TimelineStat.CURRENT_GOLD);
    final double enemyControlled = player.getStatAt(minute, TimelineStat.ENEMY_CONTROLLED) / 1000.0;
    final int lead = player.getLeadAt(minute, TimelineStat.LEAD);
    final int creepScore = player.getStatAt(minute, TimelineStat.CREEP_SCORE);
    final int damageToChampions = player.getStatAt(minute, TimelineStat.DAMAGE);
    final int maxHealth = player.getStatAt(minute, TimelineStat.TOTAL_HEALTH);
    final int currentHealth = player.getStatAt(minute, TimelineStat.CURRENT_HEALTH);
    final int maxResource = player.getStatAt(minute, TimelineStat.TOTAL_RESOURCE);
    final int currentResource = player.getStatAt(minute, TimelineStat.CURRENT_RESOURCE);
    final int moveSpeed = player.getStatAt(minute, TimelineStat.MOVEMENT_SPEED);
    return new PlayerperformanceInfo((short) minute, totalGold, (short) currentGold, enemyControlled, position, xp, (short) lead,
        (short) creepScore, damageToChampions, (short) maxHealth, (short) currentHealth, (short) maxResource, (short) currentResource,
        (short) moveSpeed);
  }

  @Nullable
  private static JSONObject getEventObject(JSONPlayer player, int minute) {
    final JSONObject infoStats;
    if (player.getInfos().size() > minute) {
      infoStats = player.getInfos().get(minute);
    } else if (!player.getInfos().isEmpty()) {
      infoStats = player.getLastInfo();
    } else {
      return null;
    }
    return infoStats;
  }

  private static void handleSummonerspells(JSONPlayer player, Playerperformance performance) {
    performance.addSummonerspell(Summonerspell.find(player.getTiny(StoredStat.SUMMONER1_ID)), player.getTiny(StoredStat.SUMMONER1_AMOUNT));
    performance.addSummonerspell(Summonerspell.find(player.getTiny(StoredStat.SUMMONER2_ID)), player.getTiny(StoredStat.SUMMONER2_AMOUNT));
  }
}
