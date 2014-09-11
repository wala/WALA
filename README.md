# About WALA

This is a fork of the program analysis framework WALA. The original framework can be found at <a href="http://wala.sourceforge.net">wala.sourceforge.net</a> and <a href="https://github.com/wala/WALA">github.com/wala/WALA</a>.

This version is tailored for the information flow control framework JOANA. See <a href="http://joana.ipd.kit.edu">joana.ipd.kit.edu</a> and <a href="https://github.com/jgf/joana">github.com/jgf/joana</a> for details.

This fork includes some additional features:
- null-pointer detection that removes spurious CFG edges.
- it includes the code for a dalivk bytecode frontend copied and integrated from <a href="https://github.com/SCanDroid/SCanDroid">github.com/SCanDroid/SCanDroid</a>.
- configuration/property files are loaded from current directory, with an automatic fallback to load from .jar.
- SDG implementation that has different edge types for control and data dependencies.
- every SSAInstrution knows its index.
- many other minor changes.
