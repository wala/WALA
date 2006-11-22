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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;

/**
 * @author sfink
 * @author aying
 * 
 * Simple wrapper around an EObjectGraph to provide content for a tree viewer.
 */
public class CGContentProvider implements ITreeContentProvider {

  protected Graph graph;

  protected Collection roots;

  protected Map<Integer, IJavaElement> capaNodeIdToJavaElement = null;

  public CGContentProvider(Graph g, Collection roots, Map<Integer, IJavaElement> capaNodeIdToJavaElement) {
    this.graph = g;
    this.roots = roots;
    this.capaNodeIdToJavaElement = capaNodeIdToJavaElement;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.IContentProvider#dispose()
   */
  public void dispose() {
    // do nothing for now
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
   *      java.lang.Object, java.lang.Object)
   */
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    // for now do nothing, since we're not dealing with listeners
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
   */
  public Object[] getChildren(Object parentElement) {

    Collection<CGNode> result = new ArrayList<CGNode>();
    for (Iterator<? extends CGNode> it = graph.getSuccNodes((CGNode) parentElement); it.hasNext();) {
      CGNode capaNode = (CGNode) it.next();
      result.add(capaNode);
    }
    return result.toArray();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
   */
  public Object getParent(Object element) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
   */
  public boolean hasChildren(Object element) {
    return graph.getSuccNodeCount((CGNode) element) > 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
   */
  public Object[] getElements(Object inputElement) {
    Object[] firstLevelNodes = new Object[roots.size()];
    Iterator rootIt = roots.iterator();
    int i = 0;
    while (rootIt.hasNext()) {
      CGNode capaNode = (CGNode) rootIt.next();
      firstLevelNodes[i++] = capaNode;
    }

    return firstLevelNodes;
  }
}
