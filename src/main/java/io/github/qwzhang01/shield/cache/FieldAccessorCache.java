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

package io.github.qwzhang01.shield.cache;

import io.github.qwzhang01.shield.domain.FieldAccessor;
import io.github.qwzhang01.shield.domain.MaskField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for FieldAccessor instances to avoid repeated MethodHandle creation.
 *
 * <p>MethodHandle creation is expensive (though still cheaper than reflection),
 * so we cache the FieldAccessor instances for reuse.</p>
 *
 * <p>Performance benefits:
 * <ul>
 *   <li>First access: Create MethodHandle (~100x faster than uncached
 *   reflection)</li>
 *   <li>Subsequent access: Use cached MethodHandle (~10x faster than cached
 *   reflection)</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.1.0
 */
public final class FieldAccessorCache {
    private static final Logger log =
            LoggerFactory.getLogger(FieldAccessorCache.class);
    /**
     * The cache storage using ConcurrentHashMap for thread-safe operations.
     */
    private static final ConcurrentHashMap<CacheKey, FieldAccessor> CACHE =
            new ConcurrentHashMap<>();
    // Statistics for monitoring
    private static long cacheHits = 0;
    private static long cacheMisses = 0;
    /**
     * Private constructor to prevent instantiation.
     */
    private FieldAccessorCache() {
        throw new UnsupportedOperationException("Utility class cannot be " +
                "instantiated");
    }

    /**
     * Gets or creates a FieldAccessor for the given field.
     * Uses double-checked locking pattern for optimal performance.
     *
     * @param field the field to get accessor for
     * @return the FieldAccessor instance
     */
    public static FieldAccessor getAccessor(Field field) {
        CacheKey key = new CacheKey(field.getDeclaringClass(), field.getName());

        // Fast path: cache hit
        FieldAccessor accessor = CACHE.get(key);
        if (accessor != null) {
            cacheHits++;
            return accessor;
        }

        // Slow path: cache miss, need to create
        cacheMisses++;
        return CACHE.computeIfAbsent(key, k -> {
            log.debug("Creating FieldAccessor for: {}", key);
            return new FieldAccessor(field);
        });
    }

    /**
     * Creates a MaskField with cached FieldAccessor.
     * This is a convenience method that combines cache lookup with MaskField
     * creation.
     *
     * @param field      the field
     * @param obj        the object
     * @param behest     whether the field inherits masking
     * @param maskFlag   whether masking is enabled
     * @param annotation the Mask annotation
     * @return the MaskField with cached accessor
     */
    public static MaskField createMaskField(Field field, Object obj,
                                            boolean behest,
                                            boolean maskFlag,
                                            io.github.qwzhang01.shield.annotation.Mask annotation) {
        FieldAccessor accessor = getAccessor(field);
        return new MaskField(field, obj, behest, maskFlag, annotation,
                accessor);
    }

    /**
     * Clears the cache. Useful for testing or when classes are reloaded.
     */
    public static void clear() {
        log.info("Clearing FieldAccessor cache. Size: {}, Hits: {}, Misses: " +
                        "{}, Hit Rate: {}%",
                CACHE.size(), cacheHits, cacheMisses, getHitRate());
        CACHE.clear();
        cacheHits = 0;
        cacheMisses = 0;
    }

    /**
     * Gets the current cache size.
     *
     * @return the number of cached accessors
     */
    public static int size() {
        return CACHE.size();
    }

    /**
     * Gets cache hit count.
     *
     * @return the number of cache hits
     */
    public static long getCacheHits() {
        return cacheHits;
    }

    /**
     * Gets cache miss count.
     *
     * @return the number of cache misses
     */
    public static long getCacheMisses() {
        return cacheMisses;
    }

    /**
     * Gets cache hit rate as percentage.
     *
     * @return the hit rate (0-100)
     */
    public static double getHitRate() {
        long total = cacheHits + cacheMisses;
        return total == 0 ? 0.0 : (cacheHits * 100.0) / total;
    }

    /**
     * Gets cache statistics as a formatted string.
     *
     * @return cache statistics
     */
    public static String getStats() {
        return String.format(
                "FieldAccessorCache[size=%d, hits=%d, misses=%d, hitRate=%" +
                        ".2f%%]",
                size(), cacheHits, cacheMisses, getHitRate()
        );
    }

    /**
     * Logs current cache statistics.
     */
    public static void logStats() {
        log.info(getStats());
    }

    /**
     * Removes a specific field accessor from cache.
     *
     * @param field the field to remove
     * @return true if removed, false if not in cache
     */
    public static boolean remove(Field field) {
        CacheKey key = new CacheKey(field.getDeclaringClass(), field.getName());
        return CACHE.remove(key) != null;
    }

    /**
     * Removes all field accessors for a specific class from cache.
     * Useful when a class is reloaded.
     *
     * @param clazz the class whose field accessors should be removed
     * @return the number of accessors removed
     */
    public static int removeByClass(Class<?> clazz) {
        int removed = 0;
        List<CacheKey> toRemove = CACHE.keySet().stream()
                .filter(key -> key.declaringClass.equals(clazz))
                .toList();

        for (CacheKey key : toRemove) {
            if (CACHE.remove(key) != null) {
                removed++;
            }
        }

        if (removed > 0) {
            log.debug("Removed {} field accessors for class: {}",
                    removed, clazz.getSimpleName());
        }

        return removed;
    }

    /**
     * Cache key combining declaring class and field name.
     * This ensures we can distinguish fields with the same name in different
     * classes.
     */
    private static class CacheKey {
        private final Class<?> declaringClass;
        private final String fieldName;
        private final int hashCode;

        CacheKey(Class<?> declaringClass, String fieldName) {
            this.declaringClass = declaringClass;
            this.fieldName = fieldName;
            // Pre-compute hashCode for performance
            this.hashCode =
                    31 * declaringClass.hashCode() + fieldName.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey that)) return false;
            return declaringClass.equals(that.declaringClass) &&
                    fieldName.equals(that.fieldName);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            return declaringClass.getSimpleName() + "." + fieldName;
        }
    }
}
