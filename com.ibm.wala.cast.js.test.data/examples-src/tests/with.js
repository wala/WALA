function im_with_stupid(stupid, x) {
  with (stupid) {
    return x+3;
  }
}

var foo = new Object();
var bar = im_with_stupid(foo, 7);
