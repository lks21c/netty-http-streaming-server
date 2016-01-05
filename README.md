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


# Command to run
First and foremost, please checkout the source code.
```Shellscript
$ git clone https://github.com/lks21c/netty-http-streaming-server
$ cd netty-http-streaming-server/dist
# copy media files into dist
$ java -jar netty-server.jar
```
> After launching the jar file, streaming server is run on 8283 port.
>
> Then you can test streaming is working via VLC player with url
http://localhost:8283/sample.mp3.

# Why I changed code from netty example.
The purpose of code change is that I removed request chunked encoding feature since it's
unnecessary for media streaming server.(the server focuses on download, not upload)

And I also added code for Partial Request.