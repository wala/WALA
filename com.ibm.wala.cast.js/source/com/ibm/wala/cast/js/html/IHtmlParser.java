package com.ibm.wala.cast.js.html;

import java.io.Reader;

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
     */
    public void parse(Reader reader, IHtmlCallback callback, String fileName);

}
