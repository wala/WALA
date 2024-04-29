package com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("requires-Internet")
public class FieldBasedCGGamesTest extends AbstractFieldBasedTest {

  @Test
  public void testBunnyHunt() throws IOException, WalaException, Error, CancelException {
    URL url = getUrl("http://www.themaninblue.com/experiment/BunnyHunt/");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC);
  }

  private static URL getUrl(String urlStr) throws MalformedURLException {
    try {
      return new URI(urlStr).toURL();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Disabled("seems to break with http issues")
  @Test
  public void testBeslimed() throws IOException, WalaException, Error, CancelException {
    URL url = getUrl("http://www.markus-inger.de/test/game.php");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC);
  }

  @Disabled("seems to break with http issues")
  @Test
  public void testDiggAttack() throws IOException, WalaException, Error, CancelException {
    URL url = getUrl("http://www.pixastic.com/labs/digg_attack/");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC);
  }

  @Disabled
  @Test
  public void testRiverRaider() throws IOException, WalaException, Error, CancelException {
    URL url =
        getUrl(
            "http://playstar.mobi/games/riverraider/index.html?playerId=&gameId=8&highscore=102425");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC_WORKLIST);
  }

  @Disabled("fails with \"timed out\" CancelException")
  @Test
  public void testSolitaire() throws IOException, WalaException, Error, CancelException {
    URL url = getUrl("http://www.inmensia.com/files/solitaire1.0.html");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC);
  }

  @Test // (expected = CancelException.class)
  public void testWorldOfSolitaire() throws IOException, WalaException, Error, CancelException {
    URL url = getUrl("http://worldofsolitaire.com/");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC);
  }

  @Test
  public void testMinesweeper() throws IOException, WalaException, Error, CancelException {
    URL url = getUrl("http://www.inmensia.com/files/minesweeper1.0.html");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC_WORKLIST);
  }

  @Test
  public void testProtoRPG() throws IOException, WalaException, Error, CancelException {
    URL url = getUrl("http://www.protorpg.com/games/protorpg/?game=prologue");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC_WORKLIST);
  }

  @Test
  public void testBattleship() throws IOException, WalaException, Error, CancelException {
    URL url = getUrl("http://www.sinkmyship.com/battleship/single.html");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC_WORKLIST);
  }
}
