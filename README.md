![WALA logo](http://wala.sourceforge.net/wiki/images/9/94/WALA-banner.png)]

[![GitHub Actions status](https://github.com/wala/WALA/workflows/Continuous%20integration/badge.svg)](https://github.com/wala/WALA/actions?query=workflow%3A%22Continuous+integration%22) [![Build Status](https://travis-ci.org/wala/WALA.svg?branch=master)](https://travis-ci.org/wala/WALA) [![Join the chat at https://gitter.im/WALAHelp/Lobby](https://badges.gitter.im/WALAHelp/Lobby.svg)](https://gitter.im/WALAHelp/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
=====================

The T. J. Watson Libraries for Analysis (WALA) provide static analysis capabilities for Java bytecode and related languages and for JavaScript. The system is licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html), which has been approved by the [OSI](http://www.opensource.org/) (Open Source Initiative) as a fully certified open source license. The initial WALA infrastructure was independently developed as part of the DOMO research project at the [IBM T.J. Watson Research Center](http://www.research.ibm.com/). In 2006, [IBM](http://www.ibm.com/us/) donated the software to the community.

For recent updates on WALA, join the [mailing list](http://sourceforge.net/p/wala/mailman/), or follow WALA on [Twitter](https://twitter.com/WALALibs) or [Google+](https://plus.google.com/117805259258761505726/posts).

### Core WALA Features

WALA features include:

* Java type system and class hierarchy analysis
* Source language framework supporting Java and JavaScript
* Interprocedural dataflow analysis ([RHS](http://www.cs.wisc.edu/~reps/#popl95) solver)
* Context-sensitive tabulation-based slicer
* Pointer analysis and call graph construction
* SSA-based register-transfer language IR
* General framework for iterative dataflow
* General analysis utilities and data structures
* A bytecode instrumentation library ([[Shrike]]) and a dynamic load-time instrumentation library for Java ([[Dila]])

### WALA Tools in JavaScript

Recently, we have been expanding the set of WALA tools implemented in JavaScript. We have released a normalizer and some basic program analyses for JavaScript in the [JS_WALA GitHub repository](https://github.com/wala/JS_WALA). We have also made available [WALA Delta](https://github.com/wala/WALADelta), a delta debugger for JavaScript-processing tools. Please see the linked GitHub repositories for further details on these tools.

### WALA-Based Tools

Several groups have built open-source tools that enhance or build on WALA that may be useful to other WALA users. For details, see the [Wala-based tools](WALA-Based-Tools) page.

### About this Wiki

We're hosting all documentation for WALA on this wiki. **We've chosen a wiki format just so that _you_ can contribute.** Don't be shy!

<!---
TODO

Replace the sourceforge mailing list link w/ somewhere else?
-->

The WALA publications department is populating this wiki with technical documentation on a demand-driven basis, driven by questions posted to the [wala-wala](http://sourceforge.net/p/wala/mailman/) mailing list. We recommend [this page](https://groups.google.com/forum/#!forum/wala-sourceforge-net) for searching the mailing list archives.

Currently, we have the [JavaDoc documentation for the WALA code](TODO) being uploaded once per day. If you think a particular file deserves better javadoc, please [open a feature request](https://github.com/wala/WALA/issues).

WALA uses Gradle as its build system.  If you intend to modify or
build WALA yourself, then see [the Gradle-specific
README](README-Gradle.md) for more instructions and helpful tips.  You
may also find `pom.xml` configuration files for Maven builds, but
Maven is no longer well supported; use Gradle if at all possible.
