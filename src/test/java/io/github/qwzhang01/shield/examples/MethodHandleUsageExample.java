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

package io.github.qwzhang01.shield.examples;

import io.github.qwzhang01.shield.annotation.MaskEmail;
import io.github.qwzhang01.shield.annotation.MaskName;
import io.github.qwzhang01.shield.annotation.MaskPhone;
import io.github.qwzhang01.shield.cache.FieldAccessorCache;
import io.github.qwzhang01.shield.domain.FieldAccessor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

/**
 * Examples demonstrating MethodHandle usage in the masking framework.
 *
 * @author avinzhang
 */
public class MethodHandleUsageExample {

    static class User {
        @MaskName
        private String name;

        @MaskPhone
        private String phone;

        @MaskEmail
        private String email;

        private int age;

        public User(String name, String phone, String email, int age) {
            this.name = name;
            this.phone = phone;
            this.email = email;
            this.age = age;
        }

        @Override
        public String toString() {
            return "User{name='" + name + "', phone='" + phone + 
                   "', email='" + email + "', age=" + age + '}';
        }
    }

    @Test
    void example1_BasicUsage() throws NoSuchFieldException {
        System.out.println("\n=== Example 1: Basic FieldAccessor Usage ===");

        // 创建用户对象
        User user = new User("张三", "13812345678", "zhangsan@example.com", 30);
        System.out.println("Original: " + user);

        // 获取字段
        Field phoneField = User.class.getDeclaredField("phone");

        // 创建 FieldAccessor（高性能访问器）
        FieldAccessor accessor = new FieldAccessor(phoneField);

        // 读取字段值（使用 MethodHandle，~10倍快于反射）
        String phone = accessor.getAsString(user);
        System.out.println("Phone: " + phone);

        // 修改字段值（使用 MethodHandle）
        accessor.set(user, "138****5678");
        System.out.println("Masked: " + user);
    }

    @Test
    void example2_UsingCache() throws NoSuchFieldException {
        System.out.println("\n=== Example 2: Using FieldAccessorCache ===");

        User user = new User("李四", "13987654321", "lisi@example.com", 25);
        Field emailField = User.class.getDeclaredField("email");

        // 方式1：使用缓存（推荐）
        FieldAccessor accessor = FieldAccessorCache.getAccessor(emailField);
        String email = accessor.getAsString(user);
        System.out.println("Original email: " + email);

        // 脱敏
        accessor.set(user, "l***i@example.com");
        System.out.println("Masked user: " + user);

        // 查看缓存统计
        System.out.println("\nCache stats: " + FieldAccessorCache.getStats());
    }

    @Test
    void example3_BatchProcessing() throws NoSuchFieldException {
        System.out.println("\n=== Example 3: Batch Processing with Cache ===");

        // 创建1000个用户
        User[] users = new User[1000];
        for (int i = 0; i < users.length; i++) {
            users[i] = new User(
                    "User" + i,
                    "138" + String.format("%08d", i),
                    "user" + i + "@example.com",
                    20 + (i % 50)
            );
        }

        Field phoneField = User.class.getDeclaredField("phone");
        Field emailField = User.class.getDeclaredField("email");

        // 使用缓存的 FieldAccessor（只创建一次）
        FieldAccessor phoneAccessor = FieldAccessorCache.getAccessor(phoneField);
        FieldAccessor emailAccessor = FieldAccessorCache.getAccessor(emailField);

        // 批量处理
        long start = System.nanoTime();
        for (User user : users) {
            String phone = phoneAccessor.getAsString(user);
            String email = emailAccessor.getAsString(user);

            // 脱敏处理
            if (phone != null && phone.length() == 11) {
                phoneAccessor.set(user, phone.substring(0, 3) + "****" + phone.substring(7));
            }

            if (email != null && email.contains("@")) {
                int atIndex = email.indexOf('@');
                emailAccessor.set(user, 
                    email.charAt(0) + "***" + email.substring(atIndex - 1));
            }
        }
        long end = System.nanoTime();

        System.out.printf("Processed %d users in %.2f ms%n", 
                         users.length, (end - start) / 1_000_000.0);
        System.out.println("Sample result: " + users[0]);
        System.out.println("Cache stats: " + FieldAccessorCache.getStats());
    }

    @Test
    void example4_TypeSafety() throws NoSuchFieldException {
        System.out.println("\n=== Example 4: Type-Safe Access ===");

        User user = new User("王五", "13611112222", "wangwu@example.com", 35);
        Field ageField = User.class.getDeclaredField("age");

        FieldAccessor accessor = FieldAccessorCache.getAccessor(ageField);

        // 类型安全的读取
        Integer age = accessor.getTyped(user, Integer.class);
        System.out.println("Age: " + age);

        // 类型检查的写入
        accessor.setTyped(user, 36);
        System.out.println("Updated age: " + accessor.get(user));

        // 类型不匹配会抛出异常
        try {
            accessor.setTyped(user, "Not a number");
        } catch (Exception e) {
            System.out.println("Expected error: " + e.getMessage());
        }
    }

    @Test
    void example5_NullSafety() throws NoSuchFieldException {
        System.out.println("\n=== Example 5: Null Safety ===");

        User user = new User(null, null, null, 0);
        Field nameField = User.class.getDeclaredField("name");

        FieldAccessor accessor = FieldAccessorCache.getAccessor(nameField);

        // 检查是否为 null
        if (accessor.isNull(user)) {
            System.out.println("Name is null, skipping mask");
        }

        // 安全的字符串转换
        String name = accessor.getAsString(user);
        System.out.println("Name as string: " + name);

        // 设置非 null 值
        accessor.set(user, "New Name");
        System.out.println("Is null after set? " + accessor.isNull(user));
    }

    @Test
    void example6_CacheManagement() throws NoSuchFieldException {
        System.out.println("\n=== Example 6: Cache Management ===");

        // 清空缓存
        FieldAccessorCache.clear();
        System.out.println("Initial cache size: " + FieldAccessorCache.size());

        // 添加一些访问器到缓存
        Field nameField = User.class.getDeclaredField("name");
        Field phoneField = User.class.getDeclaredField("phone");
        Field emailField = User.class.getDeclaredField("email");

        FieldAccessorCache.getAccessor(nameField);
        FieldAccessorCache.getAccessor(phoneField);
        FieldAccessorCache.getAccessor(emailField);

        System.out.println("Cache size after adding 3: " + FieldAccessorCache.size());

        // 移除特定字段
        boolean removed = FieldAccessorCache.remove(nameField);
        System.out.println("Removed name field? " + removed);
        System.out.println("Cache size after removal: " + FieldAccessorCache.size());

        // 移除整个类的所有字段
        int removedCount = FieldAccessorCache.removeByClass(User.class);
        System.out.println("Removed " + removedCount + " accessors for User class");
        System.out.println("Final cache size: " + FieldAccessorCache.size());
    }

    @Test
    void example7_PerformanceComparison() throws Exception {
        System.out.println("\n=== Example 7: Performance Comparison ===");

        User user = new User("测试用户", "13800138000", "test@example.com", 30);
        Field phoneField = User.class.getDeclaredField("phone");
        phoneField.setAccessible(true);

        int iterations = 100000;

        // 使用反射
        long start1 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Object value = phoneField.get(user);
            phoneField.set(user, value);
        }
        long end1 = System.nanoTime();
        double reflectionTime = (end1 - start1) / 1_000_000.0;

        // 使用 MethodHandle
        FieldAccessor accessor = new FieldAccessor(phoneField);
        long start2 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Object value = accessor.get(user);
            accessor.set(user, value);
        }
        long end2 = System.nanoTime();
        double methodHandleTime = (end2 - start2) / 1_000_000.0;

        System.out.printf("Iterations: %,d%n", iterations);
        System.out.printf("Reflection time: %.2f ms%n", reflectionTime);
        System.out.printf("MethodHandle time: %.2f ms%n", methodHandleTime);
        System.out.printf("Speedup: %.1fx faster%n", reflectionTime / methodHandleTime);
        System.out.printf("Time saved: %.2f ms (%.1f%%)%n",
                         reflectionTime - methodHandleTime,
                         ((reflectionTime - methodHandleTime) / reflectionTime) * 100);
    }

    @Test
    void example8_RealWorldScenario() throws NoSuchFieldException {
        System.out.println("\n=== Example 8: Real-World Masking Scenario ===");

        // 模拟查询数据库返回的用户列表
        User[] users = {
            new User("张三", "13812345678", "zhangsan@example.com", 30),
            new User("李四", "13987654321", "lisi@example.com", 25),
            new User("王五", "13611112222", "wangwu@example.com", 35),
            new User("赵六", "13533334444", "zhaoliu@example.com", 28)
        };

        System.out.println("Before masking:");
        for (User user : users) {
            System.out.println("  " + user);
        }

        // 获取需要脱敏的字段访问器（使用缓存）
        Field phoneField = User.class.getDeclaredField("phone");
        Field emailField = User.class.getDeclaredField("email");

        FieldAccessor phoneAccessor = FieldAccessorCache.getAccessor(phoneField);
        FieldAccessor emailAccessor = FieldAccessorCache.getAccessor(emailField);

        // 批量脱敏
        for (User user : users) {
            // 手机号脱敏
            String phone = phoneAccessor.getAsString(user);
            if (phone != null && phone.length() == 11) {
                phoneAccessor.set(user, phone.substring(0, 3) + "****" + phone.substring(7));
            }

            // 邮箱脱敏
            String email = emailAccessor.getAsString(user);
            if (email != null && email.contains("@")) {
                int atIndex = email.indexOf('@');
                String username = email.substring(0, atIndex);
                String domain = email.substring(atIndex);
                if (username.length() > 2) {
                    String masked = username.charAt(0) + "***" + 
                                   username.charAt(username.length() - 1) + domain;
                    emailAccessor.set(user, masked);
                }
            }
        }

        System.out.println("\nAfter masking:");
        for (User user : users) {
            System.out.println("  " + user);
        }

        System.out.println("\nCache statistics:");
        System.out.println(FieldAccessorCache.getStats());
    }
}
