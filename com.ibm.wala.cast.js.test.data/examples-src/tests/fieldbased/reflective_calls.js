function f(g) {
  g();
}

function h() {
  f.call(null, k);
}

function k() {}
