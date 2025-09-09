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

function m() {
  return function n() {}
}

function p() {
  var x = m.apply(null, []);
  x();
}
