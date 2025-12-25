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

package io.github.qwzhang01.shield.domain;

import io.github.qwzhang01.shield.cache.FieldAccessorCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for primitive type and wrapper type handling in FieldAccessor.
 * 
 * This test ensures that FieldAccessor correctly handles conversions between
 * primitive types (int, long, double, etc.) and their wrapper classes
 * (Integer, Long, Double, etc.).
 *
 * @author avinzhang
 */
public class PrimitiveTypeTest {

    static class TestObject {
        // Primitive types
        private int intValue;
        private long longValue;
        private double doubleValue;
        private float floatValue;
        private boolean booleanValue;
        private byte byteValue;
        private short shortValue;
        private char charValue;

        // Wrapper types
        private Integer integerValue;
        private Long longObjValue;
        private Double doubleObjValue;
        private Float floatObjValue;
        private Boolean booleanObjValue;
        private Byte byteObjValue;
        private Short shortObjValue;
        private Character characterValue;

        private String name;
    }

    private TestObject testObject;

    @BeforeEach
    void setUp() {
        testObject = new TestObject();
        FieldAccessorCache.clear();
    }

    @Test
    void testIntPrimitiveWithIntegerWrapper() throws NoSuchFieldException {
        System.out.println("\n=== Test: int (primitive) with Integer (wrapper) ===");
        
        Field field = TestObject.class.getDeclaredField("intValue");
        FieldAccessor accessor = new FieldAccessor(field);

        // Set Integer value to int field - should work
        Integer wrapperValue = 42;
        accessor.set(testObject, wrapperValue);
        
        Object result = accessor.get(testObject);
        System.out.println("Set Integer(42) to int field, got: " + result + " (type: " + result.getClass().getSimpleName() + ")");
        assertEquals(42, result);

        // Set primitive int value - should also work
        accessor.set(testObject, 100);
        result = accessor.get(testObject);
        System.out.println("Set int(100) to int field, got: " + result + " (type: " + result.getClass().getSimpleName() + ")");
        assertEquals(100, result);
    }

    @Test
    void testIntegerWrapperWithPrimitive() throws NoSuchFieldException {
        System.out.println("\n=== Test: Integer (wrapper) with int (primitive) ===");
        
        Field field = TestObject.class.getDeclaredField("integerValue");
        FieldAccessor accessor = new FieldAccessor(field);

        // Set int value to Integer field - should work
        accessor.set(testObject, 42);
        
        Object result = accessor.get(testObject);
        System.out.println("Set int(42) to Integer field, got: " + result);
        assertEquals(42, result);

        // Set Integer value - should also work
        accessor.set(testObject, Integer.valueOf(100));
        result = accessor.get(testObject);
        System.out.println("Set Integer(100) to Integer field, got: " + result);
        assertEquals(100, result);
    }

    @Test
    void testLongPrimitiveWithWrapper() throws NoSuchFieldException {
        System.out.println("\n=== Test: long (primitive) with Long (wrapper) ===");
        
        Field field = TestObject.class.getDeclaredField("longValue");
        FieldAccessor accessor = new FieldAccessor(field);

        Long wrapperValue = 123456789L;
        accessor.set(testObject, wrapperValue);
        
        Object result = accessor.get(testObject);
        System.out.println("Set Long to long field: " + result);
        assertEquals(123456789L, result);
    }

    @Test
    void testDoublePrimitiveWithWrapper() throws NoSuchFieldException {
        System.out.println("\n=== Test: double (primitive) with Double (wrapper) ===");
        
        Field field = TestObject.class.getDeclaredField("doubleValue");
        FieldAccessor accessor = new FieldAccessor(field);

        Double wrapperValue = 3.14159;
        accessor.set(testObject, wrapperValue);
        
        Object result = accessor.get(testObject);
        System.out.println("Set Double to double field: " + result);
        assertEquals(3.14159, result);
    }

    @Test
    void testFloatPrimitiveWithWrapper() throws NoSuchFieldException {
        System.out.println("\n=== Test: float (primitive) with Float (wrapper) ===");
        
        Field field = TestObject.class.getDeclaredField("floatValue");
        FieldAccessor accessor = new FieldAccessor(field);

        Float wrapperValue = 2.718f;
        accessor.set(testObject, wrapperValue);
        
        Object result = accessor.get(testObject);
        System.out.println("Set Float to float field: " + result);
        assertEquals(2.718f, result);
    }

    @Test
    void testBooleanPrimitiveWithWrapper() throws NoSuchFieldException {
        System.out.println("\n=== Test: boolean (primitive) with Boolean (wrapper) ===");
        
        Field field = TestObject.class.getDeclaredField("booleanValue");
        FieldAccessor accessor = new FieldAccessor(field);

        Boolean wrapperValue = Boolean.TRUE;
        accessor.set(testObject, wrapperValue);
        
        Object result = accessor.get(testObject);
        System.out.println("Set Boolean to boolean field: " + result);
        assertEquals(true, result);
    }

    @Test
    void testBytePrimitiveWithWrapper() throws NoSuchFieldException {
        System.out.println("\n=== Test: byte (primitive) with Byte (wrapper) ===");
        
        Field field = TestObject.class.getDeclaredField("byteValue");
        FieldAccessor accessor = new FieldAccessor(field);

        Byte wrapperValue = (byte) 127;
        accessor.set(testObject, wrapperValue);
        
        Object result = accessor.get(testObject);
        System.out.println("Set Byte to byte field: " + result);
        assertEquals((byte) 127, result);
    }

    @Test
    void testShortPrimitiveWithWrapper() throws NoSuchFieldException {
        System.out.println("\n=== Test: short (primitive) with Short (wrapper) ===");
        
        Field field = TestObject.class.getDeclaredField("shortValue");
        FieldAccessor accessor = new FieldAccessor(field);

        Short wrapperValue = (short) 32767;
        accessor.set(testObject, wrapperValue);
        
        Object result = accessor.get(testObject);
        System.out.println("Set Short to short field: " + result);
        assertEquals((short) 32767, result);
    }

    @Test
    void testCharPrimitiveWithWrapper() throws NoSuchFieldException {
        System.out.println("\n=== Test: char (primitive) with Character (wrapper) ===");
        
        Field field = TestObject.class.getDeclaredField("charValue");
        FieldAccessor accessor = new FieldAccessor(field);

        Character wrapperValue = 'A';
        accessor.set(testObject, wrapperValue);
        
        Object result = accessor.get(testObject);
        System.out.println("Set Character to char field: " + result);
        assertEquals('A', result);
    }

    @Test
    void testTypedSetWithPrimitiveAndWrapper() throws NoSuchFieldException {
        System.out.println("\n=== Test: setTyped() with primitive/wrapper conversion ===");
        
        Field field = TestObject.class.getDeclaredField("intValue");
        FieldAccessor accessor = new FieldAccessor(field);

        // Should work: Integer -> int
        accessor.setTyped(testObject, Integer.valueOf(42));
        assertEquals(42, accessor.get(testObject));
        System.out.println("✓ setTyped(Integer(42)) to int field works");

        // Should work: int -> int (direct)
        accessor.setTyped(testObject, 100);
        assertEquals(100, accessor.get(testObject));
        System.out.println("✓ setTyped(int(100)) to int field works");
    }

    @Test
    void testIncompatibleTypeRejected() throws NoSuchFieldException {
        System.out.println("\n=== Test: Incompatible type should be rejected ===");
        
        Field field = TestObject.class.getDeclaredField("intValue");
        FieldAccessor accessor = new FieldAccessor(field);

        // Should fail: String -> int
        assertThrows(Exception.class, () -> {
            accessor.setTyped(testObject, "not a number");
        });
        System.out.println("✓ setTyped(String) to int field correctly rejected");

        // Should fail: Long -> int
        assertThrows(Exception.class, () -> {
            accessor.setTyped(testObject, 42L);
        });
        System.out.println("✓ setTyped(Long) to int field correctly rejected");
    }

    @Test
    void testAllPrimitiveTypes() throws NoSuchFieldException {
        System.out.println("\n=== Test: All primitive types with their wrappers ===");
        
        // Test all primitive types
        testAndPrint("intValue", 42, Integer.class);
        testAndPrint("longValue", 123L, Long.class);
        testAndPrint("doubleValue", 3.14, Double.class);
        testAndPrint("floatValue", 2.718f, Float.class);
        testAndPrint("booleanValue", true, Boolean.class);
        testAndPrint("byteValue", (byte) 127, Byte.class);
        testAndPrint("shortValue", (short) 32767, Short.class);
        testAndPrint("charValue", 'X', Character.class);
        
        System.out.println("\n✓ All primitive types work correctly with their wrappers!");
    }

    @Test
    void testCachedAccessor() throws NoSuchFieldException {
        System.out.println("\n=== Test: Cached FieldAccessor with primitive/wrapper ===");
        
        Field field = TestObject.class.getDeclaredField("intValue");
        
        // Get from cache multiple times
        FieldAccessor accessor1 = FieldAccessorCache.getAccessor(field);
        FieldAccessor accessor2 = FieldAccessorCache.getAccessor(field);
        
        // Should be the same instance
        assertSame(accessor1, accessor2);
        System.out.println("✓ FieldAccessor is properly cached");
        
        // Should work with wrapper
        accessor1.set(testObject, Integer.valueOf(999));
        assertEquals(999, accessor2.get(testObject));
        System.out.println("✓ Cached accessor handles Integer -> int correctly");
        
        System.out.println("\nCache stats: " + FieldAccessorCache.getStats());
    }

    @Test
    void testNullValues() throws NoSuchFieldException {
        System.out.println("\n=== Test: Null values handling ===");
        
        // Wrapper type can be null
        Field wrapperField = TestObject.class.getDeclaredField("integerValue");
        FieldAccessor wrapperAccessor = new FieldAccessor(wrapperField);
        
        wrapperAccessor.set(testObject, null);
        assertNull(wrapperAccessor.get(testObject));
        System.out.println("✓ Null value to Integer field works");
        
        // Note: primitive types cannot be null (will throw exception)
        Field primitiveField = TestObject.class.getDeclaredField("intValue");
        FieldAccessor primitiveAccessor = new FieldAccessor(primitiveField);
        
        assertThrows(Exception.class, () -> {
            primitiveAccessor.set(testObject, null);
        });
        System.out.println("✓ Null value to int field correctly rejected");
    }

    private void testAndPrint(String fieldName, Object value, Class<?> wrapperClass) 
            throws NoSuchFieldException {
        Field field = TestObject.class.getDeclaredField(fieldName);
        FieldAccessor accessor = new FieldAccessor(field);
        
        accessor.set(testObject, value);
        Object result = accessor.get(testObject);
        
        assertEquals(value, result);
        System.out.printf("  ✓ %-15s: set %s(%s), got %s%n", 
                         fieldName, wrapperClass.getSimpleName(), value, result);
    }
}
