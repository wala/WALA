package com.ibm.wala.cast.js.html;


/**
 * Callback which is implemented by users of the IHtmlParser. The parser traverses the dom-nodes in an in-order. 
 * @author danielk
 * @author yinnonh
 *
 */
public interface IHtmlCallback {

	void handleStartTag(ITag tag);

	void handleEndTag(ITag tag);

}
