/*******************************************************************************
 * Copyright (c) 2002-2010 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.graph.traverse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

/**
 * Floyd-Warshall algorithm to compute all-pairs shortest path in graph with no negative cycles.
 * 
 * TODO: this API should be cleaned up.
 * 
 * @param <T> node type in the graph
 */
public class FloydWarshall<T> {

  public interface GetPath<T> {
    List<T> getPath(T from, T to);
  }

  public interface GetPaths<T> {
    Set<List<T>> getPaths(T from, T to);
  }

  protected final NumberedGraph<T> G;
  
  public FloydWarshall(NumberedGraph<T> g) {
    G = g;
  }

  protected int edgeCost() {
    return 1;
  }
  
  @SuppressWarnings("unused")
  protected void pathCallback(int i, int j, int k) {
    
  }
  
  public int[][] allPairsShortestPaths() {
    final int[][] result = new int[G.getNumberOfNodes()][G.getNumberOfNodes()];

    for(int i = 0; i < result.length; i++) {
      for(int j = 0; j < result[i].length; j++) {
        result[i][j] = Integer.MAX_VALUE;
      }
    }
    
    for(T from : G) {
      final int fn = G.getNumber(from);
      IntSet tos = G.getSuccNodeNumbers(from);
      tos.foreach(x -> result[fn][x] = edgeCost());
    }
    
    for(T kn : G) {
      int k = G.getNumber(kn);
      for(T in : G) {
        int i = G.getNumber(in);
        for(T jn : G) {
          int j = G.getNumber(jn);
          long newLen = (long)result[i][k] + (long)result[k][j];
          if (newLen <= result[i][j]) {
            pathCallback(i, j, k);
          }
          if (newLen < result[i][j]) {
            result[i][j] = (int)newLen;
          }
        }
      }
    }
    
    return result;
  }
  
  public static <T> int[][] shortestPathLengths(NumberedGraph<T> G) {
    return new FloydWarshall<>(G).allPairsShortestPaths();
  }
  
  public static <T> GetPath<T> allPairsShortestPath(final NumberedGraph<T> G) {
     return new FloydWarshall<T>(G) {
       int[][] next = new int[G.getNumberOfNodes()][G.getNumberOfNodes()];
       
       @Override
       protected void pathCallback(int i, int j, int k) {
         next[i][j] = k;
       }
       
       private GetPath<T> doit() {
         for(int i = 0; i < next.length; i++) {
           for(int j = 0; j < next[i].length; j++) {
             next[i][j] = -1;
           }
         }
         
         final int[][] paths = allPairsShortestPaths();
         return new GetPath<T>() {

           @Override
          public String toString() {
             String s = "";
             for(int i = 0; i <= G.getMaxNumber(); i++) {
               for(int j = 0; j <= G.getMaxNumber(); j++) {
                 try {
                   s += getPath(G.getNode(i), G.getNode(j));
                 } catch (UnsupportedOperationException e) {
                   
                 }
               }
             }
             return s;
           }

          @Override
          public List<T> getPath(T from, T to) {
            int fn = G.getNumber(from);
            int tn = G.getNumber(to);
            if (paths[fn][tn] == Integer.MAX_VALUE) {
              throw new UnsupportedOperationException("no path from " + from + " to " + to);
            } else {
              int intermediate = next[fn][tn];
              if (intermediate == -1) {
                return Collections.emptyList();
              } else {
                T in = G.getNode(intermediate);
                List<T> result = new LinkedList<>(getPath(from, in));
                result.add(in);
                result.addAll(getPath(in, to));
                return result;
              }
            }
          } 
         };
       }
     }.doit();
  }

  public static <T> GetPaths<T> allPairsShortestPaths(final NumberedGraph<T> G) {
    return new FloydWarshall<T>(G) {
      MutableIntSet[][] next = new MutableIntSet[G.getNumberOfNodes()][G.getNumberOfNodes()];

      @Override
      protected void pathCallback(int i, int j, int k) {
        if (next[i][j] == null) {
          next[i][j] = IntSetUtil.make();
        }
        next[i][j].add(k);
      }
      
      private GetPaths<T> doit() {        
        final int[][] paths = allPairsShortestPaths();
        return new GetPaths<T>() {
          
          @Override
         public String toString() {
            List<Set<List<T>>> x = new ArrayList<>();
            for(int i = 0; i <= G.getMaxNumber(); i++) {
              for(int j = 0; j <= G.getMaxNumber(); j++) {
                try {
                  x.add(getPaths(G.getNode(i), G.getNode(j)));
                } catch (UnsupportedOperationException e) {
                  
                }
              }
            }
            return x.toString();
          }

          @Override
        public Set<List<T>> getPaths(final T from, final T to) {
           int fn = G.getNumber(from);
           int tn = G.getNumber(to);
           if (paths[fn][tn] == Integer.MAX_VALUE) {
             throw new UnsupportedOperationException("no path from " + from + " to " + to);
           } else {
             MutableIntSet intermediate = next[fn][tn];
             if (intermediate == null) {
               List<T> none = Collections.emptyList();
               return Collections.singleton(none);
             } else {
               final Set<List<T>> result = new HashSet<>();
              
               intermediate.foreach(x -> {
                T in = G.getNode(x);
                for(List<T> pre : getPaths(from, in)) {
                  for(List<T> post : getPaths(in, to)) {
                    List<T> path = new LinkedList<>(pre);
                    path.add(in);
                    path.addAll(post);
                    result.add(path);
                  }
                }
              });
               
               return result;
             }
           }
         } 
        };
      }
    }.doit();
 }

}
