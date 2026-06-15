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

### `Iterator2List` and `Iterator2Set` deprecated for removal

The classes `Iterator2List` and `Iterator2Set`, along with all their methods
and constructors, are deprecated for removal.

The static methods `Iterator2Collection.toList(Iterator)` and
`Iterator2Collection.toSet(Iterator)` are retained but now return plain
`List<T>` and `Set<T>` respectively instead of `Iterator2List<T>` and
`Iterator2Set<T>` wrappers.

**Effect for third-party consumers:**

- Code that uses `Iterator2List<T>` or `Iterator2Set<T>` as variable types,
  method parameters, or return types should be migrated to `List<T>` or
  `Set<T>` instead.

- Code that calls `Iterator2Collection.toList(...)` or
  `Iterator2Collection.toSet(...)` continues to work, but may need to
  update type declarations if they were explicitly typed as
  `Iterator2List<T>` or `Iterator2Set<T>`.

- The `:util` module now declares Guava as an `implementation`-scope
  dependency. This Guava dependency is the same one already used by the `:cast`,
  `:dalvik`, and `:scandroid` modules.

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
