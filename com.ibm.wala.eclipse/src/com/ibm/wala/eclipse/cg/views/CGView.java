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

package com.ibm.wala.eclipse.cg.views;

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.ibm.wala.eclipse.cg.model.WalaCGModel;
import com.ibm.wala.eclipse.cg.model.WalaCGModelWithMain;
import com.ibm.wala.eclipse.util.CapaToJavaEltConverter;
import com.ibm.wala.eclipse.util.JdtUtil;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.warnings.WalaException;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 * 
 * @author aying
 */

public class CGView extends ViewPart {

  public static final String ID = "com.ibm.wala.eclipse.views.cfg.CGView";

  private TreeViewer viewer;

  /**
   * The constructor.
   */
  public CGView() throws JavaModelException {
  }

  /**
   * This is a callback that will allow us to create the viewer and initialize
   * it.
   */
  public void createPartControl(Composite parent) {
    IFile selectedJar = getSelectedJar();
    if( selectedJar != null ) {
    	createViewer(parent, selectedJar);
    }
  }

  private IFile getSelectedJar() {
    ISelection currentSelection = getSite().getWorkbenchWindow().getSelectionService().getSelection();

    if (currentSelection instanceof IStructuredSelection) {
      Object selected = ((IStructuredSelection) currentSelection).getFirstElement();
      if (selected instanceof IFile && ((IFile) selected).getFileExtension().equals("jar")) {
        return (IFile) selected;
      }
    }
    return null;
  }

  private void createViewer(Composite parent, IFile jarFile) {
    try {
      // get the selected jar file
      String applicationJar = jarFile.getRawLocation().toString();
      IJavaProject project = JdtUtil.getJavaProject(jarFile);

      // compute the call graph
      WalaCGModel model = new WalaCGModelWithMain(applicationJar);
      model.buildGraph();
      Collection roots = model.getRoots();
      CallGraph graph = model.getGraph();

      // convert call graph nodes to Eclipse JDT elements
      final Map<Integer, IJavaElement> capaNodeIdToJavaElement = CapaToJavaEltConverter.convert(
    		  model.getGraph(), project);

      // create the tree view
      viewer = new TreeViewer(parent);
      viewer.setContentProvider(new CGContentProvider(graph, roots, capaNodeIdToJavaElement));
      viewer.setLabelProvider(new CGJavaLabelProvider(capaNodeIdToJavaElement));
      viewer.setInput(getViewSite());
      viewer.addOpenListener(new IOpenListener() {
        // open the file when element in the tree is clicked
        public void open(OpenEvent e) {
          ISelection sel = e.getSelection();
          if (sel instanceof ITreeSelection) {
            ITreeSelection treeSel = (ITreeSelection) sel;
            Object selectedElt = treeSel.getFirstElement();
            if (selectedElt instanceof CGNode) {
              try {
                CGNode capaNode = (CGNode) selectedElt;
                IJavaElement jdtElt = capaNodeIdToJavaElement.get(capaNode.getGraphNodeId());
                if (jdtElt != null) {
                  JavaUI.revealInEditor(JavaUI.openInEditor(jdtElt), jdtElt);
                }
              } catch (PartInitException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
              } catch (JavaModelException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
              }
            }
          }
        }
      });
    } catch (JavaModelException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (WalaException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * Passing the focus request to the viewer's control.
   */
  public void setFocus() {
	  if( viewer != null && viewer.getControl() != null ) {
		  viewer.getControl().setFocus();
	  }
  }

}