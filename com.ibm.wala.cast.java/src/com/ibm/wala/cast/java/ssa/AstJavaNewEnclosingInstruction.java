package com.ibm.wala.cast.java.ssa;

import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ssa.SSANewInstruction;

// A new instruction with an explicit outer class, i.e. "Inner inner = outer.new Inner();"
public class AstJavaNewEnclosingInstruction extends SSANewInstruction {

  int enclosing;
  
  public AstJavaNewEnclosingInstruction(int result, NewSiteReference site, int enclosing) throws IllegalArgumentException {
    super(result, site);
    this.enclosing = enclosing;
  }
  
  public int getEnclosing() {
    return this.enclosing;
  }
  
  public String toString() {
    return super.toString() + " ENCLOSING v" + enclosing;
  }


}
