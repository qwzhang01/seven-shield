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


package io.github.qwzhang01.shield.annotation;


import io.github.qwzhang01.shield.shield.IdCardCoverAlgo;

import java.lang.annotation.*;

/**
 * Annotation for marking ID card/national identification number fields that
 * require masking.
 *
 * <p>This annotation specifically targets ID card fields and applies ID card
 * masking algorithm. The masking typically keeps the first 6 and last 4
 * digits while masking the middle portion to protect sensitive identity
 * information.</p>
 *
 * <p>This is a meta-annotation example demonstrating how to create custom
 * masking annotations. The annotation itself is marked with {@link Mask}
 * to specify the masking algorithm.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * public class User {
 *     {@code @MaskId}
 *     private String idCard; // Will be masked as 110101********1234
 * }
 * </pre>
 *
 * @author avinzhang
 * @see Mask
 * @see IdCardCoverAlgo
 * @since 1.0.0
 */
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Mask(value = IdCardCoverAlgo.class)
public @interface MaskId {
}
