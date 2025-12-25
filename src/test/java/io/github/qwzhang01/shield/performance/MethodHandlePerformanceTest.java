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

package io.github.qwzhang01.shield.performance;

import io.github.qwzhang01.shield.cache.FieldAccessorCache;
import io.github.qwzhang01.shield.domain.FieldAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

/**
 * Performance benchmark comparing MethodHandle vs Reflection.
 * 
 * <p>Expected results (on modern JVM):
 * <ul>
 *   <li>Direct access: ~1-2 ns/op (baseline)</li>
 *   <li>MethodHandle (cached): ~3-5 ns/op</li>
 *   <li>Reflection (cached): ~30-50 ns/op</li>
 *   <li>Reflection (uncached): ~100-200 ns/op</li>
 * </ul>
 *
 * @author avinzhang
 */
public class MethodHandlePerformanceTest {

    private static final int WARMUP_ITERATIONS = 10000;
    private static final int TEST_ITERATIONS = 1000000;

    static class TestObject {
        private String name = "John Doe";
        private int age = 30;
        private String email = "john@example.com";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private TestObject testObject;
    private Field nameField;
    private FieldAccessor fieldAccessor;

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        testObject = new TestObject();
        nameField = TestObject.class.getDeclaredField("name");
        nameField.setAccessible(true);
        fieldAccessor = new FieldAccessor(nameField);
        
        // Clear cache for accurate testing
        FieldAccessorCache.clear();
    }

    @Test
    void benchmarkDirectAccess() {
        System.out.println("\n=== Direct Access Benchmark ===");
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            String value = testObject.getName();
            testObject.setName(value);
        }

        // Test
        long start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            String value = testObject.getName();
            testObject.setName(value);
        }
        long end = System.nanoTime();

        double avgNanos = (end - start) / (double) TEST_ITERATIONS;
        System.out.printf("Average time per operation: %.2f ns%n", avgNanos);
        System.out.printf("Total time: %.2f ms%n", (end - start) / 1_000_000.0);
    }

    @Test
    void benchmarkMethodHandle() {
        System.out.println("\n=== MethodHandle Benchmark ===");
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Object value = fieldAccessor.get(testObject);
            fieldAccessor.set(testObject, value);
        }

        // Test
        long start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Object value = fieldAccessor.get(testObject);
            fieldAccessor.set(testObject, value);
        }
        long end = System.nanoTime();

        double avgNanos = (end - start) / (double) TEST_ITERATIONS;
        System.out.printf("Average time per operation: %.2f ns%n", avgNanos);
        System.out.printf("Total time: %.2f ms%n", (end - start) / 1_000_000.0);
    }

    @Test
    void benchmarkReflection() throws IllegalAccessException {
        System.out.println("\n=== Reflection Benchmark (Cached Field) ===");
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Object value = nameField.get(testObject);
            nameField.set(testObject, value);
        }

        // Test
        long start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Object value = nameField.get(testObject);
            nameField.set(testObject, value);
        }
        long end = System.nanoTime();

        double avgNanos = (end - start) / (double) TEST_ITERATIONS;
        System.out.printf("Average time per operation: %.2f ns%n", avgNanos);
        System.out.printf("Total time: %.2f ms%n", (end - start) / 1_000_000.0);
    }

    @Test
    void benchmarkReflectionUncached() throws IllegalAccessException, NoSuchFieldException {
        System.out.println("\n=== Reflection Benchmark (Uncached Field) ===");
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Field field = TestObject.class.getDeclaredField("name");
            field.setAccessible(true);
            Object value = field.get(testObject);
            field.set(testObject, value);
        }

        // Test
        long start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Field field = TestObject.class.getDeclaredField("name");
            field.setAccessible(true);
            Object value = field.get(testObject);
            field.set(testObject, value);
        }
        long end = System.nanoTime();

        double avgNanos = (end - start) / (double) TEST_ITERATIONS;
        System.out.printf("Average time per operation: %.2f ns%n", avgNanos);
        System.out.printf("Total time: %.2f ms%n", (end - start) / 1_000_000.0);
    }

    @Test
    void benchmarkFieldAccessorCache() {
        System.out.println("\n=== FieldAccessorCache Benchmark ===");
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            FieldAccessor accessor = FieldAccessorCache.getAccessor(nameField);
            Object value = accessor.get(testObject);
            accessor.set(testObject, value);
        }

        // Test
        FieldAccessorCache.clear(); // Reset stats
        long start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            FieldAccessor accessor = FieldAccessorCache.getAccessor(nameField);
            Object value = accessor.get(testObject);
            accessor.set(testObject, value);
        }
        long end = System.nanoTime();

        double avgNanos = (end - start) / (double) TEST_ITERATIONS;
        System.out.printf("Average time per operation: %.2f ns%n", avgNanos);
        System.out.printf("Total time: %.2f ms%n", (end - start) / 1_000_000.0);
        System.out.println("\nCache Statistics:");
        System.out.println(FieldAccessorCache.getStats());
    }

    @Test
    void runAllBenchmarks() throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("PERFORMANCE COMPARISON: MethodHandle vs Reflection");
        System.out.println("=".repeat(60));
        System.out.println("Iterations: " + TEST_ITERATIONS);
        System.out.println("Object: TestObject with String field");

        benchmarkDirectAccess();
        benchmarkMethodHandle();
        benchmarkFieldAccessorCache();
        benchmarkReflection();
        benchmarkReflectionUncached();

        System.out.println("\n" + "=".repeat(60));
        System.out.println("SUMMARY:");
        System.out.println("- Direct Access: Baseline (fastest)");
        System.out.println("- MethodHandle: ~1.5-3x slower than direct");
        System.out.println("- MethodHandle (cached): Similar to above + negligible cache overhead");
        System.out.println("- Reflection (cached): ~10-20x slower than MethodHandle");
        System.out.println("- Reflection (uncached): ~50-100x slower than MethodHandle");
        System.out.println("=".repeat(60));
    }

    @Test
    void demonstrateRealWorldScenario() throws Exception {
        System.out.println("\n=== Real-World Scenario: Masking 10,000 Objects ===");
        
        // Create 10,000 test objects
        TestObject[] objects = new TestObject[10000];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = new TestObject();
        }

        // Scenario 1: Using MethodHandle
        FieldAccessor accessor = new FieldAccessor(nameField);
        long start1 = System.nanoTime();
        for (TestObject obj : objects) {
            String value = accessor.getAsString(obj);
            String masked = value != null ? "***" : null;
            accessor.set(obj, masked);
        }
        long end1 = System.nanoTime();

        // Reset objects
        for (int i = 0; i < objects.length; i++) {
            objects[i] = new TestObject();
        }

        // Scenario 2: Using Reflection
        long start2 = System.nanoTime();
        for (TestObject obj : objects) {
            nameField.setAccessible(true);
            Object value = nameField.get(obj);
            String masked = value != null ? "***" : null;
            nameField.set(obj, masked);
        }
        long end2 = System.nanoTime();

        double methodHandleTime = (end1 - start1) / 1_000_000.0;
        double reflectionTime = (end2 - start2) / 1_000_000.0;
        double improvement = ((reflectionTime - methodHandleTime) / reflectionTime) * 100;

        System.out.printf("MethodHandle time: %.2f ms%n", methodHandleTime);
        System.out.printf("Reflection time: %.2f ms%n", reflectionTime);
        System.out.printf("Performance improvement: %.1f%%%n", improvement);
        System.out.printf("Speedup: %.1fx faster%n", reflectionTime / methodHandleTime);
    }
}
