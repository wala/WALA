package com.ibm.wala.viz.viewer;

import java.awt.Component;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JSplitPane;

import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.viz.viewer.IrViewer.SelectedPcListner;

public class IrAndSourceViewer {

  private IrViewer irViewer;
  private SourceViewer sourceViewer;
  
  private IR ir;
  
  public IrAndSourceViewer(CallGraph cg) {
  }

  public Component getComponent() {


    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    irViewer = new IrViewer();
    splitPane.setLeftComponent(irViewer);

    sourceViewer = new SourceViewer();    
    splitPane.setRightComponent(sourceViewer);

    irViewer.addSelectedPcListner(new SelectedPcListner(){

      public void valueChanged(int pc) {
        IMethod method = ir.getMethod();
        int sourceLineNumber = IrViewer.NA;
        String sourceFileName = null;
        if (pc != IrViewer.NA){
          try{
            sourceLineNumber = method.getLineNumber(pc);
            IClassLoader loader = method.getDeclaringClass().getClassLoader();
            sourceFileName = loader.getSourceFileName(method, pc);
          } catch (Exception e){
            e.printStackTrace();
          }
        }
        if (sourceFileName != null){
          URL url;
          try {
            url = (new File(sourceFileName)).toURI().toURL();
            sourceViewer.setSource(url, sourceLineNumber);
          } catch (MalformedURLException e) {
            e.printStackTrace();
          }
        } else {
           sourceViewer.removeSource();
        }
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
