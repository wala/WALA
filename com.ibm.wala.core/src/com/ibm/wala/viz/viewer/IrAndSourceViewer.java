/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.viz.viewer;

import java.awt.Component;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JSplitPane;

import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.IR;

public class IrAndSourceViewer {

  private IrViewer irViewer;
  private SourceViewer sourceViewer;
  
  private IR ir;
  
  public Component getComponent() {


    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    irViewer = new IrViewer();
    splitPane.setLeftComponent(irViewer);

    sourceViewer = new SourceViewer();    
    splitPane.setRightComponent(sourceViewer);

    irViewer.addSelectedPcListner(pc -> {
      IMethod method = ir.getMethod();
      int sourceLineNumber = IrViewer.NA;
      String sourceFileName = null;
      if (pc != IrViewer.NA){
        try{
          sourceLineNumber = method.getLineNumber(pc);
          IClassLoader loader = method.getDeclaringClass().getClassLoader();
          sourceFileName = loader.getSourceFileName(method, pc);
        } catch (Exception e1){
          e1.printStackTrace();
        }
      }
      if (sourceFileName != null){
        URL url;
        try {
          url = (new File(sourceFileName)).toURI().toURL();
          sourceViewer.setSource(url, sourceLineNumber);
        } catch (MalformedURLException e2) {
          e2.printStackTrace();
        }
      } else {
         sourceViewer.removeSource();
      }
    });
    
    return splitPane;
  }

  public void setIRAndPc(IR ir, int pc) {
    this.ir = ir;
    irViewer.setIRAndPc(ir, pc);
  }

  public void setIR(IR ir) {
    this.ir = ir;
    irViewer.setIR(ir);
  }

}
