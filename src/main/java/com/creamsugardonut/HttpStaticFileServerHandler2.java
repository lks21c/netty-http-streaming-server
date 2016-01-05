/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.creamsugardonut;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.SystemPropertyUtil;

/**
 * Http static server via zero copy which reduces overhead of copying kernel buffer to memory.
 */
public class HttpStaticFileServerHandler2 extends SimpleChannelInboundHandler<HttpRequest> {

	@Override
	public void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
		if (!request.getDecoderResult().isSuccess()) {
			sendError(ctx, BAD_REQUEST);
			return;
		}

		final String uri = request.getUri();
		final String path = sanitizeUri(uri);
		if (path == null) {
			sendError(ctx, FORBIDDEN);
			return;
		}

		File file = new File(path);
		if (file.isHidden() || !file.exists()) {
			sendError(ctx, NOT_FOUND);
			return;
		}

		if (!file.isFile()) {
			sendError(ctx, FORBIDDEN);
			return;
		}

		RandomAccessFile raf;
		try {
			raf = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException ignore) {
			sendError(ctx, NOT_FOUND);
			return;
		}
		long fileLength = raf.length();

		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
		setContentTypeHeader(response, file);
		if (HttpHeaders.isKeepAlive(request)) {
			response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}

		// Write the content.
		ChannelFuture sendFileFuture;
		ChannelFuture lastContentFuture;

		// Tell clients that Partial Requests are available.
		response.headers().add(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);

		String rangeHeader = request.headers().get(HttpHeaders.Names.RANGE);
		System.out.println(HttpHeaders.Names.RANGE + " = " + rangeHeader);
		if (rangeHeader != null && rangeHeader.length() > 0) { // Partial Request
			PartialRequestInfo partialRequestInfo = getPartialRequestInfo(rangeHeader, fileLength);

			// Set Response Header
			response.headers().add(HttpHeaders.Names.CONTENT_RANGE, HttpHeaders.Values.BYTES + " "
					+ partialRequestInfo.startOffset + "-" + partialRequestInfo.endOffset + "/" + fileLength);
			System.out.println(
					HttpHeaders.Names.CONTENT_RANGE + " : " + response.headers().get(HttpHeaders.Names.CONTENT_RANGE));

			HttpHeaders.setContentLength(response, partialRequestInfo.getChunkSize());
			System.out.println(HttpHeaders.Names.CONTENT_LENGTH + " : " + partialRequestInfo.getChunkSize());

			response.setStatus(HttpResponseStatus.PARTIAL_CONTENT);

			// Write Response
			ctx.write(response);
			sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), partialRequestInfo.getStartOffset(),
					partialRequestInfo.getChunkSize()), ctx.newProgressivePromise());
		} else {
			// Set Response Header
			HttpHeaders.setContentLength(response, fileLength);

			// Write Response
			ctx.write(response);
			sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength),
					ctx.newProgressivePromise());
		}

		lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

		sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
			@Override
			public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
				if (total < 0) { // total unknown
					System.err.println(future.channel() + " Transfer progress: " + progress);
				} else {
					System.err.println(future.channel() + " Transfer progress: " + progress + " / " + total);
				}
			}

			@Override
			public void operationComplete(ChannelProgressiveFuture future) {
				System.err.println(future.channel() + " Transfer complete.");
			}
		});

		// Decide whether to close the connection or not.
		if (!HttpHeaders.isKeepAlive(request)) {
			// Close the connection when the whole content is written out.
			lastContentFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		if (ctx.channel().isActive()) {
			sendError(ctx, INTERNAL_SERVER_ERROR);
		}
	}

	private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

	private static String sanitizeUri(String uri) {
		// Decode the path.
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new Error(e);
		}

		if (uri.isEmpty() || uri.charAt(0) != '/') {
			return null;
		}

		// Convert file separators.
		uri = uri.replace('/', File.separatorChar);

		// Simplistic dumb security check.
		// You will have to do something serious in the production environment.
		if (uri.contains(File.separator + '.') || uri.contains('.' + File.separator) || uri.charAt(0) == '.'
				|| uri.charAt(uri.length() - 1) == '.' || INSECURE_URI.matcher(uri).matches()) {
			System.out.println("insecure");
			return null;
		}

		// Convert to absolute path.
		return SystemPropertyUtil.get("user.dir") + File.separator + uri;
	}

	private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status,
				Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
		response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

		// Close the connection as soon as the error message is sent.
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

	/**
	 * Sets the content type header for the HTTP Response
	 *
	 * @param response HTTP response
	 * @param file file to extract content type
	 */
	private static void setContentTypeHeader(HttpResponse response, File file) {
		MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
		response.headers().set(CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
	}

	private PartialRequestInfo getPartialRequestInfo(String rangeHeader, long fileLength) {
		PartialRequestInfo partialRequestInfo = new PartialRequestInfo();
		long startOffset = 0;
		long endOffset;
		try {
			startOffset = Integer
					.parseInt(rangeHeader.trim().replace(HttpHeaders.Values.BYTES + "=", "").replace("-", ""));
		} catch (NumberFormatException e) {
		}

		if (startOffset == 0) {
			endOffset = startOffset + (fileLength / 1);
		} else {
			endOffset = startOffset + (fileLength / 1);
		}

		if (endOffset >= fileLength) {
			endOffset = fileLength - 1;
		}
		long chunkSize = endOffset - startOffset + 1;

		partialRequestInfo.setStartOffset(startOffset);
		partialRequestInfo.setEndOffset(endOffset);
		partialRequestInfo.setChunkSize(chunkSize);
		return partialRequestInfo;
	}

	class PartialRequestInfo {
		private long startOffset;
		private long endOffset;
		private long chunkSize;

		public long getStartOffset() {
			return startOffset;
		}

		public void setStartOffset(long startOffset) {
			this.startOffset = startOffset;
		}

		public long getEndOffset() {
			return endOffset;
		}

		public void setEndOffset(long endOffset) {
			this.endOffset = endOffset;
		}

		public long getChunkSize() {
			return chunkSize;
		}

		public void setChunkSize(long chunkSize) {
			this.chunkSize = chunkSize;
		}
	}
}
