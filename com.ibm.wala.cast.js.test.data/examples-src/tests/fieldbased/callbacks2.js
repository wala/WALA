function f(x, y) {
  return x;
}

function g(h) {
  h();
}

g(f(function k(){}, function l(){}));