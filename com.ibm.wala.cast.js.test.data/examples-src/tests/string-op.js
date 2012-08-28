
function getOp(x, op) {
  return x[ "operator" + op ];
}

function plusNum(y) {
  return this.val + y;
}

var obj = { val: 7, operatorPlus: plusNum };

var result =  ( getOp(obj, "Plus") )( 6 );

