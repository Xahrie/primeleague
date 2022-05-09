package de.xeri.league;

import java.util.Date;

import de.xeri.league.loader.GameLoader;
import de.xeri.league.models.league.Season;
import de.xeri.league.models.league.Team;
import de.xeri.league.util.Const;
import de.xeri.league.util.Data;
import de.xeri.league.util.logger.Logger;
import lombok.val;

/**
 * Created by Lara on 06.04.2022 for web
 */
public class Test {
  private static final Logger logger = Logger.getLogger("Test");

  public static void main(String[] args) {

    try {
      if (Const.check()) {
        final Date date = new Date();
        final Data data = Data.getInstance();
        Data.getInstance().getSession().createSQLQuery("SET SQL_MODE='ALLOW_INVALID_DATES';").executeUpdate();
        logger.info("Datenbank geladen");

        val team = Team.findTid(Const.TEAMID);
        data.setCurrentGroup(team.getLastLeague());
        logger.info("Gruppe geladen");

        val season = Season.current();
        data.setCurrentSeason(season);
        logger.info("Season geladen");

        // TODO success
        //PlayerLoader.load();
        GameLoader.load();
        data.commit();
        logger.info("Spiele geladen");

        data.getTransaction().commit();
        System.out.println((System.currentTimeMillis() - date.getTime())/1000.0 + " sec");
        System.out.println("TRUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUE");

      } else {
        logger.severe("Konstanten fehlerhaft!");
      }

    } catch (NullPointerException ex) {
      //TODO (Abgie) 30.04.2022: Remove after testing!!! -> Bad code
      ex.printStackTrace();
    }








  }
}
