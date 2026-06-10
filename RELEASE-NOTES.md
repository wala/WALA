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
classpaths automatically, matching [JSpecify’s recommendation that the
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
