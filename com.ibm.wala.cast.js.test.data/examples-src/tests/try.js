
function targetOne( x ) {
  return x;
}

function targetTwo( x ) {
  throw x;
}

function tryCatch( x, targetOne, targetTwo ) {
  try {
    if (x.one < 7)
      targetOne( x );
    else
      targetTwo( x );
  } catch (e) {
    e.two();
  }
}

function tryFinally( x, targetOne, targetTwo ) {
  try {
    if (x.one < 7)
      return targetOne( x );
    else
      targetTwo( x );
  } finally {
    x.two();
  }
  x.three();
}

function tryFinallyLoop( x, targetTwo ) {
  while (x.one < 7) {
    try {
      if (x.one < 3)
        break;
      else
        targetTwo( x );
    } finally {
      x.two();
    }
  }
}

function tryCatchFinally( x, targetOne, targetTwo ) {
  try {
    if (x.one < 7)
      targetOne( x );
    else
      targetTwo( x );
  } catch (e) {
    e.two();
  } finally {
    x.three();
  }
}

function tryCatchTwice( x, targetOne, targetTwo ) {
  
  try {
    if (x.one < 7)
      targetOne( x );
    else
      targetTwo( x );
  } catch (e) {
    e.two();
  }

  try {
    if (x.one < 7)
      targetOne( x );
    else
      targetTwo( x );
  } catch (e) {
    e.three();
  }

  return e;
}

o = {
 one: -12,

 two: function two () {
   return this;
 },

 three: function three () {
   return 8;
 }
};

tryCatch(o, targetOne, targetTwo);
tryFinally(o, targetOne, targetTwo);
tryFinallyLoop(o, targetTwo);
tryCatchFinally(o, targetOne, targetTwo);
(function testRet() {
  var e = tryCatchTwice(o, targetOne, targetTwo);
  e.two();
  e.three();
})();



