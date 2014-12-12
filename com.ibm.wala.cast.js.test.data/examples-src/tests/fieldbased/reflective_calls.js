function f(g) {
  g();
}

function x() {
	
}

function h() {
  f.call(null, k);
  x.apply(null, []);
}

function k() {}

h();
