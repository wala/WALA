/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * This file is a derivative of code released under the terms listed below.
 *
 */
/*
*  Copyright (c) 2013,
*      Tobias Blaschke <code@tobiasblaschke.de>
*  All rights reserved.

*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  1. Redistributions of source code must retain the above copyright notice,
*     this list of conditions and the following disclaimer.
*
*  2. Redistributions in binary form must reproduce the above copyright notice,
*     this list of conditions and the following disclaimer in the documentation
*     and/or other materials provided with the distribution.
*
*  3. The names of the contributors may not be used to endorse or promote
*     products derived from this software without specific prior written
*     permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package com.ibm.wala.dalvik.ipa.callgraph.androidModel;

import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.dalvik.util.AndroidComponent;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.util.CancelException;

/**
 * Models all classes derived from the given AndroidComponent.
 *
 * <p>So for example it contains all EntryPoints from all Activities.
 *
 * <p>It is like a "regular" AndroidModel but the calls are restricted to EntryPoints whose
 * target-class is of the type of the given AndroidComponent.
 *
 * <p>In the ClassHierarchy a MiniModel will be known as "AndroidModelClass.???Model" (where ??? is
 * the AndroidComponent) and be called by "AndroidModelClass.startUnknown???" (which is generated by
 * the UnknownTargetModel).
 *
 * <p>A MiniModel is used when a startComponent-call (startActivity, bindService, ...) is
 * encountered, but the Context at the call site is insufficient to determine the actual target. In
 * this case an UnknownTargetModel which uses an MiniModel and an ExternalModel is placed there.
 *
 * @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 * @since 2013-10-29
 */
public class MiniModel extends AndroidModel {

  private final Atom name;
  private final AndroidComponent forCompo;

  /**
   * Restrict the model to Activities.
   *
   * <p>{@inheritDoc}
   */
  @Override
  protected boolean selectEntryPoint(AndroidEntryPoint ep) {
    if (ep.belongsTo(forCompo)) {

      return true;
    }
    return false;
  }

  @Override
  public Descriptor getDescriptor() throws CancelException {
    final Descriptor descr = super.getDescriptor();

    return descr;
  }

  public MiniModel(
      final IClassHierarchy cha,
      final AnalysisOptions options,
      final IAnalysisCacheView cache,
      final AndroidComponent forCompo) {
    super(cha, options, cache);

    this.forCompo = forCompo;
    this.name = Atom.findOrCreateAsciiAtom(forCompo.getPrettyName() + "Model");

    // this.activityModel = getMethod();
  }

  @Override
  public Atom getName() {
    return this.name;
  }

  @Override
  public SummarizedMethod getMethod() throws CancelException {
    if (!built) {
      super.build(this.name);
      this.register(super.model);
    }

    return super.model;
  }

  private void register(SummarizedMethod model) {
    AndroidModelClass mClass = AndroidModelClass.getInstance(cha);
    if (!mClass.containsMethod(model.getSelector())) {
      mClass.addMethod(super.model);
    }
  }

  @Override
  public String toString() {
    return "<" + this.getClass() + " name=" + this.name + " for=" + forCompo + " />";
  }
}
