/*
 * MIT License
 *
 * Copyright (c) 2024 avinzhang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 *  all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package io.github.qwzhang01.shield.advice;

import io.github.qwzhang01.shield.MaskAlgoContainer;
import io.github.qwzhang01.shield.annotation.Mask;
import io.github.qwzhang01.shield.context.MaskContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;

/**
 * Spring MVC ResponseBodyAdvice implementation for automatic data masking.
 *
 * <p>This advice intercepts HTTP responses before they are written to the
 * client and applies data masking to fields annotated with {@link Mask}
 * or its meta-annotations. The masking is controlled by the MaskContext
 * which determines whether masking should be applied for the current request
 * .</p>
 *
 * <p><strong>How It Works:</strong></p>
 * <ol>
 *   <li>The {@link #supports} method determines if the response should be
 *   processed</li>
 *   <li>Skips file downloads, reactive types (Flux, Mono, SseEmitter)</li>
 *   <li>Only processes responses when MaskContext is enabled</li>
 *   <li>The {@link #beforeBodyWrite} method applies masking before
 *   serialization</li>
 * </ol>
 *
 * <p><strong>Usage Notes:</strong></p>
 * <ul>
 *   <li>Automatically enabled when the library is on classpath</li>
 *   <li>No manual configuration required - works out of the box</li>
 *   <li>Requires MaskContext.start() to be called (usually in an interceptor
 *   or filter)</li>
 *   <li>Automatically cleans up MaskContext after processing</li>
 * </ul>
 *
 * @author avinzhang
 * @see MaskContext
 * @see Mask
 * @since 1.0.0
 */
@ControllerAdvice
public class MaskAdvice implements ResponseBodyAdvice<Object> {
    private final MaskAlgoContainer maskAlgoContainer;

    public MaskAdvice(MaskAlgoContainer maskAlgoContainer) {
        this.maskAlgoContainer = maskAlgoContainer;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<?
            extends HttpMessageConverter<?>> converterType) {
        // 文件下载
        boolean fileDownload =
                returnType.getParameterType().equals(InputStreamResource.class) ||
                        returnType.getParameterType().equals(Resource.class) ||
                        returnType.getParameterType().equals(File.class);
        if (fileDownload) {
            return false;
        }

        // flux 响应式结果不包装
        boolean isFlux =
                Flux.class.isAssignableFrom(returnType.getParameterType())
                        || SseEmitter.class.isAssignableFrom(returnType.getParameterType())
                        || Mono.class.isAssignableFrom(returnType.getParameterType());

        if (isFlux) {
            return false;
        }
        return MaskContext.isMask();
    }

    @Nullable
    @Override
    public Object beforeBodyWrite(@Nullable Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body == null) {
            return body;
        }

        Object mask = maskAlgoContainer.mask(body);
        MaskContext.stop();
        return mask;
    }
}