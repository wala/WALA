/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.viz.viewer;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

/**
 * Renderer for the heap tree. Gives non default icons for pointer keys and instance keys.
 *
 * @author yinnonh
 */
class DualTreeCellRenderer implements TreeCellRenderer {

  private final DefaultTreeCellRenderer defaultTreeCellRenderer;
  private final DefaultTreeCellRenderer ikTreeCellRenderer;
  private final DefaultTreeCellRenderer pkTreeCellRenderer;

  public DualTreeCellRenderer() {
    defaultTreeCellRenderer = new DefaultTreeCellRenderer();

    ikTreeCellRenderer = new DefaultTreeCellRenderer();
    ikTreeCellRenderer.setOpenIcon(createImageIcon("images/ik_open.png"));
    ikTreeCellRenderer.setClosedIcon(createImageIcon("images/ik_closed.png"));
    ikTreeCellRenderer.setLeafIcon(createImageIcon("images/ik_leaf.png"));

    pkTreeCellRenderer = new DefaultTreeCellRenderer();
    pkTreeCellRenderer.setOpenIcon(createImageIcon("images/pk_open.png"));
    pkTreeCellRenderer.setClosedIcon(createImageIcon("images/pk_closed.png"));
    pkTreeCellRenderer.setLeafIcon(createImageIcon("images/pk_leaf.png"));
  }

  @Override
  public Component getTreeCellRendererComponent(
      JTree tree,
      Object value,
      boolean selected,
      boolean expanded,
      boolean leaf,
      int row,
      boolean hasFocus) {
    TreeCellRenderer delegate = getTreeCellRenderer(value);
    return delegate.getTreeCellRendererComponent(
        tree, value, selected, expanded, leaf, row, hasFocus);
  }

  private TreeCellRenderer getTreeCellRenderer(Object value) {
    if (value instanceof DefaultMutableTreeNode) {
      value = ((DefaultMutableTreeNode) value).getUserObject();
    }
    if (value instanceof PointerKey) {
      return pkTreeCellRenderer;
    } else if (value instanceof InstanceKey) {
      return ikTreeCellRenderer;
    } else {
      return defaultTreeCellRenderer;
    }
  }

  /** Returns an ImageIcon, or null if the path was invalid. */
  protected ImageIcon createImageIcon(String path) {
    java.net.URL imgURL = DualTreeCellRenderer.class.getResource(path);
    if (imgURL != null) {
      return new ImageIcon(imgURL);
    } else {
      System.err.println("Couldn't find file: " + path);
      return null;
    }
  }
}
