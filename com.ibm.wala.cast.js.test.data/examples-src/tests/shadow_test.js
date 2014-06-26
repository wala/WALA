var f = {
  m: function bad(x) { return x; }
};

g = {
  n: function global_bad(x) { return x; }
};

function test(y) {
    var f = f ? f : { };
    var g = g ? g : { };
    return f.m(y) + " " + g.n(y);
}

test(3);
