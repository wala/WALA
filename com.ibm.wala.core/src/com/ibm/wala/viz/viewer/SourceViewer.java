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
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class SourceViewer extends JPanel{
  private static final long serialVersionUID = -1688405955293925453L;
  private URL sourceURL;
  private JTextField sourceCodeLocation;
  private DefaultListModel<String> sourceCodeLinesList = new DefaultListModel<>();
  private JList<String> sourceCodeLines;

  public SourceViewer() {
    super(new BorderLayout());
    sourceURL = null;
    sourceCodeLines = new JList<>(sourceCodeLinesList);
    sourceCodeLocation = new JTextField("Source code");
    this.add(sourceCodeLocation, BorderLayout.PAGE_START);
    this.add(new JScrollPane(sourceCodeLines), BorderLayout.CENTER);
  }

  public void setSource(URL url) {
    setSource(url, IrViewer.NA);
  }

  public void setSource(URL url, int sourceLine) {
    boolean succsess = loadSource(url);
    if (succsess){
      sourceCodeLocation.setText("Source code: " + url);
      if (sourceLine != IrViewer.NA){
        sourceCodeLines.ensureIndexIsVisible(sourceLine-1);
        sourceCodeLines.setSelectedIndex(sourceLine-1);
        sourceCodeLines.validate();
      }
    } else {
      sourceCodeLocation.setText("Error loading source code from: " + url);
    }
  }
  
  private boolean loadSource(URL url) {
    if (url == null) {
      if (sourceURL != null) {
        // easing the current code.
        sourceCodeLinesList.clear();
      }
      return false; // nothing to load
    } else {
      if (url.equals(sourceURL)) {
        return true; // already loaded
      } else {
        sourceCodeLinesList.clear();
        try {
          BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
          String line;
          while ((line = br.readLine()) != null) {
            sourceCodeLinesList.addElement(line.replaceAll("\t", "   "));
          }
          br.close();
          return true;
        } catch (IOException e) {
          System.err.println("Could not load source at " + url);
          return false;
        }
      }
    }
  }

  public void removeSelection() {
    int curSelectedIndex = sourceCodeLines.getSelectedIndex();
    sourceCodeLines.removeSelectionInterval(curSelectedIndex, curSelectedIndex);
  }

  public void removeSource() {
    sourceURL = null;
    sourceCodeLocation.setText("Source code");
    sourceCodeLinesList.clear();
    sourceCodeLines.validate();
  }


}
