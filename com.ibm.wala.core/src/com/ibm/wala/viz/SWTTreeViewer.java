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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NodeDecorator;
import com.ibm.wala.util.warnings.WalaException;

/**
 */
public class SWTTreeViewer extends EJfaceApplicationRunner  {
  /**
   * The cached value of the '{@link #getGraphInput() <em>Graph Input</em>}' attribute.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @see #getGraphInput()
   * @generated
   * @ordered
   */
  protected Graph graphInput;


  /**
   * The cached value of the '{@link #getRootsInput() <em>Roots Input</em>}' attribute.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @see #getRootsInput()
   * @generated
   * @ordered
   */
  protected Collection<? extends Object> rootsInput = null;


  /**
   * The cached value of the '{@link #getNodeDecoratorInput() <em>Node Decorator Input</em>}' attribute.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @see #getNodeDecoratorInput()
   * @generated
   * @ordered
   */
  protected NodeDecorator nodeDecoratorInput = null;

  /**
   * The cached value of the '{@link #getPopUpActions() <em>Pop Up Actions</em>}' attribute list.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @see #getPopUpActions()
   * @generated
   * @ordered
   */
  protected List<ViewIRAction> popUpActions = new LinkedList<ViewIRAction>();

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public SWTTreeViewer() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public Graph getGraphInput() {
    return graphInput;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public void setGraphInput(Graph newGraphInput) {
    graphInput = newGraphInput;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public Collection<? extends Object> getRootsInput() {
    return rootsInput;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public void setRootsInput(Collection<? extends Object> newRootsInput) {
    rootsInput = newRootsInput;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public NodeDecorator getNodeDecoratorInput() {
    return nodeDecoratorInput;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public void setNodeDecoratorInput(NodeDecorator newNodeDecoratorInput) {
    nodeDecoratorInput = newNodeDecoratorInput;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public List<ViewIRAction> getPopUpActions() {
    return popUpActions;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  @Override
  public String toString() {
    StringBuffer result = new StringBuffer(super.toString());
    result.append(", graphInput: ");
    result.append(graphInput);
    result.append(", rootsInput: ");
    result.append(rootsInput);
    result.append(", NodeDecoratorInput: ");
    result.append(nodeDecoratorInput);
    result.append(", popUpActions: ");
    result.append(popUpActions);
    result.append(')');
    return result.toString();
  }

  public void run() throws WalaException {

    if (getRootsInput() == null) {
      throw new WalaException("null roots input in " + getClass());
    }

    final ApplicationWindow w = new GraphViewer(getGraphInput());
    setApplicationWindow(w);
    w.setBlockOnOpen(true);
    Runnable r = new Runnable() {
      public void run() {
        w.open();
        Display.getCurrent().dispose();
      }
    };
    Thread t = new Thread(r);
    t.start();
    if (isBlockInput()) {
      try {
        t.join();
      } catch (InterruptedException e) {
        throw new WalaException("unexpected interruption", e);
      }
    }
  }



  /**
   * @return
   * @throws IllegalStateException
   */
  public IStructuredSelection getSelection() throws IllegalStateException {
    GraphViewer viewer = (GraphViewer)getApplicationWindow();
    if (viewer == null || viewer.viewer == null) {
      throw new IllegalStateException();
    }
    return (IStructuredSelection) viewer.viewer.getSelection();
  }
  
  /**
   * @author sfink
   * 
   * An SWT window to visualize a graph
   */
  private class GraphViewer extends ApplicationWindow {

    /**
     * Graph to visualize
     */
    private final Graph graph;
    
    /**
     * JFace component implementing the tree viewer
     */
    private TreeViewer viewer;

    /**
     * @throws WalaException
     */
    public GraphViewer(Graph graph) throws WalaException {
      super(null);
      this.graph = graph;
      if (graph == null) {
        throw new WalaException("null graph for SWT viewer");
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.window.Window#createContents(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
      viewer = new TreeViewer(parent);
      viewer.setContentProvider(new GraphContentProvider());
      viewer.setLabelProvider(new GraphLabelProvider());
      viewer.setInput(getGraphInput());

      // create a pop-up menu
      if (getPopUpActions().size() > 0) {
        MenuManager mm = new MenuManager();
        viewer.getTree().setMenu(mm.createContextMenu(viewer.getTree()));
        for (Iterator<ViewIRAction> it = getPopUpActions().iterator(); it.hasNext(); ) {
          mm.add(it.next());
        }
      }
      return viewer.getTree();
    }


    /**
     * @author sfink
     * 
     * Simple wrapper around an EObjectGraph to provide content for a tree
     * viewer.
     */
    private class GraphContentProvider implements ITreeContentProvider {

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

        Object[] result = new Object[graph.getSuccNodeCount(parentElement)];
        int i = 0;
        for (Iterator it = graph.getSuccNodes(parentElement); it.hasNext();) {
          result[i++] = it.next();
        }
        return result;
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
      @SuppressWarnings("unchecked")
      public boolean hasChildren(Object element) {
        return graph.getSuccNodeCount(element) > 0;
      }

      /*
       * (non-Javadoc)
       * 
       * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
       */
      public Object[] getElements(Object inputElement) {
        Collection<? extends Object> roots = getRootsInput();
        Assertions.productionAssertion(roots != null);
        Assertions.productionAssertion(roots.size() >= 1);
        return roots.toArray();
      }
    }

    /**
     * @author sfink
     * 
     * Simplel graph label provider. TODO: finish this implementation.
     */
    private class GraphLabelProvider extends LabelProvider {

      final NodeDecorator d = getNodeDecoratorInput();

      @Override
      public String getText(Object element) {
        try {
          return (d == null) ? super.getText(element) : d.getLabel(element);
        } catch (WalaException e) {
          e.printStackTrace();
          Assertions.UNREACHABLE();
          return null;
        }
      }

    }
  }
}
