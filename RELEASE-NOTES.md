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
