function a() {
  return 0;
}

var b = function bf(x) {
  return x + 1;
}

function c(x) {
  return x - 1;
}

var ma = a, mb = b, mc = c;

ma();

mb(7);

mc(8);
  