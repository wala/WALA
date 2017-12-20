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
package com.ibm.wala.dataflow.IFDS;

import java.util.Iterator;

import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NumberedGraph;

/**
 * 
 * A supergraph as defined by Reps, Horwitz, and Sagiv POPL95
 * <p>
 * In our implementation we don't require explicit entry and exit nodes. So, the first basic block in a method is implicitly the
 * entry node, but might also be a call node too. Similarly for exit nodes. The solver is coded to deal with this appropriately.
 * <p>
 * Additionally, due to exceptional control flow, each method might have multiple exits or multiple entries.
 * 
 * T type of node in the supergraph P type of a procedure (like a box in an RSM)
 */
public interface ISupergraph<T, P> extends NumberedGraph<T> {

  public static final byte CALL_EDGE = 0;

  public static final byte RETURN_EDGE = 1;

  public static final byte CALL_TO_RETURN_EDGE = 2;

  public static final byte OTHER = 3;

  /**
   * @return the graph of procedures (e.g. a call graph) over which this supergraph is induced.
   */
  Graph<? extends P> getProcedureGraph();

  /**
   * @param n a node in this supergraph
   * @return true iff this node includes a call.
   */
  boolean isCall(T n);

  /**
   * @param call a "call" node in the supergraph
   * @return an Iterator of nodes that are targets of this call.
   */
  Iterator<? extends T> getCalledNodes(T call);

  /**
   * @param call a "call" node in the supergraph
   * @return an Iterator of nodes that are normal (non-call) successors of this call. This should only apply to backwards problems,
   *         where we might have, say, a call and a goto flow into a return site.
   */
  Iterator<T> getNormalSuccessors(T call);

  /**
   * @param call a "call" node in the supergraph
   * @param callee a "called" "procedure" in the supergraph. if callee is null, answer return sites for which no callee was found.
   * @return the corresponding return nodes. There may be many, because of exceptional control flow.
   */
  Iterator<? extends T> getReturnSites(T call, P callee);

  /**
   * @param ret a "return" node in the supergraph
   * @param callee a "called" "procedure" in the supergraph. if callee is null, answer return sites for which no callee was found.
   * @return the corresponding call nodes. There may be many.
   */
  Iterator<? extends T> getCallSites(T ret, P callee);

  /**
   * @param n a node in the supergraph
   * @return true iff this node is an exit node
   */
  boolean isExit(T n);

  /**
   * @param n a node in the supergraph
   * @return an object which represents the procedure which contains n
   */
  P getProcOf(T n);

  /**
   * @return the blocks in the supergraph that represents entry nodes for procedure p
   */
  T[] getEntriesForProcedure(P procedure);

  /**
   * @return the blocks in the supergraph that represents exit nodes for procedure p
   */
  T[] getExitsForProcedure(P procedure);

  /**
   * @param procedure an object that represents a procedure
   * @return the number of blocks from this procedure in this supergraph
   */
  int getNumberOfBlocks(P procedure);

  /**
   * @param n a node in the supergraph
   * @return the "logical" basic block number of n in its procedure
   */
  int getLocalBlockNumber(T n);

  /**
   * @param procedure an object that represents a procedure
   * @param i the "logical" basic block number of a node in the procedure
   * @return the corresponding node in the supergraph
   */
  T getLocalBlock(P procedure, int i);

  /**
   * @param n a node in this supergraph
   * @return true iff this node is a return site.
   */
  boolean isReturn(T n);

  /**
   * @return true iff this node is an entry node s_p for a procedure
   */
  boolean isEntry(T n);

  /**
   * @param src node in the supergraph
   * @param dest a successor of src in the supergraph
   * @return one of CALL_EDGE, RETURN_EDGE, CALL_TO_RETURN_EDGE, or OTHER
   */
  byte classifyEdge(T src, T dest);

}
