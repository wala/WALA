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
package com.ibm.wala.ide.ui;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.action.IAction;
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
import org.eclipse.ui.PlatformUI;

import com.ibm.wala.util.PlatformUtil;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.viz.NodeDecorator;

/**
 * A class to view a WALA {@link Graph} with an SWT {@link TreeViewer}
 */
@SuppressWarnings("unchecked")
public class SWTTreeViewer extends AbstractJFaceRunner {

  protected Graph graphInput;

  protected Collection<? extends Object> rootsInput = null;

  protected NodeDecorator nodeDecoratorInput = null;

  final protected List<IAction> popUpActions = new LinkedList<>();

  public SWTTreeViewer() {
    super();
  }

  public Graph getGraphInput() {
    return graphInput;
  }

  public void setGraphInput(Graph newGraphInput) {
    graphInput = newGraphInput;
  }

  public Collection<? extends Object> getRootsInput() {
    return rootsInput;
  }

  public void setRootsInput(Collection<? extends Object> newRootsInput) {
    rootsInput = newRootsInput;
  }

  public NodeDecorator getNodeDecoratorInput() {
    return nodeDecoratorInput;
  }

  public void setNodeDecoratorInput(NodeDecorator newNodeDecoratorInput) {
    nodeDecoratorInput = newNodeDecoratorInput;
  }

  public List<IAction> getPopUpActions() {
    return popUpActions;
  }

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

    if (PlatformUI.isWorkbenchRunning()) {
      // run the code on the UI thread
      Display d = PlatformUI.getWorkbench().getDisplay();
      Runnable r = () -> {
        try {
          w.open();
        } catch (Exception e) {
          e.printStackTrace();
        }
      };
      if (isBlockInput()) {
        d.syncExec(r);
      } else {
        d.asyncExec(r);
      }
    } else {
      if (PlatformUtil.onMacOSX()) {
        // the Mac does not like running the Window code in another thread
        // side-effect: we always block input on Mac
        w.open();
        Display.getCurrent().dispose();
      } else {
        Runnable r = () -> {
          w.open();
          Display.getCurrent().dispose();
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
    }
  }

  /**
   * @throws IllegalStateException
   */
  public IStructuredSelection getSelection() throws IllegalStateException {
    GraphViewer viewer = (GraphViewer) getApplicationWindow();
    if (viewer == null || viewer.treeViewer == null) {
      throw new IllegalStateException();
    }
    return (IStructuredSelection) viewer.treeViewer.getSelection();
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
    private final Graph<Object> graph;

    /**
     * JFace component implementing the tree viewer
     */
    private TreeViewer treeViewer;

    /**
     * @throws WalaException
     */
    public GraphViewer(Graph<Object> graph) throws WalaException {
      super(null);
      this.graph = graph;
      if (graph == null) {
        throw new WalaException("null graph for SWT viewer");
      }
    }

    /*
     * @see org.eclipse.jface.window.Window#createContents(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
      treeViewer = new TreeViewer(parent);
      treeViewer.setContentProvider(new GraphContentProvider());
      treeViewer.setLabelProvider(new GraphLabelProvider());
      treeViewer.setInput(getGraphInput());

      // create a pop-up menu
      if (getPopUpActions().size() > 0) {
        MenuManager mm = new MenuManager();
        treeViewer.getTree().setMenu(mm.createContextMenu(treeViewer.getTree()));
        for (IAction iAction : getPopUpActions()) {
          mm.add(iAction);
        }
      }
      return treeViewer.getTree();
    }

    /**
     * @author sfink
     * 
     * Simple wrapper around an EObjectGraph to provide content for a tree viewer.
     */
    private class GraphContentProvider implements ITreeContentProvider {

      /*
       * @see org.eclipse.jface.viewers.IContentProvider#dispose()
       */
      @Override
      public void dispose() {
        // do nothing for now
      }

      /*
       * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
       *      java.lang.Object, java.lang.Object)
       */
      @Override
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // for now do nothing, since we're not dealing with listeners
      }

      /*
       * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
       */
      @Override
      public Object[] getChildren(Object parentElement) {

        Object[] result = new Object[graph.getSuccNodeCount(parentElement)];
        int i = 0;
        for (Object o : Iterator2Iterable.make(graph.getSuccNodes(parentElement))) {
          result[i++] = o;
        }
        return result;
      }

      /*
       * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
       */
      @Override
      public Object getParent(Object element) {
        // TODO Auto-generated method stub
        Assertions.UNREACHABLE();
        return null;
      }

      /*
       * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
       */
      @Override
      public boolean hasChildren(Object element) {
        return graph.getSuccNodeCount(element) > 0;
      }

      /*
       * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
       */
      @Override
      public Object[] getElements(Object inputElement) {
        Collection<? extends Object> roots = getRootsInput();
        Assertions.productionAssertion(roots != null);
        Assertions.productionAssertion(roots.size() >= 1);
        return roots.toArray();
      }
    }

    /**
     * Simple graph label provider. TODO: finish this implementation.
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
