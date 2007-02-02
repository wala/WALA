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
package com.ibm.wala.cast.tree;

import java.io.*;

/**
 *  Encapsulates a translator from source files to CAstEntities.  This
 * interface is meant ease the creation of CAst consumers that can
 * take asts from multiple sources.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public interface TranslatorToCAst {

  CAstEntity translate(Reader file, String fileName) throws IOException;

}
