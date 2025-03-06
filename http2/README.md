Just set the property `server.http2.enabled` to true
h2c is used in non SSL requests

curl command

`curl --http2 -sI http://localhost:8080/hello`