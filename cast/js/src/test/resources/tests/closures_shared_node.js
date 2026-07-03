// Regression test for https://github.com/wala/WALA/issues/1990: `step`'s two closures
// dispatch through the single call site in `invoke`, whose 1-CFA call string truncates the
// callers apart, so one call graph node serves both closures. The identity chain delays the
// second closure so it reaches that node only after its constraints were first generated;
// without re-resolution, `late` never becomes a callee of `step`.
function early() {
  return 3;
}

function late() {
  return 4;
}

function make(target) {
  function step() {
    return target();
  }
  return step;
}

function id1(f) {
  return f;
}

function id2(f) {
  return id1(f);
}

function id3(f) {
  return id2(f);
}

function invoke(f) {
  return f();
}

var a = make(early);
invoke(a);

var b = id3(make(late));
invoke(b);
