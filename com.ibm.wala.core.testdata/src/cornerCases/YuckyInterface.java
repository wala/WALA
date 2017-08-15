/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cornerCases;

import sun.java2d.FontSupport;

/**
 * @author sfink
 *
 * When analyzed with J2EEClassHierarchy exclusions, the superinterface
 * FontSupport should not be found because we exclude sun.java2d.*
 */
@SuppressWarnings("restriction")
public interface YuckyInterface extends FontSupport {


}
