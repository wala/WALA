//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package cornerCases;

import sun.java2d.FontSupport;

/**
 * @author sfink
 *
 * When analyzed with J2EEClassHierarchy exclusions, the superinterface
 * FontSupport should not be found because we exclude sun.java2d.*
 */
public interface YuckyInterface extends FontSupport {


}
