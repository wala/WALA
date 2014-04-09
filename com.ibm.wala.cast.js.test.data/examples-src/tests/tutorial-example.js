function outer(s) {
  var x = arguments[0];
  if (s.indexOf('o') > 0) {
    
    function inner(y) {
      var t = ".suffix";
      var arr = [ x + t, y ];
      this.data = arr;
    }
    return new inner(s);
  }
}
var outerProp = outer("outer").data;
