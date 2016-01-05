# Intro
This is a http streaming server within a single jar file. This is forked and optimized from [Netty Example Code](https://github.com/netty/netty/blob/netty-4.0.33.Final/example/src/main/java/io/netty/example/http/file/HttpStaticFileServer.java).

# Key Feature
- The server supports and runs with non blocking i/o and zero copy.
- The server is written in Java and wrapped into a single jar, as a result it is very simple and compatible with any environment.
- It's lightweight. The jar file size is only 1.8MB!
- This server supports partial request.

# Where to use
- This is optimized for media file streaming such as mp3 or mp4.

# Requirement
* JDK 1.7+


# Usage
First and foremost, please checkout the source code.
```Shellscript
$ git clone https://github.com/lks21c/netty-http-streaming-server
$ cd netty-http-streaming-server/dist
# copy media files into dist
$ java -jar netty-server.jar
```
> After launching the jar file, streaming server is run on 8283 port.
>
> Then you can test media streaming is working via VLC player with url
http://localhost:8283/sample.mp3.

You'll see log message below after playing the mp3 file via VLC.

```
Range = bytes=0-
Content-Range : bytes 0-1430173/1430174
Content-Length : 1430174
[id: 0xe80e543f, /127.0.0.1:33879 => /127.0.0.1:8283] Transfer complete.
Jan 05, 2016 5:07:24 PM io.netty.handler.logging.LoggingHandler logMessage
INFO: [id: 0x7008e8ab, /0:0:0:0:0:0:0:0:8283] RECEIVED: [id: 0xdee6bacb, /127.0.0.1:33881 => /127.0.0.1:8283]
Range = bytes=331800-
Content-Range : bytes 331800-1430173/1430174
Content-Length : 1098374
[id: 0xdee6bacb, /127.0.0.1:33881 => /127.0.0.1:8283] Transfer complete.
Jan 05, 2016 5:07:26 PM io.netty.handler.logging.LoggingHandler logMessage
INFO: [id: 0x7008e8ab, /0:0:0:0:0:0:0:0:8283] RECEIVED: [id: 0xbb60d099, /127.0.0.1:33882 => /127.0.0.1:8283]
Range = bytes=670751-
Content-Range : bytes 670751-1430173/1430174
Content-Length : 759423
[id: 0xbb60d099, /127.0.0.1:33882 => /127.0.0.1:8283] Transfer complete.
```

# Why I changed code from netty example.
The purpose of code change is that I removed "request chunked encoding" feature since it's
unnecessary for media streaming server.(the server focuses on download, not upload)

And I also added code for Partial Request.