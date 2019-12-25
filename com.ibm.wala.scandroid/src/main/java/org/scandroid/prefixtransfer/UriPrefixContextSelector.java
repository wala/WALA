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
 *
 * Copyright (c) 2009-2012,
 *
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.scandroid.prefixtransfer;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.NormalAllocationInNode;
import com.ibm.wala.ipa.callgraph.propagation.ReceiverInstanceContext;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallerSiteContext;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallerSiteContextPair;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.intset.IntSet;

public class UriPrefixContextSelector extends DefaultContextSelector {

  // private final ContextSelector delegate;

  public UriPrefixContextSelector(AnalysisOptions options, IClassHierarchy cha) {
    super(options, cha);
  }

  /* TODO: fix receivers[0] references. */
  @Override
  public Context getCalleeTarget(
      CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receivers) {
    if (callee.getSignature().equals("java.lang.StringBuilder.toString()Ljava/lang/String;")
        || callee
            .getSignature()
            .equals("java.lang.StringBuilder.append(Ljava/lang/String;)Ljava/lang/StringBuilder;")
        || callee
            .getSignature()
            .equals("java.lang.String.valueOf(Ljava/lang/Object;)Ljava/lang/String;")
        || callee.getSignature().equals("java.lang.String.toString()Ljava/lang/String;")
        || callee
            .getSignature()
            .equals("android.net.Uri.parse(Ljava/lang/String;)Landroid/net/Uri;")
        || callee
            .getSignature()
            .equals(
                "android.net.Uri.withAppendedPath(Landroid/net/Uri;Ljava/lang/String;)Landroid/net/Uri;")
        ||
        //
        // callee.getSignature().equals("android.net.Uri$StringUri.buildUpon()Landroid/net/Uri$Builder;") ||
        //
        // callee.getSignature().equals("android.net.Uri$OpaqueUri.buildUpon()Landroid/net/Uri$Builder;") ||
        callee.getSignature().equals("android.net.Uri$Builder.build()Landroid/net/Uri;")) {
      //            System.out.println("Adding context to "+callee.getSignature());
      //            for (int i = 0; i < receivers.length; i++)
      //            	System.out.println("\t#"+i+" "+receivers[i]);
      if (receivers[0] instanceof NormalAllocationInNode) {
        //                System.out.println("\t\tNormalAllocationInNode "+callee.getSignature());
        if (((NormalAllocationInNode) receivers[0])
            .getSite()
            .getDeclaredType()
            .getClassLoader()
            .equals(ClassLoaderReference.Application))
          // create a context based on the site and the receiver
          return new CallerSiteContextPair(caller, site, new ReceiverInstanceContext(receivers[0]));
        //                if
        // (callee.getSignature().equals("android.net.Uri$StringUri.buildUpon()Landroid/net/Uri$Builder;") ||
        //
        //	callee.getSignature().equals("android.net.Uri$OpaqueUri.buildUpon()Landroid/net/Uri$Builder;") ||
        //
        //	callee.getSignature().equals("android.net.Uri$Builder.build()Landroid/net/Uri;"))
        //                	return new CallerSiteContextPair(caller,site,new
        // ReceiverInstanceContext(receivers[0]));
        if (callee.getSignature().equals("android.net.Uri$Builder.build()Landroid/net/Uri;"))
          return new CallerSiteContextPair(caller, site, new ReceiverInstanceContext(receivers[0]));
        // return new CallerSiteContext(caller,site);
      } else if (callee
              .getSignature()
              .equals("java.lang.String.valueOf(Ljava/lang/Object;)Ljava/lang/String;")
          || callee.getSignature().equals("java.lang.String.toString()Ljava/lang/String;")
          || callee
              .getSignature()
              .equals("android.net.Uri.parse(Ljava/lang/String;)Landroid/net/Uri;")
          || callee
              .getSignature()
              .equals(
                  "android.net.Uri.withAppendedPath(Landroid/net/Uri;Ljava/lang/String;)Landroid/net/Uri;")) {
        return new CallerSiteContext(caller, site);
      }
    } else // if(callee.getSignature().contains("value"))
    {
      //          System.out.println("Found signature: "+callee.getSignature());
    }
    if (!caller.getContext().equals(Everywhere.EVERYWHERE)) {
      // System.out.println("Call to "+callee+" from caller "+caller+" in context
      // "+caller.getContext());
    }
    return super.getCalleeTarget(caller, site, callee, receivers);
  }

  @Override
  public IntSet getRelevantParameters(CGNode node, CallSiteReference call) {
    return super.getRelevantParameters(node, call);
  }
}
