function f() {
  return this;
}

function g(h) {
  h();
}

g(new f(function k(){}));