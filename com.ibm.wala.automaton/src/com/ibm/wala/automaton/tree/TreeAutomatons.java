/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.automaton.tree;

import java.util.*;

import com.ibm.wala.automaton.*;
import com.ibm.wala.automaton.string.*;

public class TreeAutomatons {
    static public abstract class AbstractBinarizationCopier implements ISymbolCopier {
        abstract protected IParentBinaryTree createBinaryTree(ISymbol s);
        
        public ISymbol copy(ISymbol symbol) {
            IParentTree t = (IParentTree) symbol;
            IBinaryTree cbt = copyAll(t.getChildren().iterator());
            IParentBinaryTree bt = createBinaryTree(t.getLabel());
            bt.setLeft(cbt);
            return bt;
        }
        
        private IBinaryTree copy(IParentTree tree, Iterator siblings) {
            IParentBinaryTree t = (IParentBinaryTree) tree.copy(this);
            if (siblings.hasNext()) {
                IParentTree r = (IParentTree) siblings.next();
                IBinaryTree br = (IBinaryTree) copy(r, siblings);
                t.setRight(br);
            }
            return t;
        }
        
        private IBinaryTree copyAll(Iterator i) {
            if (i.hasNext()) {
                return copy((IParentTree)i.next(), i);
            }
            else {
                return null;
            }
        }
        
        public String copyName(String name) {
            return name;
        }

        public Collection copySymbols(Collection symbols) {
            IBinaryTree bt = copyAll(symbols.iterator());
            List l = new ArrayList();
            l.add(bt);
            return l;
        }
        
        public ISymbol copySymbolReference(ISymbol parent, ISymbol symbol) {
            throw(new AssertionError("should not reach this code."));
        }

        public Collection copySymbolReferences(ISymbol parent, Collection symbols) {
            throw(new AssertionError("should not reach this code."));
        }
    }
    
    static public abstract class AbstractUnbinarizationCopier implements ISymbolCopier {
        abstract protected IParentTree createTree(ISymbol s);
        
        public ISymbol copy(ISymbol symbol) {
            IParentBinaryTree bt = (IParentBinaryTree) symbol;
            IBinaryTree lbt = bt.getLeft();
            IParentTree t = createTree(bt.getLabel());
            t.getChildren().addAll(copyAll(lbt));
            return t;
        }
        
        public Collection copyAll(IBinaryTree bt) {
            List l = null;
            if (bt.equals(BinaryTree.LEAF)) {
                return new ArrayList();
            }
            else if (bt instanceof IParentBinaryTree) {
                IParentBinaryTree pbt = (IParentBinaryTree) bt;
                if (pbt.getRight() == null) {
                    l = new ArrayList();
                }
                else {
                    l = (List) copyAll(pbt.getRight());
                }
                l.add(0, copy(pbt));
                return l;
            }
            else {
                throw(new AssertionError("should not reach this code."));
            }
        }

        public Collection copySymbols(Collection symbols) {
            throw(new AssertionError("should not reach this code."));
        }
        
        public String copyName(String name) {
            return name;
        }

        public ISymbol copySymbolReference(ISymbol parent, ISymbol symbol) {
            throw(new AssertionError("should not reach this code."));
        }

        public Collection copySymbolReferences(ISymbol parent, Collection symbols) {
            throw(new AssertionError("should not reach this code."));
        }
        
    }
    
    static public class BinarizationCopier extends AbstractBinarizationCopier {
        protected IParentBinaryTree createBinaryTree(ISymbol s) {
            return new BinaryTree(s);
        }
        
        static final public BinarizationCopier defaultCopier = new BinarizationCopier();
    }
    
    static public class UnbinarizationCopier extends AbstractUnbinarizationCopier {
        protected IParentTree createTree(ISymbol s) {
            return new Tree(s);
        }
        
        static final public UnbinarizationCopier defaultCopier = new UnbinarizationCopier();
    }
    
    static public IBinaryTree binarize(ITree tree, AbstractBinarizationCopier copier) {
        return (IBinaryTree) tree.copy(copier);
    }
    
    static public IBinaryTree binarize(ITree tree) {
        return binarize(tree, BinarizationCopier.defaultCopier);
    }
    
    static public IBinaryTree binarize(Collection trees, AbstractBinarizationCopier copier) {
        Collection c = copier.copySymbols(trees);
        Iterator i = c.iterator();
        if (i.hasNext()) {
            return (IBinaryTree) i.next();
        }
        else {
            return null;
        }
    }
    
    static public IBinaryTree binarize(Collection trees) {
        return binarize(trees, BinarizationCopier.defaultCopier);
    }
    
    static public List unbinarize(IBinaryTree tree, AbstractUnbinarizationCopier copier) {
        IBinaryTree bt = new BinaryTree("_", tree, null);
        IParentTree t = (IParentTree) bt.copy(copier);
        return t.getChildren();
    }
    
    static public List unbinarize(IBinaryTree tree) {
        return unbinarize(tree, UnbinarizationCopier.defaultCopier);
    }
}
