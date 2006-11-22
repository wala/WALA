/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.logging;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import com.ibm.wala.util.internationalization.StringBundle;

/**
 * Specific formatter used for every messages coming from logging activities wanted 
 * on standard output and logging file. 
 * <p>
 * Used by default by 'log.properties'.
 * <p>
 * You can define your own logging properties file using default formatters, or your
 * own dedicated handlers and formatters.
 * <p>
 * To use those specific logging properties, identity location of 'log.properties' file via
 * next VM argument: -Djava.util.logging.config.file=[your path]/log.properties
 * 
 * @author egeay
 */
public final class SimplifiedFormatter extends Formatter {
  
  //--- Abstract method implementation

  /**
   * Formats the given log record and returns the formatted string.
   * This specific formatter do not show the time and shows the source information
   * only if they are differents from the root logger ones.
   */
  public String format( final LogRecord logRecord ) {
    final StringBuffer buf = new StringBuffer();
    
    final String lineSeparator = System.getProperty( "line.separator" ); //$NON-NLS-1$
    if( ! ROOT_LOGGER_NAME.equals( logRecord.getSourceClassName() ) ) {
      buf.append( StringBundle.getInstance().get( getClass(), "source_location", //$NON-NLS-1$
                                                  new Object[] { 
                                                       logRecord.getSourceClassName(),
                                                       logRecord.getSourceMethodName() } ) );
      buf.append( lineSeparator );
    }
      
    buf.append( logRecord.getLevel().getLocalizedName() );
    buf.append( ": " ); //$NON-NLS-1$
    buf.append( formatMessage( logRecord ) );
    buf.append( lineSeparator );
    
    if ( logRecord.getThrown() != null ) {
      buf.append( getStackStrace( logRecord.getThrown() ) );
    }
      
    return buf.toString();
  }
  
  //--- Private code
  
  private String getStackStrace( final Throwable exception ) {
    final StringWriter strWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter( strWriter );
    exception.printStackTrace( printWriter );
    printWriter.close();
    return strWriter.toString();
  }
  
  private static final String ROOT_LOGGER_NAME = "java.util.logging.LogManager$RootLogger"; //$NON-NLS-1$

}
