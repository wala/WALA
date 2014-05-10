/******************************************************************************
 * Copyright (c) 2002 - 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.html;

import java.io.Reader;
import java.net.URL;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;

/**
 * @author danielk
 * @author yinnonh
 * Parses an HTML file using call backs
 */
public interface IHtmlParser {

    /**
     * Parses a given HTML, calling the given callback.
     * @param reader
     * @param callback
     * @param fileName
     * @throws Error 
     */
    public void parse(URL url, Reader reader, IHtmlCallback callback, String fileName) throws Error;

}
