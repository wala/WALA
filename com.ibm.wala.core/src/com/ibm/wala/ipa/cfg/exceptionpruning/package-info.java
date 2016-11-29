/**
 * If we want to filter edges of a control flow graph we already have
 * {@link com.ibm.wala.ipa.cfg.EdgeFilter}, but if we want to remove
 * exceptions in particular we may want to combine different analysis.
 * Therefore we need a possibility to collect a set of removed exceptions,
 * so that an exceptional edge may be removed as soon as all exceptions,
 * which can occur along these edge are filtered.
 *
 * This package contains classes for this job and also adapter for some
 * analysis.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
package com.ibm.wala.ipa.cfg.exceptionpruning;