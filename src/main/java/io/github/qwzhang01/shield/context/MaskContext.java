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


package io.github.qwzhang01.shield.context;

import java.util.HashSet;
import java.util.Set;

/**
 * Thread-local context for managing data masking state.
 *
 * <p>This class provides a ThreadLocal-based context to control whether data
 * masking should be applied in the current thread. It's particularly useful
 * for enabling/disabling masking dynamically based on request context or
 * user permissions.</p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>
 * // Enable masking for current thread
 * MaskContext.start();
 * try {
 *     // Your business logic here - masking will be applied
 *     userService.getUserInfo();
 * } finally {
 *     // Clean up to prevent memory leaks
 *     MaskContext.stop();
 * }
 * </pre>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class MaskContext {
    /**
     * ThreadLocal storage for masking context to ensure thread safety
     */
    private static final ThreadLocal<MaskContext> MASK_CONTEXT =
            new ThreadLocal<>();

    /**
     * Flag indicating whether masking is enabled in current thread
     */
    private boolean enabled = false;

    /**
     * Set of field names to be included in masking operations
     */
    private final Set<String> includeFields = new HashSet<>();

    /**
     * Starts the masking context for the current thread.
     * Must be called before any masking operations.
     */
    public static void start() {
        MaskContext context = MASK_CONTEXT.get();
        if (context == null) {
            context = new MaskContext();
        }
        context.enabled = true;
        MASK_CONTEXT.set(context);
    }

    /**
     * Stops the masking context and cleans up ThreadLocal storage.
     * Should always be called in a finally block to prevent memory leaks.
     */
    public static void stop() {
        MASK_CONTEXT.remove();
    }

    /**
     * Checks if masking is currently enabled for the current thread.
     *
     * @return true if masking is enabled, false otherwise
     */
    public static boolean isMask() {
        return MASK_CONTEXT.get() != null && MASK_CONTEXT.get().enabled;
    }

    /**
     * Gets the current masking context for the current thread.
     *
     * @return the current MaskContext instance, or null if not initialized
     */
    public static MaskContext current() {
        return MASK_CONTEXT.get();
    }

    /**
     * Adds a field name to the include list for selective masking.
     *
     * @param field the field name to include in masking operations
     */
    public static void addIncludeField(String field) {
        MaskContext context = MASK_CONTEXT.get();
        if (context == null) {
            context = new MaskContext();
        }
        context.includeFields.add(field);
        MASK_CONTEXT.set(context);
    }

    /**
     * Gets the set of field names to be included in masking.
     *
     * @return an immutable set of field names, empty set if context is not
     * initialized
     */
    public static Set<String> getIncludeFields() {
        MaskContext context = MASK_CONTEXT.get();
        if (context == null) {
            return new HashSet<>();
        }
        return context.includeFields;
    }
}
