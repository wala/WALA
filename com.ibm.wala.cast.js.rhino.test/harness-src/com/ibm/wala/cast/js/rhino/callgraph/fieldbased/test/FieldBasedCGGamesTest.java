package com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.test.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

public class FieldBasedCGGamesTest extends AbstractFieldBasedTest {

  @Test
  public void testBunnyHunt() throws IOException, WalaException, Error, CancelException {
    runTestExceptOnTravis(new URL("http://www.themaninblue.com/experiment/BunnyHunt/"), new Object[][]{}, BuilderType.OPTIMISTIC);
  }
  
  @Test
  public void testBeslimed() throws IOException, WalaException, Error, CancelException {
    runTestExceptOnTravis(new URL("http://www.markus-inger.de/test/game.php"), new Object[][]{}, BuilderType.OPTIMISTIC);
  }

  // seems to break with http issues
  //@Test
  public void testDiggAttack() throws IOException, WalaException, Error, CancelException {
    runTestExceptOnTravis(new URL("http://www.pixastic.com/labs/digg_attack/"), new Object[][]{}, BuilderType.OPTIMISTIC);
  }

  @Test
  public void testRiverRaider() throws IOException, WalaException, Error, CancelException {
    runTestExceptOnTravis(new URL("http://playstar.mobi/games/riverraider/index.html?playerId=&gameId=8&highscore=102425"), new Object[][]{}, BuilderType.OPTIMISTIC_WORKLIST);
  }
 
  @Test
  public void testSolitaire() throws IOException, WalaException, Error, CancelException {
    runTestExceptOnTravis(new URL("http://www.inmensia.com/files/solitaire1.0.html"), new Object[][]{}, BuilderType.OPTIMISTIC);
  }
 
  @Test // (expected = CancelException.class)
  public void testWorldOfSolitaire() throws IOException, WalaException, Error, CancelException {
    runTestExceptOnTravis(new URL("http://worldofsolitaire.com/"), new Object[][]{}, BuilderType.OPTIMISTIC);
    if (System.getenv("TRAVIS") != null) {
      // fake it
      // throw new CancelException(null);
    }
  }
  
  @Test
  public void testMinesweeper() throws IOException, WalaException, Error, CancelException {
    runTestExceptOnTravis(new URL("http://www.inmensia.com/files/minesweeper1.0.html"), new Object[][]{}, BuilderType.OPTIMISTIC_WORKLIST);
  }
  
  @Test
  public void testProtoRPG() throws IOException, WalaException, Error, CancelException {
    runTestExceptOnTravis(new URL("http://www.protorpg.com/games/protorpg/?game=prologue"), new Object[][]{}, BuilderType.OPTIMISTIC_WORKLIST);
  }

  @Test
  public void testBattleship() throws IOException, WalaException, Error, CancelException {
    runTestExceptOnTravis(new URL("http://www.sinkmyship.com/battleship/single.html"), new Object[][]{}, BuilderType.OPTIMISTIC_WORKLIST);
  }

  
}
