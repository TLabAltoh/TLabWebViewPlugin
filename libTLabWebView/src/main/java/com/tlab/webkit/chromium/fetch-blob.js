function writeBuffer(buffer, bufferId, segmentSize, offset)
{
    if (segmentSize === 0) return;
    var i = offset;
    while(i + segmentSize <= buffer.length)
    {
       window.tlab._write(bufferId, buffer.slice(i, i + segmentSize));
       i += segmentSize
    }
    writeBuffer(buffer, bufferId, parseInt(segmentSize / 2), i);
}
var xhr = new XMLHttpRequest();
xhr.open("GET", url, true);
xhr.setRequestHeader("Content-type", mimetype + ";charset=UTF-8");
xhr.responseType = "blob";
xhr.onload = function(e) {
    if (this.status == 200) {
        var blobFile = this.response;
        var reader = new FileReader();
        reader.readAsDataURL(blobFile);
        reader.onloadend = function() {
            base64data = reader.result;
            bufferId = url;
            buffer = new TextEncoder().encode(base64data);
            window.tlab._malloc(bufferId, buffer.length);
            writeBuffer(buffer, bufferId, 500000, 0);
            window.tlab.fetchBlob(url, contentDisposition, mimetype);
        }
    }
};
xhr.send();