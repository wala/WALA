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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.NormalAllocationInNode;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.ReturnValueKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * Panel for showing the Pointer Analysis results. Shows the heap graph on the left, and the ir viewer on the right. Sets the IR of
 * and pc according to the chosen pointer/instance key when possible (e.g., allocation side for NormalAllocationInNode instance
 * keys. Can be customized for clients that use different their own pointer / instance keys (see JsPaPanel)
 * 
 * Note the two steps initialization require (calling init())
 * @author yinnonh
 * 
 */
public class PaPanel extends JSplitPane {

  private static final long serialVersionUID = 8120735305334110889L;
  protected final PointerAnalysis<InstanceKey> pa;
  protected final CallGraph cg;

  private JTextField fullName;

  private IrAndSourceViewer irViewer;

  private MutableMapping<List<LocalPointerKey>> cgNodeIdToLocalPointers = MutableMapping.<List<LocalPointerKey>> make();
  private MutableMapping<List<ReturnValueKey>> cgNodeIdToReturnValue = MutableMapping.<List<ReturnValueKey>> make();
  private MutableMapping<List<InstanceFieldPointerKey>> instanceKeyIdToInstanceFieldPointers = MutableMapping.<List<InstanceFieldPointerKey>> make();


  public PaPanel(CallGraph cg, PointerAnalysis<InstanceKey> pa) {
    super(JSplitPane.HORIZONTAL_SPLIT);

    this.pa = pa;
    this.cg = cg;

    initDataStructures(pa);
  }

  /**
   * Two steps initialization is required here is our deriver can choose the roots for the heap tree.
   */
  public void init() {
    this.setDividerLocation(250);

    DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    for (Object rootChildNode : getRootNodes()){
      DefaultMutableTreeNode n = new DefaultMutableTreeNode(rootChildNode);
      root.add(n);
      expandNodeRec(n, 1);
    }
    
    JTree heapTree = new JTree(root);
    
    heapTree.setCellRenderer(new DualTreeCellRenderer());

    this.setLeftComponent(new JScrollPane(heapTree));
    JPanel rightPanel = new JPanel(new BorderLayout());
    this.setRightComponent(rightPanel);
    fullName = new JTextField("");
    rightPanel.add(fullName, BorderLayout.PAGE_START);
    irViewer = new IrAndSourceViewer();
    rightPanel.add(irViewer.getComponent(), BorderLayout.CENTER);

    heapTree.addTreeExpansionListener(new TreeExpansionListener() {

      @Override
      public void treeExpanded(TreeExpansionEvent event) {
        TreePath path = event.getPath();
        if (path == null) {
          return;
        }
        DefaultMutableTreeNode lastNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        expandNodeRec(lastNode, 2);
      }

      @Override
      public void treeCollapsed(TreeExpansionEvent event) {
      }
    });

    heapTree.addTreeSelectionListener(new TreeSelectionListener() {

      @Override
      public void valueChanged(TreeSelectionEvent e) {
      TreePath newLeadSelectionPath = e.getNewLeadSelectionPath();
      if (null == newLeadSelectionPath){
        return;
      }
      DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) newLeadSelectionPath.getLastPathComponent();
      Object userObject = treeNode.getUserObject();
      fullName.setText(userObject.toString());
      if (userObject instanceof LocalPointerKey){
        LocalPointerKey lpk = (LocalPointerKey) userObject;
        IR ir1 = lpk.getNode().getIR();
        SSAInstruction def = lpk.getNode().getDU().getDef(lpk.getValueNumber());
        int pc1 = IrViewer.NA;
        if (def != null){
          SSAInstruction[] instructions = ir1.getInstructions();
          for (int i = 0; i < instructions.length; i++) {
            SSAInstruction instruction = instructions[i];
            if (def == instruction){
              pc1 = i;
            }
          }
        }
        irViewer.setIRAndPc(ir1, pc1);
      } else if (userObject instanceof InstanceFieldPointerKey){
        InstanceKey ik = ((InstanceFieldPointerKey) userObject).getInstanceKey();
        if (ik instanceof NormalAllocationInNode){
          NormalAllocationInNode normalIk1 = (NormalAllocationInNode) ik;
          IR ir2 = normalIk1.getNode().getIR();
          int pc2 = normalIk1.getSite().getProgramCounter();
          irViewer.setIRAndPc(ir2, pc2);
        }
      } else if (userObject instanceof NormalAllocationInNode){
        NormalAllocationInNode normalIk2 = (NormalAllocationInNode) userObject;
        IR ir3 = normalIk2.getNode().getIR();
        int pc3 = normalIk2.getSite().getProgramCounter();
        irViewer.setIRAndPc(ir3, pc3);
      } else if (userObject instanceof CGNode){
        irViewer.setIR(((CGNode)userObject).getIR());
      }


    }});
  }

  private void initDataStructures(PointerAnalysis<InstanceKey> pa) {
    HeapGraph<InstanceKey> heapGraph = pa.getHeapGraph();
    OrdinalSetMapping<InstanceKey> instanceKeyMapping = pa.getInstanceKeyMapping();
    for (Object n : heapGraph){
      if (heapGraph.getPredNodeCount(n) == 0){ // considering only roots of the heap graph.
        if (n instanceof PointerKey){
          if (n instanceof LocalPointerKey){
            LocalPointerKey lpk = (LocalPointerKey) n;
            int nodeId = lpk.getNode().getGraphNodeId();
            mapUsingMutableMapping(cgNodeIdToLocalPointers, nodeId, lpk);
          } else if (n instanceof ReturnValueKey){
            ReturnValueKey rvk = (ReturnValueKey) n;
            int nodeId = rvk.getNode().getGraphNodeId();
            mapUsingMutableMapping(cgNodeIdToReturnValue, nodeId, rvk);
          } else if (n instanceof InstanceFieldPointerKey){
            InstanceFieldPointerKey ifpk = (InstanceFieldPointerKey) n;
            int instanceKeyId = instanceKeyMapping.getMappedIndex(ifpk.getInstanceKey());
            mapUsingMutableMapping(instanceKeyIdToInstanceFieldPointers, instanceKeyId, ifpk);
          }
        } else {
          System.err.println("Non Pointer key root: " + n);
        }
      }
    }
  }


  /**
   * Override if you want different roots for your heap tree.
   */
  protected List<Object> getRootNodes(){
    List<Object> ret = new ArrayList<>();
    for (CGNode n : cg){
      ret.add(n);
    }
    return ret;
  }

  /**
   * expands the given "treeNode" "rec" levels.
   * @param treeNode
   * @param rec
   */
  private void expandNodeRec(DefaultMutableTreeNode treeNode, int rec) {
    if (rec == 0){
      return;
    }

    if (treeNode.getChildCount() == 0){ // may be expandable.
      List<Object> children = getChildrenFor(treeNode.getUserObject());
      for (Object child : children){
        treeNode.add(new DefaultMutableTreeNode(child));
      }
    }

    for (int i = 0 ; i < treeNode.getChildCount(); i++){
      TreeNode child = treeNode.getChildAt(i);
      expandNodeRec((DefaultMutableTreeNode)child, rec-1);
    }
  }

  /**
   * Used for filling the tree dynamically. Override and handle your own nodes / different links.
   * @param node
   */
  protected List<Object> getChildrenFor(Object node) {
    List<Object> ret = new ArrayList<>();
    if (node instanceof InstanceKey){
      ret.addAll(getPointerKeysUnderInstanceKey((InstanceKey) node));
    } else if (node instanceof PointerKey){
      for (InstanceKey ik : pa.getPointsToSet((PointerKey) node)){
        ret.add(ik);
      }
    } else if (node instanceof CGNode){
      int nodeId = ((CGNode) node).getGraphNodeId();
      ret.addAll(nonNullList(cgNodeIdToLocalPointers.getMappedObject(nodeId)));
      ret.addAll(nonNullList(cgNodeIdToReturnValue.getMappedObject(nodeId)));
    } else {
      assert false : "Unhandled Node : " + node;
    }
    return ret;
  }

  /**
   * Get the set of pointer keys that should be presented below an instance key in the heap tree. Override if you have special
   * pointer keys (not just for fields)
   * 
   * @param ik
   */
  protected List<? extends PointerKey> getPointerKeysUnderInstanceKey(InstanceKey ik) {
    int ikIndex = pa.getInstanceKeyMapping().getMappedIndex(ik);
    List<? extends PointerKey> ret;
    if (ikIndex <= instanceKeyIdToInstanceFieldPointers.getMaximumIndex()){
      ret = nonNullList(instanceKeyIdToInstanceFieldPointers.getMappedObject(ikIndex));
    } else {
      ret = Collections.emptyList();
    }
    return ret;
  }


  /**
   * Utility method for mutable mapping. map[index] U= o
   * @param <T>
   * @param map
   * @param index
   * @param o
   */
  protected static <T> void mapUsingMutableMapping(MutableMapping<List<T>> map, int index, T o){
    List<T> set;
    if (index <= map.getMaximumIndex()){
      set = map.getMappedObject(index);
    } else {
      set = null;
    }
    if (null == set){
      set = new ArrayList<>();
      map.put(index, set);
    }
    set.add(o);
  }

  protected <T> List<T> nonNullList(List<T> l){
    if (null == l){
      return Collections.emptyList();
    } else {
      return l;
    }
  }
}
