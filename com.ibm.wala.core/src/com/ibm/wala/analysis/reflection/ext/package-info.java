/**
 * This package provides a context, a context interpreter and a context selector
 * to handle the special case of calls to {@link java.lang.Class#getMethod}  and
 * {@link java.lang.Class#getDeclaredMethod} with higher precision than   usual.
 * 
 * The context interpreter and context selector should both be placed in   front
 * of   {@link com.ibm.wala.analysis.reflection.JavaLangClassContextInterpreter}
 * and {@link com.ibm.wala.analysis.reflection.JavaLangClassContextSelector}  in
 * order to work.
 * 
 * Problem description:
 * {@link com.ibm.wala.analysis.reflection.JavaLangClassContextInterpreter}     and
 * and {@link com.ibm.wala.analysis.reflection.JavaLangClassContextSelector} ignore
 * the chance to optimize the set of methods returned if the first      argument to
 * {@link java.lang.Class#getMethod} or   {@link java.lang.Class#getDeclaredMethod}
 * is a string constant.
 * @author
 *  Michael Heilmann
 *
 */
package com.ibm.wala.analysis.reflection.ext;