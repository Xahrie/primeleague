package de.xeri.league.servlet.controller;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.xeri.league.models.match.Game;

/**
 * Created by Lara on 04.04.2022 for web
 */
@ManagedBean
@RequestScoped
public class MatchlistController {
  private Game game;

  public Game getGame() {
    return game;
  }

  public void setGame(Game game) {
    this.game = game;
  }

  public String doLookup() {
    game = Game.find("1");
    return "matchlist";
  }

  public String doLookup(String id) {
    game = Game.find(id);
    return "matchlist";
  }
}

