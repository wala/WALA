function foo(x, y) {
  return x + y;
}

function bar(n, bx, by) {
  var s = bx;
  for (var i=0; i<n; i++) {
    s = foo(s, by);
  }
  return s;
}

a = "strA";
b = "strB";
c = bar(10, a, b);
