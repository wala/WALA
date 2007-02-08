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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

/**
 * @author sfink
 * @author aying
 * 
 * Simple wrapper around an EObjectGraph to provide content for a tree viewer.
 */
public class CGContentProvider implements ITreeContentProvider {

  protected CallGraph graph;

  protected Collection roots;

  public CGContentProvider(CallGraph g, Collection roots) {
    this.graph = g;
    this.roots = roots;
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
  @SuppressWarnings("unchecked")
  public Object[] getChildren(Object parentElement) {
    Collection result = new ArrayList();
    
    if (parentElement instanceof CGNode) {
    	for(Iterator it = ((CGNode)parentElement).iterateSites(); it.hasNext(); ) {
    		CallSiteReference site = (CallSiteReference) it.next();
    		if (! ((CGNode)parentElement).getPossibleTargets(site).isEmpty()) {
    		  result.add(new Pair(parentElement, site));
    		}
    	}
    
    } else {
      Pair pe = (Pair)parentElement;
      for (Iterator it = ((CGNode)pe.fst).getPossibleTargets((CallSiteReference)pe.snd).iterator(); it.hasNext(); ) {
        result.add(it.next());
      }
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
    if (element instanceof CGNode) {
	  return ((CGNode)element).iterateSites().hasNext();
    } else {
      Pair pe = (Pair)element;
      return !((CGNode)pe.fst).getPossibleTargets((CallSiteReference)pe.snd).isEmpty();
    }
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
