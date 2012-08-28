
function Polygon() {
  this.edges = 8;
  this.regular = false;
  this.shape = function shape() { return "rectangle"; };
  this.area = function area() { return -1; };
}

function objectMasquerading () {

  function Rectangle(top_len,side_len) {
    this.temp = Polygon;
    this.temp();
    this.temp = null;
    this.edges = 4;
    this.top = top_len;
    this.side = side_len;
    this.area = function area() { return this.top*this.sides; };
  }

  return new Rectangle(3, 5);
}

function sharedClassObject() {

  function Rectangle(top_len, side_len) {
    this.edges = 4;
    this.top = top_len;
    this.side = side_len;
    this.area = function area() { return this.top*this.sides; };
  }

  Rectangle.prototype = new Polygon();

  return new Rectangle(3, 7);
}

var rec1 = objectMasquerading();
rec1.area();
rec1.shape();

var rec2 = sharedClassObject();
rec2.area();
rec2.shape();

