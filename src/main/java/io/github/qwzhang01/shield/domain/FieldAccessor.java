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

import io.github.qwzhang01.shield.exception.ShieldException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/**
 * High-performance field accessor using MethodHandle instead of reflection.
 * MethodHandle provides near-native performance compared to traditional reflection.
 *
 * <p>Performance comparison:
 * <ul>
 *   <li>Direct access: 1x (baseline)</li>
 *   <li>MethodHandle: ~1.5x</li>
 *   <li>Reflection (cached): ~10x</li>
 *   <li>Reflection (uncached): ~100x</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.1.0
 */
public class FieldAccessor {
    private static final Logger log = LoggerFactory.getLogger(FieldAccessor.class);

    private final Field field;
    private final String fieldName;
    private final Class<?> fieldType;
    private final MethodHandle getter;
    private final MethodHandle setter;

    /**
     * Creates a new FieldAccessor for the given field.
     *
     * @param field the field to access
     * @throws ShieldException if MethodHandle creation fails
     */
    public FieldAccessor(Field field) {
        this.field = field;
        this.fieldName = field.getName();
        this.fieldType = field.getType();

        try {
            // Make field accessible
            field.setAccessible(true);

            // Create MethodHandles for getter and setter
            // unreflectGetter/Setter is faster than findGetter/findSetter
            this.getter = MethodHandles.lookup().unreflectGetter(field);
            this.setter = MethodHandles.lookup().unreflectSetter(field);

            log.debug("Created FieldAccessor for field: {}.{}",
                    field.getDeclaringClass().getSimpleName(), fieldName);
        } catch (IllegalAccessException e) {
            throw new ShieldException(
                    "Failed to create MethodHandle for field: " + field.getName(), e);
        }
    }

    /**
     * Gets the value of this field from the given object.
     * Uses MethodHandle for high performance.
     *
     * @param target the object to get the field value from
     * @return the field value
     * @throws ShieldException if access fails
     */
    public Object get(Object target) {
        if (target == null) {
            return null;
        }

        try {
            return getter.invoke(target);
        } catch (Throwable e) {
            throw new ShieldException(
                    "Failed to get field value: " + fieldName +
                            " from object: " + target.getClass().getSimpleName(), e);
        }
    }

    /**
     * Sets the value of this field on the given object.
     * Uses MethodHandle for high performance.
     * Automatically handles primitive/wrapper type conversions.
     *
     * @param target the object to set the field value on
     * @param value  the value to set
     * @throws ShieldException if access fails
     */
    public void set(Object target, Object value) {
        if (target == null) {
            return;
        }

        try {
            // MethodHandle automatically handles primitive/wrapper conversions
            // No manual conversion needed
            setter.invoke(target, value);
        } catch (Throwable e) {
            throw new ShieldException(
                    "Failed to set field value: " + fieldName +
                            " on object: " + target.getClass().getSimpleName() +
                            " with value: " + value +
                            " (Field type: " + fieldType.getSimpleName() + 
                            ", Value type: " + (value != null ? value.getClass().getSimpleName() : "null") + ")", e);
        }
    }

    /**
     * Gets the value as a specific type (with type safety).
     *
     * @param target the object to get the field value from
     * @param type   the expected type
     * @param <T>    the generic type
     * @return the field value cast to the specified type
     * @throws ShieldException if access fails or type mismatch
     */
    @SuppressWarnings("unchecked")
    public <T> T getTyped(Object target, Class<T> type) {
        Object value = get(target);
        if (value == null) {
            return null;
        }

        if (!type.isInstance(value)) {
            throw new ShieldException(
                    "Field value type mismatch. Expected: " + type.getName() +
                            ", but got: " + value.getClass().getName());
        }

        return (T) value;
    }

    /**
     * Gets the value as String (common use case for masking).
     *
     * @param target the object to get the field value from
     * @return the field value as String, or null if value is null
     */
    public String getAsString(Object target) {
        Object value = get(target);
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * Sets the value with type checking and automatic primitive/wrapper conversion.
     *
     * @param target the object to set the field value on
     * @param value  the value to set
     * @throws ShieldException if type mismatch
     */
    public void setTyped(Object target, Object value) {
        if (value == null) {
            set(target, null);
            return;
        }

        // Check if types are compatible (handles primitive and wrapper types)
        if (!isCompatibleType(value)) {
            throw new ShieldException(
                    "Type mismatch for field: " + fieldName +
                            ". Expected: " + fieldType.getName() +
                            ", but got: " + value.getClass().getName());
        }
        set(target, value);
    }

    /**
     * Checks if the given value is compatible with the field type.
     * Handles primitive types and their wrapper classes.
     *
     * @param value the value to check
     * @return true if compatible, false otherwise
     */
    private boolean isCompatibleType(Object value) {
        Class<?> valueType = value.getClass();

        // Direct type match
        if (fieldType.isInstance(value)) {
            return true;
        }

        // Handle primitive types and their wrappers
        if (fieldType.isPrimitive()) {
            return isPrimitiveWrapperMatch(fieldType, valueType);
        }

        // Handle wrapper types with primitives
        if (isWrapperType(fieldType)) {
            Class<?> primitiveType = getPrimitiveType(fieldType);
            return primitiveType != null && isPrimitiveWrapperMatch(primitiveType, valueType);
        }

        return false;
    }

    /**
     * Checks if a primitive type and its wrapper class match.
     */
    private boolean isPrimitiveWrapperMatch(Class<?> primitiveType, Class<?> wrapperType) {
        if (primitiveType == int.class && wrapperType == Integer.class) return true;
        if (primitiveType == long.class && wrapperType == Long.class) return true;
        if (primitiveType == double.class && wrapperType == Double.class) return true;
        if (primitiveType == float.class && wrapperType == Float.class) return true;
        if (primitiveType == boolean.class && wrapperType == Boolean.class) return true;
        if (primitiveType == byte.class && wrapperType == Byte.class) return true;
        if (primitiveType == short.class && wrapperType == Short.class) return true;
        if (primitiveType == char.class && wrapperType == Character.class) return true;
        return false;
    }

    /**
     * Checks if a class is a primitive wrapper type.
     */
    private boolean isWrapperType(Class<?> clazz) {
        return clazz == Integer.class || clazz == Long.class ||
               clazz == Double.class || clazz == Float.class ||
               clazz == Boolean.class || clazz == Byte.class ||
               clazz == Short.class || clazz == Character.class;
    }

    /**
     * Gets the primitive type for a wrapper class.
     */
    private Class<?> getPrimitiveType(Class<?> wrapperType) {
        if (wrapperType == Integer.class) return int.class;
        if (wrapperType == Long.class) return long.class;
        if (wrapperType == Double.class) return double.class;
        if (wrapperType == Float.class) return float.class;
        if (wrapperType == Boolean.class) return boolean.class;
        if (wrapperType == Byte.class) return byte.class;
        if (wrapperType == Short.class) return short.class;
        if (wrapperType == Character.class) return char.class;
        return null;
    }

    /**
     * Gets the original Field object (for metadata access).
     *
     * @return the field
     */
    public Field getField() {
        return field;
    }

    /**
     * Gets the field name.
     *
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Gets the field type.
     *
     * @return the field type
     */
    public Class<?> getFieldType() {
        return fieldType;
    }

    /**
     * Checks if the field is of String type.
     *
     * @return true if the field is a String
     */
    public boolean isStringType() {
        return String.class.equals(fieldType);
    }

    /**
     * Checks if the field value is null.
     *
     * @param target the object to check
     * @return true if the field value is null
     */
    public boolean isNull(Object target) {
        return get(target) == null;
    }

    @Override
    public String toString() {
        return "FieldAccessor{" +
                "field=" + field.getDeclaringClass().getSimpleName() + "." + fieldName +
                ", type=" + fieldType.getSimpleName() +
                '}';
    }
}
