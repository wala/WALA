package com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import java.io.IOException;
import java.net.URL;
import org.junit.Ignore;
import org.junit.Test;

public class FieldBasedCGGamesTest extends AbstractFieldBasedTest {

  @Test
  public void testBunnyHunt() throws IOException, WalaException, Error, CancelException {
    URL url = new URL("http://www.themaninblue.com/experiment/BunnyHunt/");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC);
  }

  @Ignore("seems to break with http issues")
  @Test
  public void testBeslimed() throws IOException, WalaException, Error, CancelException {
    URL url = new URL("http://www.markus-inger.de/test/game.php");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC);
  }

  @Ignore("seems to break with http issues")
  @Test
  public void testDiggAttack() throws IOException, WalaException, Error, CancelException {
    URL url = new URL("http://www.pixastic.com/labs/digg_attack/");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC);
  }

  @Ignore
  @Test
  public void testRiverRaider() throws IOException, WalaException, Error, CancelException {
    URL url =
        new URL(
            "http://playstar.mobi/games/riverraider/index.html?playerId=&gameId=8&highscore=102425");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC_WORKLIST);
  }

  @Ignore("fails with \"timed out\" CancelException")
  @Test
  public void testSolitaire() throws IOException, WalaException, Error, CancelException {
    URL url = new URL("http://www.inmensia.com/files/solitaire1.0.html");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC);
  }

  @Test // (expected = CancelException.class)
  public void testWorldOfSolitaire() throws IOException, WalaException, Error, CancelException {
    URL url = new URL("http://worldofsolitaire.com/");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC);
  }

  @Test
  public void testMinesweeper() throws IOException, WalaException, Error, CancelException {
    URL url = new URL("http://www.inmensia.com/files/minesweeper1.0.html");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC_WORKLIST);
  }

  @Test
  public void testProtoRPG() throws IOException, WalaException, Error, CancelException {
    URL url = new URL("http://www.protorpg.com/games/protorpg/?game=prologue");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC_WORKLIST);
  }

  @Test
  public void testBattleship() throws IOException, WalaException, Error, CancelException {
    URL url = new URL("http://www.sinkmyship.com/battleship/single.html");
    runTest(url, new Object[][] {}, BuilderType.OPTIMISTIC_WORKLIST);
  }
}
