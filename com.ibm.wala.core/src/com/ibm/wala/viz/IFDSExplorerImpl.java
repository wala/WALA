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
package com.ibm.wala.viz;

import java.io.File;
import java.util.Collection;
import java.util.Properties;

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.graph.InferGraphRootsImpl;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.warnings.WalaException;

/**
 * Explore the result of an IFDS problem with an SWT viewer and
 * ghostview.
 * 
 * @author Stephen Fink
 */
public class IFDSExplorerImpl  {

  /**
   * absolute path name to invoke dot
   */
  protected static String dotExe = null;


  /**
   * Absolute path name to invoke ghostview
   */
  protected static String gvExe = null;


  /**
   */
  public static void setDotExe(String newDotExe) {
    dotExe = newDotExe;
  }

  /**
   */
  public static void setGvExe(String newGvExe) {
    gvExe = newGvExe;
  }


  /* (non-Javadoc)
   */
  public static void viewIFDS(CallGraph cg, TabulationResult r) throws WalaException {
    
    if (r == null) {
      throw new IllegalArgumentException("r is null");
    }
    assert gvExe != null;
    assert dotExe != null;
    
    // dump the domain to stderr
    System.err.println("Domain:\n" + r.getProblem().getDomain().toString());
    Trace.println("Domain:\n" + r.getProblem().getDomain().toString());
    
    Properties p = null;;
    try {
      p = WalaProperties.loadProperties();
    } catch (WalaException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
    String psFile = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + "ir.ps";
    String dotFile = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + "ir.dt";

    final SWTTreeViewer v = new SWTTreeViewer();
    v.setGraphInput(cg);
    v.setBlockInput(true);
    Collection<CGNode> roots = InferGraphRootsImpl.inferRoots(cg);
    v.setRootsInput(roots);
    v.getPopUpActions().add(new ViewAnnotatedIRAction(v,cg,psFile,dotFile,dotExe,gvExe, new IFDSAnnotator(r)));
    v.run();
  }
  
  private final static class IFDSAnnotator extends BasicBlockDecorator {
    final private TabulationResult r;
    public IFDSAnnotator(TabulationResult r) {
     this.r = r;
    }
    @SuppressWarnings("unchecked")
    public String getLabel(Object o) throws WalaException {
      IBasicBlock bb = (IBasicBlock)o;
      Object b = new BasicBlockInContext(getCurrentNode(),bb);
      IntSet result = r.getResult(b);
      String label = result == null ? "no result" : result.toString();
      return label + "\\n-----\\n";
    }
    
  }

  public static String getDotExe() {
    return dotExe;
  }

  public static String getGvExe() {
    return gvExe;
  }

} //IFDSExplorerImpl
