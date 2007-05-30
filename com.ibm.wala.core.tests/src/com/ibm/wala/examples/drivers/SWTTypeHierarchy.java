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
package com.ibm.wala.examples.drivers;

import java.util.Collection;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.window.ApplicationWindow;

import com.ibm.wala.ecore.java.ETypeHierarchy;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.InferGraphRoots;
import com.ibm.wala.viz.SWTTreeViewer;

/**
 * 
 * This application is a WALA client: it invokes an SWT TreeViewer to visualize
 * a TypeHierarchy in a precomputed file serialized on disk. So, you must run
 * ExportTypeHierarchyToXML before running this, to compute the type hierarchy and
 * serialize it to disk.
 * 
 * @author sfink
 */
public class SWTTypeHierarchy {

  /**
   * Usage: SWTTreeViewerBasicPipeline
   */
  public static void main(String[] args) {
    run();
  }

  public static ApplicationWindow run()  {

    try {
      ETypeHierarchy th = GVTypeHierarchy.readTypeHierarchy();
      if (th.getClasses().getNodes().getContents().size() <1) {
        System.err.println("PANIC: th # classes=" + th.getClasses().getNodes().getContents().size());
        System.exit(-1);
      }

      Graph<EObject> g = GVTypeHierarchy.typeHierarchy2Graph(th);
      g = GVTypeHierarchy.pruneForAppLoader(g);
      
      if (g.getNumberOfNodes() == 0) {
        System.err.println("ERROR: The type hierarchy in " + ExportTypeHierarchyToXML.getFileName() + " has no nodes from the Application loader");
        System.exit(-1);
      }
      
      // create and run the viewer
      final SWTTreeViewer v =new SWTTreeViewer();
      v.setGraphInput(g);
      Collection<EObject> roots = InferGraphRoots.inferRoots(g);
      if (roots.size() < 1) {
        System.err.println("PANIC: roots.size()=" + roots.size());
        System.exit(-1);
      }
      v.setRootsInput(roots);
      v.run();
      return v.getApplicationWindow();

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } 
  }
}