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
package com.ibm.wala.ipa.summaries;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.types.TypeName;
import java.util.Collection;

/**
 * A capability, implemented by class loaders that can introduce "shell" classes for types modeled
 * only by a summary, so that source classes subclassing such a type can resolve their base at
 * definition time rather than falling back to the language root.
 *
 * <p>This exists because a class resolves its superclass through its own loader; a summary-modeled
 * framework base materialized late (and in the {@link BypassSyntheticClassLoader}) is invisible to
 * a source subclass at class-hierarchy-build time. Loaders that own a mutable set of classes (the
 * {@link BypassSyntheticClassLoader} and the CAst module loaders) can instead register a shell
 * eagerly, before the hierarchy is built. See <a
 * href="https://github.com/wala/WALA/issues/1957">#1957</a>.
 *
 * @see Util#addSummaryClassShells(SummaryClassShellLoader, XMLMethodSummaryReader)
 */
public interface SummaryClassShellLoader extends IClassLoader {

  /**
   * Register a shell {@link IClass} for a summary-modeled type, so that classes loaded by this
   * loader can resolve it as a superclass.
   *
   * <p>Idempotent: if a class is already registered under {@code name}, it is returned unchanged.
   *
   * @param name the type to register a shell for
   * @param superName the shell's superclass, or {@code null} for this loader's language root type
   * @return the registered (or pre-existing) class
   */
  IClass defineSummaryClassShell(TypeName name, TypeName superName);

  /**
   * Register a shell {@link IClass} that also exposes the given method summaries as its own
   * declared methods, so that a walk of the shell (e.g. when resolving an inherited method on a
   * subclass) finds them. The method bodies are still bound late by the usual bypass mechanism;
   * only the declarations need to exist on the shell.
   *
   * <p>The default implementation ignores {@code methods} and delegates to {@link
   * #defineSummaryClassShell(TypeName, TypeName)}; implementations whose shells can carry methods
   * (such as the {@link BypassSyntheticClassLoader}) override this.
   *
   * @param name the type to register a shell for
   * @param superName the shell's superclass, or {@code null} for this loader's language root type
   * @param methods summaries for the methods the shell should declare
   * @return the registered (or pre-existing) class
   */
  default IClass defineSummaryClassShell(
      TypeName name, TypeName superName, Collection<MethodSummary> methods) {
    return defineSummaryClassShell(name, superName);
  }
}
