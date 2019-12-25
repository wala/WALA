/*
 * Copyright (c) 2002 - 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.html;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;

/**
 * Callback which is implemented by users of the IHtmlParser. The parser traverses the dom-nodes in
 * an in-order.
 *
 * @author danielk
 * @author yinnonh
 */
public interface IHtmlCallback {

  void handleStartTag(ITag tag);

  void handleText(Position pos, String text);

  void handleEndTag(ITag tag);
}
