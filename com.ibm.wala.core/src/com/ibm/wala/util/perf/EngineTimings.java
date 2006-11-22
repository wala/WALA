/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.perf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.emf.ecore.EObject;

import com.ibm.wala.ecore.graph.EGraph;
import com.ibm.wala.ecore.perf.EPhaseTiming;
import com.ibm.wala.ecore.perf.PerfFactory;
import com.ibm.wala.emf.wrappers.EObjectGraphImpl;
import com.ibm.wala.emf.wrappers.EUtil;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.warnings.WalaException;

/**
 * 
 * A utility class to help monitor timing of analysis engines
 * 
 * @author sfink
 */
public class EngineTimings {

  /**
   * The max number of events we will record for a single thread
   */
  private static final int MAX_EVENTS = 1000000;

  /**
   * Maintain an arraylist of records for each thread
   */
  static ThreadLocal<ArrayList<Record>> myBuffer = new ThreadLocal<ArrayList<Record>>();

  // TODO: make this a property!
  private static final String FILENAME = "c:/temp/perf.xml";


  /**
   * Record a "start" event for a particular "virtual engine". Use with extreme
   * care.
   * 
   * @param engine
   */
  public static void startVirtual(String engine) {
    ArrayList<Record> list = getMyBuffer();
    if (list.size() >= MAX_EVENTS) {
      // give up
      return;
    }
    list.add(new Record(engine, true, System.currentTimeMillis()));
  }


  /**
   * Record a "finish" event for a particular "virtual engine". Use with extreme
   * care.
   * 
   * @param engine
   */
  public static void finishVirtual(String engine) {
    ArrayList<Record> list = getMyBuffer();
    if (list.size() >= MAX_EVENTS) {
      // give up
      return;
    }
    list.add(new Record(engine, false, System.currentTimeMillis()));
  }

  private static ArrayList<Record> getMyBuffer() {
    ArrayList<Record> list = myBuffer.get();
    if (list == null) {
      list = new ArrayList<Record>();
      myBuffer.set(list);
    }
    return list;
  }

  /**
   * A record for an individual call to start or stop
   */
  private final static class Record {
    final String engineName;

    final boolean isStart;

    final long milliClock;

    Record(String engineName, boolean isStart, long milliClock) {
      this.engineName = engineName;
      this.isStart = isStart;
      this.milliClock = milliClock;
    }
  }

  /**
   * dump a timing report to stdout. TODO: generalize this.
   * 
   * @throws WalaException
   */
  public static void report() throws WalaException {
    ArrayList<Record> list = getMyBuffer();
    if (list.size() >= MAX_EVENTS) {
      System.out.println("Too many events reported");
      return;
    }

    List<Phase> phases = collateTimings(list);

    for (Iterator<Phase> it = phases.iterator(); it.hasNext();) {
      Phase p = it.next();
      String pad = makePadding(p.depth);
      System.out.println(pad + p.name + " " + p.millis);
    }
  }
  /**
   * dump a timing report to file. TODO: generalize this.
   * 
   * @throws WalaException
   */
  public static void reportToFile() throws WalaException {
    ArrayList<Record> list = getMyBuffer();
    if (list.size() >= MAX_EVENTS) {
      System.out.println("Too many events reported");
      return;
    }
    
    List<Phase> phases = collateTimings(list);

    EGraph g = makePhaseTree(phases);
    saveToFile(g);
  }

  private static void saveToFile(EGraph g) throws WalaException {
    HashSet<EObject> s = new HashSet<EObject>();
    s.add(g);
    s.add(g.getNodes());

    EUtil.saveToFile(s, FILENAME);
  }

  /**
   * @param phases
   * @return EGraph<EPhaseTiming>
   */
  private static EGraph makePhaseTree(List<Phase> phases) {

    Phase root = phases.get(0);
    EObjectGraphImpl g = new EObjectGraphImpl();
    EPhaseTiming r = make(root);
    g.addNode(r);
    Stack<Phase> s = new Stack<Phase>();
    s.push(root);

    // mapping from phase -> EPhaseTiming
    Map<Phase, EPhaseTiming> phase2Emf = new HashMap<Phase, EPhaseTiming>();
    phase2Emf.put(root, r);

    // map: phase -> number of children recorded so far
    Map<Phase, Integer> childCount = new HashMap<Phase, Integer>();
    for (Iterator<Phase> it = phases.iterator(); it.hasNext();) {
      childCount.put(it.next(), new Integer(0));
    }

    // run through the list of phases, simulating the stack as we go.
    for (int i = 1; i < phases.size(); i++) {
      Phase p = phases.get(i);
      EPhaseTiming ept = make(p);
      g.addNode(ept);
      phase2Emf.put(p, ept);

      Phase parent = s.peek();
      while (parent.depth != (p.depth - 1)) {
        s.pop();
        parent = s.peek();
      }
      Integer lastCount = childCount.get(parent);
      childCount.put(parent, new Integer(lastCount.intValue() + 1));
      ept.setOrder(lastCount.intValue());
      g.addEdge(phase2Emf.get(parent), ept);

      s.push(p);

    }

    addUnaccountedFor(g);

    return (EGraph) g.export();

  }

  private static void addUnaccountedFor(EObjectGraphImpl g) {
    Collection nodes = new Iterator2Collection<EObject>(g.iterateNodes());
    for (Iterator it = nodes.iterator(); it.hasNext();) {
      EPhaseTiming node = (EPhaseTiming) it.next();

      if (g.getSuccNodeCount(node) > 0) {
        long counted = node.getMillis();
        for (Iterator it2 = g.getSuccNodes(node); it2.hasNext();) {
          EPhaseTiming p = (EPhaseTiming) it2.next();
          counted -= p.getMillis();
        }
        if (counted > 0) {
          EPhaseTiming MIA = PerfFactory.eINSTANCE.createEPhaseTiming();
          MIA.setMillis(counted);
          MIA.setName("Unaccounted");
          MIA.setOrder(g.getSuccNodeCount(node));
          g.addNode(MIA);
          g.addEdge(node,MIA);
        }
      }
    }

  }

  /**
   * @param p
   * @return an EMF-based object representing the the phase information
   */
  private static EPhaseTiming make(Phase p) {
    EPhaseTiming result = PerfFactory.eINSTANCE.createEPhaseTiming();
    result.setMillis(p.millis);
    result.setName(p.name);
    return result;
  }

  /**
   * @param depth
   * @return a string of 'depth' spaces
   */
  private static String makePadding(int depth) {
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < depth; i++) {
      result.append(' ');
    }
    return result.toString();
  }

  /**
   * @param list
   * @return list<Phases>
   * @throws WalaException 
   */
  private static List<Phase> collateTimings(ArrayList<Record> list) throws WalaException {
    ArrayList<Phase> phases = new ArrayList<Phase>();
    int currentDepth = 0;
    long startMillis = list.get(0).milliClock;
    Phase root = new Phase("root", currentDepth++, startMillis);
    phases.add(root);
    Stack<Phase> pending = new Stack<Phase>();
    pending.push(root);

    for (Iterator<Record> it = list.iterator(); it.hasNext();) {
      Record r = it.next();
      if (r.isStart) {
        Phase p = new Phase(r.engineName, currentDepth++, r.milliClock);
        pending.push(p);
        phases.add(p);
      } else {
        Phase p = pending.pop();
        currentDepth--;
        p.millis = r.milliClock - p.millis;
      }
    }

    Phase p = pending.pop();
    if (p != root) {
      throw new WalaException("failed to time correctly: unbalanced start/finish");
    }
    long endMillis = list.get(list.size() - 1).milliClock;
    p.millis = endMillis - p.millis;
    return phases;
  }

  /**
   * @author sfink
   * 
   * A record holding timing information for a particular engine invocation
   */
  private static class Phase {
    String name;

    int depth;

    long millis;

    Phase(String name, int depth, long millis) {
      this.name = name;
      this.depth = depth;
      this.millis = millis;
    }
  }

}
