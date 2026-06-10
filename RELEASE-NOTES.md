# WALA Release Notes

## Dependency changes

### JSpecify 1.0.0 promoted to `api` scope project-wide

JSpecify (`org.jspecify:jspecify:1.0.0`) is now declared as an `api`-scope
(transitive) dependency in every WALA subproject that exports annotated APIs:
`:cast`, `:cast:java:ecj`, `:cast:js`, `:cast:js:html:nu_validator`,
`:cast:js:nodejs`, `:cast:js:rhino`, `:core`, `:dalvik`, `:ide`, `:ide:jdt`,
`:scandroid`, and `:shrike`. In `:ide` the dependency was also promoted from
`implementation` to `api`.

**Effect for third-party consumers:** Any project depending on those WALA
modules will receive `org.jspecify:jspecify:1.0.0` on its compile and runtime
classpaths automatically, matching [JSpecify's recommendation that the
annotations library be an API dependency whenever the annotations appear in
exported API surfaces](https://jspecify.dev/docs/using/#gradle).

## API changes

### `Language.getFakeRootMethod` no longer takes `AnalysisOptions`

`Language.getFakeRootMethod(IClassHierarchy, IAnalysisCacheView)` is now the
implemented method. The previous
`getFakeRootMethod(IClassHierarchy, AnalysisOptions, IAnalysisCacheView)`
overload is retained as a `@Deprecated(forRemoval = true, since = "1.8.0")`
default that delegates to it and ignores the unused `options` argument. The
`AstCallGraph.ScriptFakeRoot` and `CrossLanguageCallGraph.CrossLanguageFakeRoot`
constructors that take an unused `AnalysisOptions` are deprecated the same way.

**Effect for third-party consumers:** Classes that implement `Language`
directly must implement the new two-argument `getFakeRootMethod`; the
three-argument form is no longer abstract. Callers of the deprecated overload,
or of the deprecated fake-root constructors, should drop the `AnalysisOptions`
argument before the deprecated members are removed in a future release.

### Class-to-record conversions

Several public classes have been converted to Java 16+ `record` classes. As a
result, the implicitly-defined record accessor methods use the uncapitalized
field name (e.g., `type()` instead of `getType()`). Deprecated backward-
compatibility bridge methods (`getXyz()`) that delegate to the new accessors
have been provided for each affected record, annotated with
`@Deprecated(forRemoval = true, since = "1.8.0")`.

#### Affected types and their renamed accessors

<!-- markdownlint-disable MD013 -->
| Type | Old accessor | New accessor |
|-|-|-|
| `AllocationSite` | `getMethod()`, `getSite()`, `getConcreteType()` | `method()`, `site()`, `concreteType()`                    |
| `AllocationString` | `getAllocationSites()` | `allocationSites()`                                       |
| `CallGraphResult` | `getCallGraph()`, `getPointerAnalysis()`, `getFlowGraph()` | `callGraph()`, `pointerAnalysis()`, `flowGraph()`         |
| `ClassLoaderReference` | `getName()`, `getLanguage()`, `getParent()` | `name()`, `language()`, `parent()`                        |
| `ColoredVertices` | `isFullColoring()`, `getColors()`, `getNumColors()` | `fullColoring()`, `colors()`, `numColors()`               |
| `ConcreteTypeKey` | `getType()`, `getConcreteType()` | `type()`                                                  |
| `DomainElement` | direct field access `codeElement`, `taintSource` | `codeElement()`, `taintSource()`                          |
| `FilteredException` | `getException()` | `exception()`                                             |
| `FilteredPointerKey.SingleClassFilter` | `getConcreteType()` | `concreteType()`                                          |
| `GlobalObjectKey` | `getConcreteType()` | `concreteType()`                                          |
| `InstanceKey` (interface) | `getConcreteType()` | `concreteType()`                                          |
| `InstructionByIIndexWrapper` | `getInstruction()` | `instruction()`                                           |
| `IntPair` | `getX()`, `getY()` | `x()`, `y()`                                              |
| `JavaTypeContext` | `getType()` | `type()`                                                  |
| `MemoryAccess` | `getInstructionIndex()`, `getNode()` | `instructionIndex()`, `node()`                            |
| `MethodNamePattern` | `getDescriptor()`, `getClassName()`, `getMemberName()` | `descriptor()`, `className()`, `memberName()`             |
| `PointerKeyAndCallSite` | `getKey()`, `getCallSiteRef()` | `key()`, `callSiteRef()`                                  |
| `ReferenceToken` | `getKind()`, `getClassName()`, `getElementName()`, `getDescriptor()` | `kind()`, `className()`, `elementName()`, `descriptor()`  |
| `Selector` | `getName()`, `getDescriptor()` | `name()`, `descriptor()`                                  |
| `StackMapConstants.StackMapFrame` | `getFrameType()`, `getOffset()`, `getLocalTypes()`, `getStackTypes()` | `frameType()`, `offset()`, `localTypes()`, `stackTypes()` |
| `Weight` | `getType()`, `getNumber()` | `type()`, `number()`                                      |
<!-- markdownlint-enable MD013 -->

**Migration:** replace calls to the old `getXyz()` accessors with the new
`xyz()` record accessors. The deprecated bridge methods will be removed in a
future release.
