
var sleep = require('sleep');
var express = require('express');
var app = express();
app.use (function(req, res, next) {
    var data='';
    req.setEncoding('utf8');
    req.on('data', function(chunk) {
       data += chunk;
    });

    req.on('end', function() {
        req.body = data;
        next();
    });
});
app.disable('etag');

app.get('/*', function(req, res){
  res.set('content-type', 'application/json');
  res.send(200,'{"result":"hello world"}');
});

app.put('/*', function(req,res){
  res.set('content-type', 'application/json');
  res.send(201,'{"result":"success"}');
});

app.post('/*', function(req, res){
  res.set('content-type', 'application/json');
  res.set('x-pp-user', 'user1');
  res.send(201, '{"result":"success"}');
});
app.listen(8000);