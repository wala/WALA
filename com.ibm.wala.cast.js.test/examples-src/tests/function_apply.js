function bar(y) { return y + 3; }

var p = bar;
p.apply(null, [7]);

