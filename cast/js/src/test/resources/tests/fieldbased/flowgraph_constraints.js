// IIFE, named function
(function f1() {})();

// IIFE, anon function
(function () {})();

// parameters and returns
function id(p) {
  return p;
}

function callId() {
  var x = {};
  var y = id(x);
}

function callIdHigherOrder() {
  var fp = id;
  var x = {};
  var y = fp(x);
}

function voidFun() {
  return;
}

voidFun();

function functionPrototypeCallApply() {
  function nested() {}
  var x = nested;
  var res1 = x.call(null, nested);
  var res2 = x.apply(null, [nested]);
}
