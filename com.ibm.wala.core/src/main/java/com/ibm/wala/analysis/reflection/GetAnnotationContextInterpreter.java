/*
 * Copyright (c) 2021 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.analysis.reflection;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.BypassSyntheticClassLoader;
import com.ibm.wala.ipa.summaries.SyntheticIR;
import com.ibm.wala.shrike.shrikeCT.AnnotationsReader;
import com.ibm.wala.shrike.shrikeCT.AnnotationsReader.ArrayElementValue;
import com.ibm.wala.shrike.shrikeCT.AnnotationsReader.ConstantElementValue;
import com.ibm.wala.shrike.shrikeCT.AnnotationsReader.ElementValue;
import com.ibm.wala.shrike.shrikeCT.AnnotationsReader.EnumElementValue;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.*;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * {@link SSAContextInterpreter} specialized to interpret Class.getAnnotation() in a {@link
 * JavaTypeContext}
 */
public class GetAnnotationContextInterpreter implements SSAContextInterpreter {

  private static final boolean DEBUG = false;

  /* END Custom change: caching */
  /* BEGIN Custom change: caching */
  private final Map<String, IR> cache = HashMapFactory.make();
  private final Map<IClass, FakeAnnotationClass> annotationCache = HashMapFactory.make();
  private final IClassHierarchy cha;

  public GetAnnotationContextInterpreter(IClassHierarchy cha) {
    this.cha = cha;
  }

  @Override
  public IR getIR(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    assert understands(node);
    if (DEBUG) {
      System.err.println("generating IR for " + node);
    }
    /* BEGIN Custom change: caching */
    final Context context = node.getContext();
    final IMethod method = node.getMethod();
    final String hashKey = method.toString() + '@' + context.toString();

    IR result = cache.get(hashKey);

    if (result == null) {
      result = makeIR(method, context);
      cache.put(hashKey, result);
    }

    /* END Custom change: caching */
    return result;
  }

  @Override
  public IRView getIRView(CGNode node) {
    return getIR(node);
  }

  @Override
  public int getNumberOfStatements(CGNode node) {
    assert understands(node);
    return getIR(node).getInstructions().length;
  }

  @Override
  public boolean understands(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (!(node.getContext().isA(GetAnnotationContext.class))) {
      return false;
    }
    return node.getMethod().getReference().equals(GetAnnotationContextSelector.GET_ANNOTATION_CLASS)
        || node.getMethod()
            .getReference()
            .equals(GetAnnotationContextSelector.GET_ANNOTATION_CONSTRUCTOR)
        || node.getMethod().getReference().equals(GetAnnotationContextSelector.GET_ANNOTATION_FIELD)
        || node.getMethod()
            .getReference()
            .equals(GetAnnotationContextSelector.GET_ANNOTATION_METHOD)
        || node.getMethod()
            .getReference()
            .equals(GetAnnotationContextSelector.GET_ANNOTATION_PARAMETER);
  }

  @Override
  public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
    return EmptyIterator.instance();
  }

  @Override
  public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
    return EmptyIterator.instance();
  }

  private SSAInstruction[] makeStatements(
      Context context, Map<Integer, ConstantValue> constants) { // TODO !!!
    ArrayList<SSAInstruction> statements = new ArrayList<>();
    int nextLocal = 3;
    int retValue = nextLocal++;

    TypeReference trParam =
        ((FilteredPointerKey.SingleClassFilter) context.get(ContextKey.PARAMETERS[1]))
            .getConcreteType()
            .getReference();
    IClass klassParam = cha.lookupClass(trParam);

    Collection<Annotation> annots = null;
    ContextItem contextItem = context.get(ContextKey.PARAMETERS[0]);
    if (contextItem instanceof ContextItem.Value) {
      IMember memRec = (IMember) ((ContextItem.Value<?>) contextItem).getValue();
      annots = memRec.getAnnotations();
    } else if (contextItem instanceof FilteredPointerKey.SingleClassFilter) {
      TypeReference trRec =
          ((FilteredPointerKey.SingleClassFilter) contextItem).getConcreteType().getReference();
      IClass klassRec = cha.lookupClass(trRec);
      annots = klassRec.getAnnotations();
    } else {
      assert false;
    }

    Annotation annot = null;
    if (annots != null) {
      for (Annotation k : annots) {
        if (k.getType().equals(trParam)) {
          annot = k;
        }
      }
    }
    if (annot != null) {
      SSAInstructionFactory insts = Language.JAVA.instructionFactory();

      BypassSyntheticClassLoader loader =
          (BypassSyntheticClassLoader)
              cha.getLoader(cha.getScope().getLoader(Atom.findOrCreateUnicodeAtom("Synthetic")));
      FakeAnnotationClass clAnnot = annotationCache.get(klassParam);
      if (clAnnot == null) {
        clAnnot = new FakeAnnotationClass(loader.getReference(), cha, klassParam);
        annotationCache.put(klassParam, clAnnot);
      }

      loader.registerClass(TypeName.string2TypeName("Lcom/ibm/wala/FakeAnnotationClass"), clAnnot);

      Map<String, AnnotationsReader.ElementValue> annotMap = annot.getNamedArguments();
      for (String key : annotMap.keySet()) {
        clAnnot.addField(
            Atom.findOrCreateUnicodeAtom(key), getTRForElementValue(annotMap.get(key)));
        MethodReference mr =
            MethodReference.findOrCreate(
                clAnnot.getReference(),
                Atom.findOrCreateUnicodeAtom(key),
                Descriptor.findOrCreate(
                    new TypeName[0], getTRForElementValue(annotMap.get(key)).getName()));
        FakeAnnotationMethod method = new FakeAnnotationMethod(mr, cha, klassParam);
        clAnnot.addMethod(method);
      }

      int iindex = 0;
      NewSiteReference site = NewSiteReference.make(iindex, clAnnot.getReference());
      SSANewInstruction N = insts.NewInstruction(iindex++, retValue, site);
      statements.add(N);

      for (IField field : clAnnot.getAllFields()) {
        AnnotationsReader.ElementValue eValue = annotMap.get(field.getName().toString());
        if (eValue != null) {
          constants.put(nextLocal, new ConstantValue(eValue.toString()));
        }
        SSAPutInstruction P =
            insts.PutInstruction(iindex++, retValue, nextLocal++, field.getReference());
        statements.add(P);
      }

      SSAReturnInstruction R = insts.ReturnInstruction(statements.size(), retValue, false);
      statements.add(R);
    }

    return statements.toArray(new SSAInstruction[0]);
  }

  private TypeReference getTRForElementValue(ElementValue val) {
    if (val instanceof ConstantElementValue) {
      if (((ConstantElementValue) val).val instanceof String) {
        return TypeReference.JavaLangString;
      } else if (((ConstantElementValue) val).val instanceof Integer) {
        return TypeReference.JavaLangInteger;
      } else if (((ConstantElementValue) val).val instanceof Boolean) {
        return TypeReference.JavaLangBoolean;
      } else if (((ConstantElementValue) val).val instanceof Character) {
        return TypeReference.JavaLangCharacter;
      } else if (((ConstantElementValue) val).val instanceof Double) {
        return TypeReference.JavaLangDouble;
      } else if (((ConstantElementValue) val).val instanceof Float) {
        return TypeReference.JavaLangFloat;
      } else if (((ConstantElementValue) val).val instanceof Long) {
        return TypeReference.JavaLangLong;
      } else if (((ConstantElementValue) val).val instanceof Short) {
        return TypeReference.JavaLangShort;
      } else if (((ConstantElementValue) val).val instanceof Byte) {
        return TypeReference.JavaLangByte;
      } else if (((ConstantElementValue) val).val instanceof Class) {
        return TypeReference.JavaLangClass;
      }
    } else if (val instanceof EnumElementValue) {
      return TypeReference.JavaLangEnum;
    } else if (val instanceof ArrayElementValue) {
      TypeReference elemtr =
          getTRForElementValue(
              ((ArrayElementValue) val).vals[0]); // return TR of first element in array
      return elemtr.getArrayTypeForElementType();
    }
    return TypeReference.JavaLangClass;
  }

  private IR makeIR(IMethod method, Context context) {
    Map<Integer, ConstantValue> constants = HashMapFactory.make();
    SSAInstruction[] instrs = makeStatements(context, constants);
    return new SyntheticIR(
        method,
        context,
        new InducedCFG(instrs, method, context),
        instrs,
        SSAOptions.defaultOptions(),
        constants);
  }

  @Override
  public boolean recordFactoryType(CGNode node, IClass klass) {
    return false;
  }

  @Override
  public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
    return EmptyIterator.instance();
  }

  @Override
  public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
    return EmptyIterator.instance();
  }

  @Override
  public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode N) {
    return getIR(N).getControlFlowGraph();
  }

  @Override
  public DefUse getDU(CGNode node) {
    return new DefUse(getIR(node));
  }
}
