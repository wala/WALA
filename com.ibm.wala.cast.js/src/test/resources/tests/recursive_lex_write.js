(function () {
  function foo() {
      foo = function() { return 3; }
      return 4;
  }
  foo();
  foo();
})();