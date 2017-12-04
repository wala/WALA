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

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.collections.HashMapFactory;

public class IrViewer extends JPanel{
  private static final long serialVersionUID = -5668847442988389016L;
  private JTextField methodName;
  private DefaultListModel<String> irLineList = new DefaultListModel<>();
  private JList<String> irLines;

  // mapping from ir viewer list line to source code line number.
  private Map<Integer, Integer> lineToPosition = null;
  
  // Mapping for pc to line in the ir viewer list.
  private Map<Integer, Integer> pcToLine = null;
  private Map<Integer, Integer> lineToPc = null;

  public interface SelectedPcListner{
    void valueChanged(int pc);
  }
  Set<SelectedPcListner> selectedPcListners = new HashSet<>();
  
  public IrViewer() {
    super(new BorderLayout());
    irLines = new JList<>(irLineList);
    methodName = new JTextField("IR");
    this.add(methodName, BorderLayout.PAGE_START);
    this.add(new JScrollPane(irLines, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
    
    
    irLines.addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
      int index = irLines.getSelectedIndex();
      Integer pc = lineToPc.get(index);
      if (pc == null) {
        pc = NA;
      }
      for (SelectedPcListner selectedPcListner : selectedPcListners) {
        selectedPcListner.valueChanged(pc);
      }
    }});
  }
  
  public void setIR(IR ir) {
    this.lineToPosition = HashMapFactory.make();
    this.pcToLine = HashMapFactory.make();
    this.lineToPc = HashMapFactory.make();
    
    int firstLineWithPosition = NA;

    try {
      methodName.setText("IR: " + ir.getMethod());
      irLineList.clear();
      BufferedReader br = new BufferedReader(new StringReader(ir.toString()));
      int lineNum = 0;
      int position = NA;
      String line;
      while ((line = br.readLine()) != null) {
        irLineList.addElement(line);
        int pc = parseIrLine(line);
        if (pc != NA) {
          IMethod m = ir.getMethod();
          int newPosition = m.getLineNumber(pc);
          if (newPosition != -1) {
            position = newPosition;
          }
          lineToPc.put(lineNum, pc);
          pcToLine.put(pc, lineNum);

          if (position != NA) {
            lineToPosition.put(lineNum, position);
            if (firstLineWithPosition == NA){
              firstLineWithPosition = lineNum;
            }
          }
        }
        lineNum++;
      }
    } catch (IOException e) {
      // ???
      assert false;
    }
    
    // focusing on the first line with position 
    if (firstLineWithPosition != NA){
      irLines.setSelectedIndex(firstLineWithPosition);
      irLines.ensureIndexIsVisible(firstLineWithPosition);
    }
  }

  static final int NA = -1;

  private static int parseIrLine(String line) {
    int firstSpace = line.indexOf(' ');
    if (firstSpace > 0) {
      String pcString = line.substring(0, firstSpace);
      try {
        return Integer.parseInt(pcString);
      } catch (NumberFormatException e) {
        return NA;
      }
    } else {
      return NA;
    }
  }

  public void addSelectedPcListner(SelectedPcListner selectedPcListner) {
    this.selectedPcListners.add(selectedPcListner);
  }
  
  public void setPc(int pc){
    Integer lineNum = pcToLine.get(pc);
    if (lineNum != null){
      irLines.ensureIndexIsVisible(lineNum);
      irLines.setSelectedIndex(lineNum);
    } else {
      removeSelection();
    }
  }

  public void setIRAndPc(IR ir, int pc) {
    setIR(ir);
    if (pc != NA){
      setPc(pc);
    } else {
      removeSelection();
    }
  }

  private void removeSelection() {
    int curSelectedIndex = irLines.getSelectedIndex();
    irLines.removeSelectionInterval(curSelectedIndex, curSelectedIndex);
  }


}
