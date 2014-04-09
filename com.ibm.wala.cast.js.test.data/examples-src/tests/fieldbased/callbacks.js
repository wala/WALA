function f(g, h) {
  g(h)();
}

function k(m) {
  return m();
}

function l() {
  return function n() {};
}

f(k, l);
k(function p(){});