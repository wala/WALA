/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package cornerCases;

import javax.swing.tree.RowMapper;

/**
 * @author sfink
 *     <p>When analyzed with Java60RegressionExclusions exclusions, the superinterface RowMapper
 *     should not be found because we exclude javax.swing.*
 */
public interface YuckyInterface extends RowMapper {}
