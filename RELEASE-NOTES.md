# WALA Release Notes

## Version 1.9.0

### Functionality changes

#### `MultiMap.removeAll` now returns an empty set when the key is not present

Previously, `MultiMap.removeAll(k)` would return `null` when `k` was not a key
in the multi-map. Now, it returns an empty set, which is more consistent and
safer.

#### `Assertions.UNREACHABLE` now returns a generic type

The `Assertions.UNREACHABLE()`, `Assertions.UNREACHABLE(String)`, and
`Assertions.UNREACHABLE(Object)` methods now return a generic type `<T>`
instead of `void`. This allows callers to write `return Assertions.UNREACHABLE(…)`
in methods that return a value, eliminating the previously required pattern of
calling `Assertions.UNREACHABLE()` followed by a separate `return null;` (or
similar unreachable-`return` statement).

**Effect for third-party consumers:**

* Existing call sites that previously wrote:

  ```java
  Assertions.UNREACHABLE("message");
  return null;
  ```

  can now be simplified to:

  ```java
  return Assertions.UNREACHABLE("message");
  ```

* Existing call sites that just call `Assertions.UNREACHABLE()` as a
  statement without a following `return` continue to work unchanged.

#### `DFSDiscoverTimeIterator` and `DFSFinishTimeIterator` no longer extend `ArrayList`

`DFSDiscoverTimeIterator<T>` and `DFSFinishTimeIterator<T>` previously
extended `ArrayList<T>` (in addition to implementing `Iterator<T>`). They
now implement only `Iterator<T>` and `Serializable`, backed by an internal
stack instead of the inherited `ArrayList` storage.

**Effect for third-party consumers:**

* Code that called `ArrayList` or `List` methods on instances of these
  types (e.g. `size()`, `get()`, `contains()`, `add()`) will no longer
  compile.

* Code that declared variables, parameters, or return types as
  `ArrayList<T>` or `List<T>` referencing these iterators will need to be
  updated to use `Iterator<T>` instead.

* Code that only consumed these types via the `Iterator<T>` interface
  (`hasNext()`, `next()`, `forEachRemaining()`) is unaffected.

#### Fourteen `Atom` methods deprecated

The following methods of `com.ibm.wala.core.util.strings.Atom` are now
deprecated (with `@Deprecated(since = "1.9.0")`). They are no longer used
anywhere within WALA itself except by correctness tests, and we do not believe
they are used anywhere outside WALA:

* `arrayDescriptorFromElementDescriptor()`
* `concat(byte, ImmutableByteArray)`
* `contains(byte)`
* `findOrCreate(ImmutableByteArray, int, int)`
* `findOrCreateUtf8Atom(byte[])`
* `isArrayDescriptor()`
* `isArrayDescriptor(ImmutableByteArray)`
* `isClassDescriptor()`
* `isMethodDescriptor()`
* `isReservedMemberName()`
* `left(int)`
* `parseForArrayDimensionality()`
* `parseForArrayElementDescriptor()`
* `parseForInnermostArrayElementDescriptor()`

**Effect for third-party consumers:** if you do use any of these methods
outside WALA, please [let the WALA maintainers
know](https://github.com/wala/WALA/issues/new) so that we don't remove them in a
future release.

#### Analysis scope files may reference classes and sources packaged in JARs

In an analysis scope file, `classFile` and `sourceFile` entries previously
could only name files that exist on the filesystem.  They now also work when
the named class or source file is a classpath resource, including a resource
packaged inside a JAR.  For example, an entry such as
`Application,Java,classFile,hello/Hello.class` now resolves
`hello/Hello.class` via the classloader, whether it is an unpacked file or an
entry of `hello.jar`.  This is particularly useful when running an analysis
from a single executable JAR whose supporting classes and sources are
included as entries.

#### `Atom` interning is now lock-free

The `Atom` interning dictionary now uses a JCTools `NonBlockingHashMap`
instead of a plain `HashMap`. The interning factories `findOrCreate(byte[])`,
`findOrCreate(ImmutableByteArray)`, and the deprecated
`findOrCreate(ImmutableByteArray, int, int)`, along with
`resetDictionaryForTesting()`, are no longer `synchronized`. The common
"already interned" lookup path is now lock-free, and when two threads race to
intern the same content, `putIfAbsent` ensures that only one canonical `Atom`
instance survives.

**Effect for third-party consumers:** No API changes: `Atom` interning still
guarantees one canonical instance per content, comparable with `==`, and
remains thread-safe. Concurrent interning no longer contends on a class-level
monitor.

### Dependency changes

#### `:core` now depends on `jctools-core`

The `:core` module now declares `org.jctools:jctools-core:4.0.6` as an
`implementation`-scope dependency. JCTools provides the lock-free
`NonBlockingHashMap` used by `Atom` interning.

**Effect for third-party consumers:** The `:core` module now receives
`jctools-core` on its runtime classpath. Consumers who run WALA from a
manually assembled classpath must include `jctools-core` on it, or `Atom`
interning will fail at runtime with a `NoClassDefFoundError`.

## Version 1.8.0

### Functionality changes

WALA now requires Java 17+ JVM to run, hence the bump to version 1.8.0.
Beyond this change, there have been various cleanups and bug fixes.

#### Key pull requests

* Require Java 17+ across all of WALA by @liblit in
  [#1904](https://github.com/wala/WALA/pull/1904)
* perf(dominators): empty bucket while iterating in step 2 by @toxamin in
  [#1897](https://github.com/wala/WALA/pull/1897)
* Gate PropagationSystem's implicit-key debug dump under `DEBUG` by @khatchad
  in [#1934](https://github.com/wala/WALA/pull/1934)
* Deprecate the unused `options` parameter on `Language.getFakeRootMethod` by
  @khatchad in [#1933](https://github.com/wala/WALA/pull/1933)
* Convert many classes to Java 16+ records by @liblit in
  [#1932](https://github.com/wala/WALA/pull/1932)
* Deprecate `Iterator2List` and `Iterator2Set` by @liblit in
  [#1950](https://github.com/wala/WALA/pull/1950)
* Register summary-modeled class shells before CHA build (#1957) by @khatchad
  in [#1958](https://github.com/wala/WALA/pull/1958)
* Expose a summary class shell's own methods by @khatchad in
  [#1962](https://github.com/wala/WALA/pull/1962)
* Fix `Language`/`JavaLanguage` initialization deadlock by @liblit in
  [#1966](https://github.com/wala/WALA/pull/1966)
* Keep summary parameter names that begin with "arg" by @khatchad in
  [#1972](https://github.com/wala/WALA/pull/1972)

**Full Changelog**:
[v1.7.2...v1.8.0](https://github.com/wala/WALA/compare/v1.7.2...v1.8.0)

### Dependency changes

#### `:core` and `:util` now declare Guava as `implementation`-scope dependencies

Guava (`com.google.guava:guava`) is now declared as an `implementation`-scope
dependency in the `:core` and `:util` subprojects.

**Effect for third-party consumers:** The `:core` and `:util` modules now
receive Guava on their runtime classpaths. This is the same Guava dependency
already used by the `:cast`, `:dalvik`, and `:scandroid` modules.

#### JSpecify 1.0.0 promoted to `api` scope project-wide

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

### API changes

#### `Iterator2List` and `Iterator2Set` deprecated for removal

The classes `Iterator2List` and `Iterator2Set`, along with all their methods
and constructors, are deprecated for removal.

The static methods `Iterator2Collection.toList(Iterator)` and
`Iterator2Collection.toSet(Iterator)` are retained but now return plain
`List<T>` and `Set<T>` respectively instead of `Iterator2List<T>` and
`Iterator2Set<T>` wrappers.

**Effect for third-party consumers:**

* Code that uses `Iterator2List<T>` or `Iterator2Set<T>` as variable types,
  method parameters, or return types should be migrated to `List<T>` or
  `Set<T>` instead.

* Code that calls `Iterator2Collection.toList(...)` or
  `Iterator2Collection.toSet(...)` continues to work, but may need to
  update type declarations if they were explicitly typed as
  `Iterator2List<T>` or `Iterator2Set<T>`.

#### `Language.getFakeRootMethod` no longer takes `AnalysisOptions`

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

#### Class-to-record conversions

Several public classes have been converted to Java 16+ `record` classes. As a
result, the implicitly-defined record accessor methods use the uncapitalized
field name (e.g., `type()` instead of `getType()`). Deprecated backward-
compatibility bridge methods (`getXyz()`) that delegate to the new accessors
have been provided for each affected record, annotated with
`@Deprecated(forRemoval = true, since = "1.8.0")`.

##### Affected types and their renamed accessors

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
