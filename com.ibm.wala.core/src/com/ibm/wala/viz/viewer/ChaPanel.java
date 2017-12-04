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

import java.util.Collection;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;

public class ChaPanel extends JSplitPane {

  private static final long serialVersionUID = -9058908127737757320L;
  private final IClassHierarchy cha;

  public ChaPanel(IClassHierarchy cha) {
    this.cha = cha;
  
    this.setDividerLocation(250);
    JTree tree = buildTree();
    this.setLeftComponent(new JScrollPane(tree));
    
    final DefaultListModel<String> methodListModel = new DefaultListModel<>();
    JList methodList = new JList<>(methodListModel);
    this.setRightComponent(methodList);
    
    tree.addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
      TreePath newLeadSelectionPath = e.getNewLeadSelectionPath();
      if (null == newLeadSelectionPath){
        return;
      }
      DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) newLeadSelectionPath.getLastPathComponent();
      IClass klass = (IClass) treeNode.getUserObject();
      methodListModel.clear();
      for (IMethod m : klass.getDeclaredMethods()){
        methodListModel.addElement(m.toString());
      }
    }   
  });
  }

  private JTree buildTree() {
    IClass rootClass = cha.getRootClass();
    DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootClass);

    expandNode(root);
    JTree tree = new JTree(root);

    tree.addTreeExpansionListener(new TreeExpansionListener() {

      @Override
      public void treeExpanded(TreeExpansionEvent event) {
        TreePath path = event.getPath();
        if (path == null) {
          return;
        }
        DefaultMutableTreeNode lastNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        expandNode(lastNode);
      }

      @Override
      public void treeCollapsed(TreeExpansionEvent event) {

      }
    });

    return tree;
  }

  private void expandNode(DefaultMutableTreeNode treeNode) {
    expandNode(treeNode, 3);
  }

  private void expandNode(DefaultMutableTreeNode treeNode, int rec) {
    if (rec == 0) {
      return;
    }

    if (treeNode.getChildCount() == 0) {
      IClass klass = (IClass) treeNode.getUserObject();
      Collection<IClass> immediateSubclasses = cha.getImmediateSubclasses(klass);
      for (IClass immediateSubclass : immediateSubclasses){
        treeNode.add(new DefaultMutableTreeNode(immediateSubclass));
      }
    }

    for (int i = 0; i < treeNode.getChildCount(); i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode) treeNode.getChildAt(i);
      expandNode(child, rec - 1);
    }
  }

  
  
}
