![WALA logo](http://wala.sourceforge.net/wiki/images/9/94/WALA-banner.png)

[![GitHub Actions status](https://github.com/wala/WALA/workflows/Continuous%20integration/badge.svg)](https://github.com/wala/WALA/actions?query=workflow%3A%22Continuous+integration%22) [![Join the chat at https://gitter.im/WALAHelp/Lobby](https://badges.gitter.im/WALAHelp/Lobby.svg)](https://gitter.im/WALAHelp/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

-------------------------

The T. J. Watson Libraries for Analysis (WALA) provide static analysis capabilities for Java bytecode and related languages and for JavaScript. The system is licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html), which has been approved by the [OSI](http://www.opensource.org/) (Open Source Initiative) as a fully certified open source license. The initial WALA infrastructure was independently developed as part of the DOMO research project at the [IBM T.J. Watson Research Center](http://www.research.ibm.com/). In 2006, [IBM](http://www.ibm.com/us/) donated the software to the community.

For recent updates on WALA, join the [mailing list](http://sourceforge.net/p/wala/mailman/).

## Core WALA Features

WALA features include:

* Java type system and class hierarchy analysis
* Source language framework supporting Java and JavaScript
* Interprocedural dataflow analysis ([RHS](http://www.cs.wisc.edu/~reps/#popl95) solver)
* Context-sensitive tabulation-based slicer
* Pointer analysis and call graph construction
* SSA-based register-transfer language IR
* General framework for iterative dataflow
* General analysis utilities and data structures
* A bytecode instrumentation library ([Shrike](https://github.com/wala/WALA/wiki/Shrike))

## Getting Started

The fastest way to get started with WALA is to use the packages in Maven Central, as noted [here](https://github.com/wala/WALA/wiki/Getting-Started#quick-start-using-maven-central-packages).  See the [WALA-start](https://github.com/wala/WALA-start) repo for a Gradle-based example.  We are actively re-organizing the deeper wiki technical documentation.  In the meantime, you can check out tutorial slides to get an overview of WALA:

* [Core WALA](http://wala.sourceforge.net/files/PLDI_WALA_Tutorial.pdf) (PDF)
* [WALA JavaScript](http://wala.sourceforge.net/files/WALAJavaScriptTutorial.pdf) (PDF)

You can also watch screencasts of the WALA JavaScript tutorial [here](https://www.youtube.com/user/WALALibraries/videos).

Finally, for now, to search the wiki documentation, we recommend a site-specific search on Google, e.g., [a search for "call graph"](https://www.google.com/search?q=call+graph+site%3Ahttps%3A%2F%2Fgithub.com%2Fwala%2FWALA%2Fwiki&oq=call+graph+site%3Ahttps%3A%2F%2Fgithub.com%2Fwala%2FWALA%2Fwiki).

## Documentation

We're hosting documentation for WALA on [the GitHub wiki](https://github.com/wala/WALA/wiki).  **We've chosen a wiki format just so that _you_ can contribute.** Don't be shy!

The WALA publications department is populating this wiki with technical documentation on a demand-driven basis, driven by questions posted to the [wala-wala](http://sourceforge.net/p/wala/mailman/) mailing list and also [Gitter](https://gitter.im/WALAHelp/Lobby). We recommend [this page](https://groups.google.com/forum/#!forum/wala-sourceforge-net) for searching the mailing list archives.

Currently, we have the [JavaDoc documentation for the WALA code](https://wala.github.io/javadoc) being updated continuously. If you think a particular file deserves better javadoc, please [open a feature request](https://github.com/wala/WALA/issues).

## Getting Help

To get help with WALA, please either [email the mailing list](http://sourceforge.net/p/wala/mailman/), [ask a question on Gitter](https://gitter.im/WALAHelp/Lobby), or [open an issue](https://github.com/wala/WALA/issues).

## Building from Source

WALA uses Gradle as its build system.  If you intend to modify or build WALA yourself, then see [the Gradle-specific README](README-Gradle.md) for more instructions and helpful tips.  You may also find `pom.xml` configuration files for Maven builds, but Maven is no longer well supported; use Gradle if at all possible.

## WALA Tools in JavaScript

Recently, we have been expanding the set of WALA tools implemented in JavaScript. We have released a normalizer and some basic program analyses for JavaScript in the [JS_WALA GitHub repository](https://github.com/wala/JS_WALA). We have also made available [jsdelta](https://github.com/wala/jsdelta) and [WALA Delta](https://github.com/wala/WALADelta), delta debuggers for JavaScript-processing tools. Please see the linked GitHub repositories for further details on these tools.

## WALA-Based Tools

Several groups have built open-source tools that enhance or build on WALA that may be useful to other WALA users. For details, see the [Wala-based tools](https://github.com/wala/WALA/wiki/WALA-Based-Tools) page.
