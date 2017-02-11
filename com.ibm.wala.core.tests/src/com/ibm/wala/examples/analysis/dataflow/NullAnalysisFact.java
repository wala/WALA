package com.ibm.wala.examples.analysis.dataflow;

import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.ipa.callgraph.CGNode;

public class NullAnalysisFact {

  public CGNode node;
  public Integer ssaVarId;
  public List<String> accessPath;
  public NullSet type;

  public NullAnalysisFact(CGNode node, Integer ssaVarId, List<String> accessPath, NullSet type) {
    this.node = node;
    this.ssaVarId = ssaVarId;
    this.accessPath = accessPath;
    this.type = type;
  }
  
  public NullAnalysisFact(CGNode node, Integer ssaVarId, NullSet type) {
    this.node = node;
    this.ssaVarId = ssaVarId;
    this.accessPath = getEmptyAccessPath();
    this.type = type;
  }
  
  public static List<String> getEmptyAccessPath() {
    return new LinkedList<String>();
  }
  
  @Override
  public String toString() {
    return this.node + ", " + this.ssaVarId + ", " + this.accessPath + ", " + this.type; 
  }
  
  private boolean check(Object x, Object y) {
      return (x == null) ? (y == null) : x.equals(y);
    }
  
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof NullAnalysisFact)) {
      return false;
    }
    NullAnalysisFact oFact = (NullAnalysisFact) o;
    return check(this.node, oFact.node) && check(this.ssaVarId, oFact.ssaVarId)
        && check(this.accessPath, oFact.accessPath) && check(this.type, oFact.type);
  }

  private int hc(Object o) {
    return (o == null) ? 0 : o.hashCode();
  }

  @Override
  public int hashCode() {
    return 7*hc(node) + 11*hc(ssaVarId) + 3*hc(accessPath) + 13*hc(type);
  }

}
