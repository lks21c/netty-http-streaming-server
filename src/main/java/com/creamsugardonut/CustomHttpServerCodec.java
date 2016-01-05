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

import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.http.HttpResponseEncoder;

/**
 * Forked from HttpServerCodec. This disables chunked-encoding support since it isn't necessary for Http Streaming
 * Server.
 *
 */
public final class CustomHttpServerCodec
		extends CombinedChannelDuplexHandler<CustomHttpRequestDecoder, HttpResponseEncoder> {

	/**
	 * Creates a new instance with the default decoder options ({@code maxInitialLineLength (4096}},
	 * {@code maxHeaderSize (8192)}, and {@code maxChunkSize (8192)}).
	 */
	public CustomHttpServerCodec() {
		this(4096, 8192, 8192);
	}

	/**
	 * Creates a new instance with the specified decoder options.
	 */
	public CustomHttpServerCodec(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize) {
		super(new CustomHttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize),
				new HttpResponseEncoder());
	}

	/**
	 * Creates a new instance with the specified decoder options.
	 */
	public CustomHttpServerCodec(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize,
			boolean validateHeaders) {
		super(new CustomHttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize, validateHeaders),
				new HttpResponseEncoder());
	}
}
