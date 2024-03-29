package de.xeri.prm.servlet.loader.ourteam;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import de.xeri.prm.loader.MatchLoader;
import de.xeri.prm.manager.PrimeData;
import de.xeri.prm.models.league.Absence;
import de.xeri.prm.models.league.Player;
import de.xeri.prm.models.league.Schedule;
import de.xeri.prm.models.league.Team;
import de.xeri.prm.models.league.TurnamentMatch;
import de.xeri.prm.util.Const;
import de.xeri.prm.util.FacesUtil;
import de.xeri.prm.util.Util;
import lombok.Data;
import org.primefaces.component.timeline.TimelineUpdater;
import org.primefaces.event.timeline.TimelineAddEvent;
import org.primefaces.event.timeline.TimelineModificationEvent;
import org.primefaces.model.timeline.TimelineEvent;
import org.primefaces.model.timeline.TimelineGroup;
import org.primefaces.model.timeline.TimelineModel;

/**
 * Created by Lara on 02.06.2022 for web
 */
@ManagedBean
@SessionScoped
@Data
public class LoadWe implements Serializable {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final long serialVersionUID = 7301096097113313751L;

  private List<Player> players;
  private List<Team> scrimpartners;
  private List<Schedule> upcomingSchedules;
  private Schedule nextOrLast;

  private TimelineModel<Absence, String> model;  // current event to be changed, edited, deleted or added
  private TimelineEvent<Absence> event; // current event to be changed, edited, deleted or added
  private LocalDateTime start;
  private LocalDateTime end;

  private boolean editableTime = true;

  private List<TurnamentMatch> scrimteamMatches;

  @PostConstruct
  public void init() {
    PrimeData.getInstance(); // establish connection
    updateMatches();

    this.start = LocalDateTime.now().minusHours(24);
    this.end = LocalDateTime.now().plusHours(50);

    this.scrimpartners = Team.getScrimTeams();

    final Team team = Team.findTid(Const.TEAMID);
    this.players = new ArrayList<>(team.getPlayers());

    this.model = new TimelineModel<>();

    List<String> schedulingPlayers =
        Arrays.asList("SYSTEM", "Whitelizard", "Seven", "Alex", "Xahrie", "Nuklas", "Grakala", "Diluc", "Suders", "C3L3TI3", "Rebone", "KiKi");
    IntStream.range(0, schedulingPlayers.size()).forEach(i -> model.addGroup(new TimelineGroup<>("idx_" + i, schedulingPlayers.get(i))));


    for (Absence absence : Absence.get()) {
      final TimelineEvent<Absence> e = TimelineEvent.<Absence>builder()
          .id(absence.getIdx())
          .data(absence)
          .startDate(Util.getLocalDate(absence.getStart()))
          .endDate(Util.getLocalDate(absence.getEnd()))
          .group(absence.getPlayer())
          .editable(true)
          .build();
      model.add(e);
    }

    this.scrimteamMatches = scrimpartners.stream()
        .filter(t -> !PrimeData.getInstance().getCurrentGroup().getTeams().contains(t))
        .map(Team::getCurrentTurnamentMatches)
        .flatMap(Collection::stream)
        .sorted(Comparator.comparing(TurnamentMatch::getStart))
        .distinct().collect(Collectors.toList());
  }

  public void updateMatches() {
    final Team team = Team.findTid(Const.TEAMID);
    for (TurnamentMatch match : team.getTurnamentMatches()) {
      if (match.isRecently()) {
        MatchLoader.analyseMatchPage(match);
        PrimeData.getInstance().save(match);
        PrimeData.getInstance().commit();
      }
    }

    final List<Schedule> allSchedules = new ArrayList<>(Schedule.get());
    this.upcomingSchedules = allSchedules.stream()
        .filter(schedule -> schedule.getEndTime().after(new Date()))
        .sorted(Comparator.comparing(Schedule::getStartTime))
        .collect(Collectors.toList());

    this.nextOrLast = upcomingSchedules.isEmpty() ? allSchedules.get(allSchedules.size() - 1) : upcomingSchedules.get(0);

  }

  public void onChange(TimelineModificationEvent<Absence> e) {
    this.event = e.getTimelineEvent();
    try {
      handleEvent();
      model.update(event);
      FacesUtil.sendMessage(event.getData().getPlayer() + " aktualisiert", "");
    } catch (Exception exception) {
      FacesUtil.sendException(event.getData().getPlayer() + " konnte nicht gespeichert werden", exception);
    }
  }

  public void onEdit(TimelineModificationEvent<Absence> e) {
    event = e.getTimelineEvent();
  }

  public void onAdd(TimelineAddEvent e) {
    event = TimelineEvent.<Absence>builder()
        .id(e.getId())
        .data(new Absence())
        .startDate(e.getStartDate())
        .endDate(e.getEndDate())
        .group(e.getGroup())
        .editable(true)
        .build();
    model.add(event);
  }

  public void onDelete(TimelineModificationEvent<Absence> e) {
    event = e.getTimelineEvent();
    System.out.println("DELETE PRE");

    delete();
  }

  public void delete() {
    final Absence absence = Absence.find(event.getId());
    System.out.println("DELETE");
    PrimeData.getInstance().remove(absence);
    PrimeData.getInstance().commit();

    TimelineUpdater timelineUpdater = TimelineUpdater.getCurrentInstance(":form:timeline");
    model.delete(event, timelineUpdater);

    FacesUtil.sendMessage(event.getData().getPlayer() + " gelöscht", "");
  }

  public void saveDetails() {
    handleEvent();

    TimelineUpdater timelineUpdater = TimelineUpdater.getCurrentInstance(":form:timeline");
    model.update(event, timelineUpdater);

    FacesUtil.sendMessage(event.getData().getPlayer() + " gespeichert", "");
  }

  private void handleEvent() {
    System.out.println(event.getData().toString());

    final Absence data = event.getData();
    data.setIdx(event.getId());
    data.setStart(Util.getDate(event.getStartDate()));
    data.setEnd(Util.getDate(event.getEndDate() != null ? event.getEndDate() : event.getStartDate()));
    data.setPlayer(event.getGroup());
    event.setStyleClass(data.getType().name().toLowerCase());
    Absence.get(event.getData());
    PrimeData.getInstance().commit();
    System.out.println(event.getData().toString());
  }

}