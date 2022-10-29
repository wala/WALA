/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ide.ui;

import com.ibm.wala.util.PlatformUtil;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.viz.NodeDecorator;
import java.util.ArrayList;
import java.util.Collection;
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

/** A class to view a WALA {@link Graph} with an SWT {@link TreeViewer} */
@SuppressWarnings("unchecked")
public class SWTTreeViewer<T> extends AbstractJFaceRunner {

  protected Graph<T> graphInput;

  protected Collection<?> rootsInput = null;

  protected NodeDecorator<Object> nodeDecoratorInput = null;

  protected final List<IAction> popUpActions = new ArrayList<>();

  public SWTTreeViewer() {
    super();
  }

  public Graph<T> getGraphInput() {
    return graphInput;
  }

  public void setGraphInput(Graph<T> newGraphInput) {
    graphInput = newGraphInput;
  }

  public Collection<?> getRootsInput() {
    return rootsInput;
  }

  public void setRootsInput(Collection<?> newRootsInput) {
    rootsInput = newRootsInput;
  }

  public NodeDecorator<Object> getNodeDecoratorInput() {
    return nodeDecoratorInput;
  }

  public void setNodeDecoratorInput(NodeDecorator<Object> newNodeDecoratorInput) {
    nodeDecoratorInput = newNodeDecoratorInput;
  }

  public List<IAction> getPopUpActions() {
    return popUpActions;
  }

  @Override
  public String toString() {
    return super.toString()
        + ", graphInput: "
        + graphInput
        + ", rootsInput: "
        + rootsInput
        + ", NodeDecoratorInput: "
        + nodeDecoratorInput
        + ", popUpActions: "
        + popUpActions
        + ')';
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
      Runnable r =
          () -> {
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
        Runnable r =
            () -> {
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
   * For testing purposes, open the tree viewer window and then immediately close it. Useful for
   * testing that there is no failure while opening the window.
   */
  public void justOpenForTest() throws WalaException {

    if (getRootsInput() == null) {
      throw new IllegalStateException("null roots input in " + getClass());
    }

    final ApplicationWindow w = new GraphViewer(getGraphInput());
    setApplicationWindow(w);
    if (PlatformUI.isWorkbenchRunning()) {
      throw new IllegalStateException("not designed to run inside workbench");
    }
    w.open();
    Display.getCurrent().dispose();
  }

  public IStructuredSelection getSelection() throws IllegalStateException {
    GraphViewer viewer = (GraphViewer) getApplicationWindow();
    if (viewer == null || viewer.treeViewer == null) {
      throw new IllegalStateException();
    }
    return (IStructuredSelection) viewer.treeViewer.getSelection();
  }

  /**
   * @author sfink
   *     <p>An SWT window to visualize a graph
   */
  private class GraphViewer extends ApplicationWindow {

    /** Graph to visualize */
    private final Graph<T> graph;

    /** JFace component implementing the tree viewer */
    private TreeViewer treeViewer;

    public GraphViewer(Graph<T> graph) throws WalaException {
      super(null);
      this.graph = graph;
      if (graph == null) {
        throw new WalaException("null graph for SWT viewer");
      }
    }

    /** @see org.eclipse.jface.window.Window#createContents(org.eclipse.swt.widgets.Composite) */
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
     *     <p>Simple wrapper around an EObjectGraph to provide content for a tree viewer.
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

        Object[] result = new Object[graph.getSuccNodeCount((T) parentElement)];
        int i = 0;
        for (Object o : Iterator2Iterable.make(graph.getSuccNodes((T) parentElement))) {
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
        return graph.getSuccNodeCount((T) element) > 0;
      }

      /*
       * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
       */
      @Override
      public Object[] getElements(Object inputElement) {
        Collection<?> roots = getRootsInput();
        Assertions.productionAssertion(roots != null);
        Assertions.productionAssertion(roots.size() >= 1);
        return roots.toArray();
      }
    }

    /** Simple graph label provider. TODO: finish this implementation. */
    private class GraphLabelProvider extends LabelProvider {

      final NodeDecorator<Object> d = getNodeDecoratorInput();

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
