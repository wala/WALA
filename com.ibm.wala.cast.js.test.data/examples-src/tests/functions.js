
function outer(outerArg) {
  var local = 3;

  function inner1(inner1Arg) {
    outerArg = inner1Arg + 1;
  };

  var fun = function(inner2Arg) { return inner2Arg + outerArg; };

  inner1( fun(6) );
}



  