package io.github.qwzhang01.shield.kit;

import io.github.qwzhang01.shield.MaskAlgoContainer;
import io.github.qwzhang01.shield.annotation.Mask;
import io.github.qwzhang01.shield.domain.MaskField;
import io.github.qwzhang01.shield.domain.MaskVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Utility class for reflection-based operations on classes and fields for
 * data masking.
 *
 * <p>This class provides comprehensive functionality for:</p>
 * <ul>
 *   <li>Discovering fields annotated with {@link Mask} or meta-annotations</li>
 *   <li>Recursively traversing object graphs (including collections, arrays,
 *   maps)</li>
 *   <li>Filtering fields based on modifiers and types</li>
 *   <li>High-performance field access using MethodHandle</li>
 * </ul>
 *
 * <p><strong>Supported Data Structures:</strong></p>
 * <ul>
 *   <li>Simple objects with annotated fields</li>
 *   <li>Nested objects (unlimited depth)</li>
 *   <li>Collections (List, Set, etc.)</li>
 *   <li>Arrays</li>
 *   <li>Maps (values are processed)</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class ClazzKit {
    private static final Logger log =
            LoggerFactory.getLogger(MaskAlgoContainer.class);

    private static final Set<Class<?>> PRIMITIVE_TYPES = Set.of(
            String.class, Integer.class, Long.class, Double.class, Float.class,
            Boolean.class, Byte.class, Short.class, Character.class,
            int.class, long.class, double.class, float.class,
            boolean.class, byte.class, short.class, char.class
    );

    /**
     * Discovers and collects all fields requiring masking in the given object.
     *
     * <p>This is the main entry point for field discovery. It recursively
     * traverses the object graph and identifies all fields annotated with
     * {@link Mask} or its meta-annotations.</p>
     *
     * @param obj the object to scan for maskable fields
     * @return a list of MaskField instances representing fields to be masked,
     * empty list if object is null or has no maskable fields
     */
    public static List<MaskField> getMaskFields(Object obj) {
        if (obj == null) {
            return Collections.emptyList();
        }
        Boolean maskFlag = null;
        if (obj instanceof MaskVo maskVo) {
            maskFlag = Boolean.TRUE.equals(maskVo.getMaskFlag());
        }

        return collectMaskFields(obj, maskFlag, false);
    }

    private static List<MaskField> collectMaskFields(Object obj,
                                                     Boolean maskFlag,
                                                     boolean parentFieldHasMask) {
        if (obj == null) {
            return Collections.emptyList();
        }
        try {

            if (obj instanceof Collection<?> collection) {
                return processCollection(collection, maskFlag,
                        parentFieldHasMask);
            } else if (obj.getClass().isArray()) {
                return processArray((Object[]) obj, maskFlag,
                        parentFieldHasMask);
            } else if (obj instanceof Map<?, ?> map) {
                return processMap(map, maskFlag, parentFieldHasMask);
            } else {

                Class<?> clazz = obj.getClass();
                Field[] fields = clazz.getDeclaredFields();

                List<MaskField> list = new ArrayList<>();

                for (Field field : fields) {
                    if (shouldSkipField(field)) {
                        continue;
                    }

                    field.setAccessible(true);

                    Mask maskAnnotation = findMaskAnnotation(field);
                    if (maskAnnotation != null) {
                        Object fieldValue = field.get(obj);
                        if (fieldValue != null) {
                            if (maskFlag == null) {
                                if (obj instanceof MaskVo maskVo) {
                                    maskFlag =
                                            Boolean.TRUE.equals(maskVo.getMaskFlag());
                                }
                            }

                            if (isComplexObject(fieldValue.getClass())) {
                                List<MaskField> maskFields =
                                        collectMaskFields(fieldValue,
                                                maskFlag, true);
                                if (!maskFields.isEmpty()) {
                                    list.addAll(maskFields);
                                }
                            } else {

                                boolean behest = maskAnnotation.behest();
                                if (behest) {
                                    behest = parentFieldHasMask;
                                } else {
                                    behest = true;
                                }

                                // Use FieldAccessorCache for high performance
                                MaskField maskField =
                                        io.github.qwzhang01.shield.cache.FieldAccessorCache
                                                .createMaskField(field, obj,
                                                        behest,
                                                        maskFlag == null || maskFlag,
                                                        maskAnnotation);
                                list.add(maskField);
                            }
                        }
                    } else {
                        // Field doesn't have @Mask annotation, but still
                        // need to
                        // recursively process its nested fields
                        Object fieldValue = field.get(obj);
                        if (fieldValue != null) {
                            if (maskFlag == null) {
                                if (obj instanceof MaskVo maskVo) {
                                    maskFlag =
                                            Boolean.TRUE.equals(maskVo.getMaskFlag());
                                }
                            }
                            if (isComplexObject(fieldValue.getClass())) {
                                List<MaskField> maskFields =
                                        collectMaskFields(fieldValue,
                                                maskFlag, false);
                                if (!maskFields.isEmpty()) {
                                    list.addAll(maskFields);
                                }
                            }
                        }
                    }
                }

                return list;
            }
        } catch (Exception e) {
            log.error("Masking exception occurred during field collection", e);
            return Collections.emptyList();
        }
    }

    /**
     * Finds the @Mask annotation on a field, including meta-annotations.
     *
     * <p>This method searches for @Mask in two ways:</p>
     * <ol>
     *   <li>Direct annotation: Field directly annotated with @Mask</li>
     *   <li>Meta-annotation: Field annotated with an annotation that is itself
     *       annotated with @Mask (e.g., @MaskPhone, @MaskEmail)</li>
     * </ol>
     *
     * @param field the field to check for @Mask annotation
     * @return the @Mask annotation if found, null otherwise
     */
    private static Mask findMaskAnnotation(Field field) {
        // First check for direct annotation
        Mask mask = field.getAnnotation(Mask.class);
        if (mask != null) {
            return mask;
        }

        // Then check for meta-annotations
        for (Annotation annotation : field.getAnnotations()) {
            Mask metaMask =
                    annotation.annotationType().getAnnotation(Mask.class);
            if (metaMask != null) {
                return metaMask;
            }
        }

        return null;
    }


    /**
     * Determines whether a field should be skipped during masking.
     * Skips static, final, and transient fields.
     *
     * @param field the field to check
     * @return true if the field should be skipped, false otherwise
     */
    private static boolean shouldSkipField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || Modifier.isTransient(modifiers);
    }

    /**
     * Checks if a class is a primitive type or common Java type that doesn't
     * need recursive processing.
     *
     * @param clazz the class to check
     * @return true if the class is a primitive or common type, false otherwise
     */
    private static boolean isPrimitiveOrCommonType(Class<?> clazz) {
        if (clazz == null) {
            return true;
        }
        return PRIMITIVE_TYPES.contains(clazz) ||
                clazz.isPrimitive() ||
                clazz.isEnum() ||
                clazz.getPackageName().startsWith("java.") ||
                clazz.getPackageName().startsWith("javax.");
    }

    /**
     * Determines if a class is a complex object that requires recursive
     * processing.
     *
     * @param clazz the class to check
     * @return true if the class is complex and needs recursive processing,
     * false otherwise
     */
    private static boolean isComplexObject(Class<?> clazz) {
        if (Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz) || clazz.isArray()) {
            return true;
        }

        return !isPrimitiveOrCommonType(clazz);
    }

    /**
     * Processes all elements in a collection recursively.
     *
     * @param collection         the collection to process
     * @param maskFlag           the masking flag from parent context
     * @param parentFieldHasMask whether the parent field has @Mask annotation
     * @return list of MaskField instances found in collection elements
     */
    private static List<MaskField> processCollection(Collection<?> collection,
                                                     Boolean maskFlag,
                                                     boolean parentFieldHasMask) {

        List<MaskField> list = new ArrayList<>();

        for (Object item : collection) {
            if (item != null && isComplexObject(item.getClass())) {
                List<MaskField> maskFields = collectMaskFields(item, maskFlag
                        , parentFieldHasMask);
                if (!maskFields.isEmpty()) {
                    list.addAll(maskFields);
                }
            }
        }

        return list;
    }

    /**
     * Processes all elements in an array recursively.
     *
     * @param array              the array to process
     * @param maskFlag           the masking flag from parent context
     * @param parentFieldHasMask whether the parent field has @Mask annotation
     * @return list of MaskField instances found in array elements
     */
    private static List<MaskField> processArray(Object[] array,
                                                Boolean maskFlag,
                                                boolean parentFieldHasMask) {

        List<MaskField> list = new ArrayList<>();

        for (int i = 0; i < array.length; i++) {
            Object item = array[i];
            if (item != null && isComplexObject(item.getClass())) {
                List<MaskField> maskFields = collectMaskFields(item, maskFlag
                        , parentFieldHasMask);
                if (!maskFields.isEmpty()) {
                    list.addAll(maskFields);
                }
            }
        }
        return list;
    }

    /**
     * Processes all values in a map recursively.
     * Note: Only map values are processed, keys are not masked.
     *
     * @param map                the map to process
     * @param maskFlag           the masking flag from parent context
     * @param parentFieldHasMask whether the parent field has @Mask annotation
     * @return list of MaskField instances found in map values
     */
    private static List<MaskField> processMap(Map<?, ?> map,
                                              Boolean maskFlag,
                                              boolean parentFieldHasMask) {

        List<MaskField> list = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value != null && isComplexObject(value.getClass())) {
                List<MaskField> maskFields = collectMaskFields(value,
                        maskFlag, parentFieldHasMask);
                if (!maskFields.isEmpty()) {
                    list.addAll(maskFields);
                }
            }
        }
        return list;
    }
}
