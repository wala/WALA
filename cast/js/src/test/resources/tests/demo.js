function Document_prototype_write(x) {

}

function id(x) {
    return x;
}

var document = { URL: "whatever" };
var url = id(document.URL);
Document_prototype_write(url);

var notUrl = id("not a url");
Document_prototype_write(notUrl);
