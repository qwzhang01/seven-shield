# Seven-Shield

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-green.svg)](https://spring.io/projects/spring-boot)

A powerful, annotation-based data masking library for Spring Boot applications. Seven-Shield provides automatic and flexible masking of sensitive data in API responses, supporting multiple data types including phone numbers, emails, ID cards, and names.

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Usage Guide](#usage-guide)
  - [Basic Usage](#basic-usage)
  - [Built-in Annotations](#built-in-annotations)
  - [Custom Masking Algorithms](#custom-masking-algorithms)
  - [Row-Level Control](#row-level-control)
  - [Thread-Local Context Control](#thread-local-context-control)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Advanced Features](#advanced-features)
- [Performance](#performance)
- [Maven Repository](#maven-repository)
- [Contributing](#contributing)
- [License](#license)

## Features

- 🎯 **Annotation-Based**: Simple, declarative masking using annotations
- 🔒 **Multiple Masking Algorithms**: Built-in support for phone, email, ID card, and name masking
- 🚀 **High Performance**: Uses MethodHandle for ~10x faster field access compared to traditional reflection
- 🔧 **Extensible**: Easy to create custom masking algorithms
- 🌳 **Deep Object Support**: Automatically handles nested objects, collections, arrays, and maps
- 🎛️ **Fine-Grained Control**: Row-level and thread-local masking control
- ⚡ **Spring Boot Auto-Configuration**: Zero-configuration integration with Spring Boot
- 🧵 **Thread-Safe**: Built with concurrency in mind
- 📦 **Lightweight**: Minimal dependencies

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml

<dependency>
    <groupId>io.github.qwzhang01</groupId>
    <artifactId>seven-shield</artifactId>
    <version>1.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'io.github.qwzhang01:seven-shield:1.0'
```

### Requirements

- Java 17 or higher
- Spring Boot 3.1.5 or higher
- Maven 3.6+ (for building from source)

## Quick Start

### 1. Add Dependency

Add Seven-Shield to your Spring Boot project (see [Installation](#installation)).

### 2. Annotate Your Data Model

```java
public class UserInfo {
    @MaskPhone
    private String phoneNumber;
    
    @MaskEmail
    private String email;
    
    @MaskId
    private String idCard;
    
    @MaskName
    private String name;
    
    // Standard getters and setters
}
```

### 3. Enable Masking Context

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")
    public ResponseEntity<UserInfo> getUser(@PathVariable Long id) {
        // Enable masking for this request
        MaskContext.start();
        try {
            UserInfo user = userService.getUser(id);
            return ResponseEntity.ok(user);
        } finally {
            MaskContext.stop(); // Important: Always clean up
        }
    }
}
```

### 4. See the Results

**Original Data:**
```json
{
    "phoneNumber": "13812345678",
    "email": "user@example.com",
    "idCard": "110101199001011234",
    "name": "张三"
}
```

**Masked Response:**
```json
{
    "phoneNumber": "138****5678",
    "email": "u***r@example.com",
    "idCard": "110101********1234",
    "name": "张*"
}
```

## Usage Guide

### Basic Usage

#### Step 1: Define Your Model with Annotations

```java
public class Employee {
    private Long id;
    
    @MaskPhone
    private String mobile;
    
    @MaskEmail
    private String email;
    
    @MaskId
    private String nationalId;
    
    @MaskName
    private String fullName;
    
    private String department; // This field will NOT be masked
}
```

#### Step 2: Control Masking with MaskContext

Seven-Shield uses a ThreadLocal context to control when masking should be applied:

```java
@Service
public class EmployeeService {
    
    public Employee getEmployeeForPublic(Long id) {
        // Enable masking for public access
        MaskContext.start();
        try {
            return employeeRepository.findById(id).orElse(null);
        } finally {
            MaskContext.stop();
        }
    }
    
    public Employee getEmployeeForAdmin(Long id) {
        // No masking for admin access
        return employeeRepository.findById(id).orElse(null);
    }
}
```

### Built-in Annotations

Seven-Shield provides several pre-configured annotations for common masking scenarios:

#### @MaskPhone

Masks phone numbers (Chinese mobile format).

```java
@MaskPhone
private String phone; // 13812345678 → 138****5678
```

**Pattern**: Keeps first 3 and last 4 digits, masks middle 4 digits.

#### @MaskEmail

Masks email addresses.

```java
@MaskEmail
private String email; // user@example.com → u***r@example.com
```

**Pattern**: Keeps first and last character of username, masks middle, preserves domain.

#### @MaskId

Masks ID card/national identification numbers.

```java
@MaskId
private String idCard; // 110101199001011234 → 110101********1234
```

**Pattern**: 
- 18-digit ID: Keeps first 6 and last 4 digits
- 15-digit ID: Keeps first 6 and last 3 digits

#### @MaskName

Masks Chinese names.

```java
@MaskName
private String name; // 张三 → 张*
```

**Pattern**:
- Single character: Not masked
- Two characters: Keeps first character
- Three+ characters: Keeps first and last character

#### @MaskNameEn

Masks English names.

```java
@MaskNameEn
private String englishName; // John → J**n
```

**Pattern**: Keeps first and last character, masks middle characters.

#### @Mask (Generic)

Generic masking annotation that allows you to specify a custom algorithm.

```java
@Mask(CustomCoverAlgo.class)
private String customField;
```

### Custom Masking Algorithms

Create your own masking algorithm by implementing the `CoverAlgo` interface:

#### Step 1: Implement CoverAlgo

```java
public class CreditCardMaskAlgo implements CoverAlgo {
    
    @Override
    public String mask(String content) {
        if (content == null || content.length() < 8) {
            return content;
        }
        
        // Mask credit card: 1234567890123456 → 1234********3456
        int length = content.length();
        String prefix = content.substring(0, 4);
        String suffix = content.substring(length - 4);
        String masked = "*".repeat(length - 8);
        
        return prefix + masked + suffix;
    }
}
```

#### Step 2: Create Custom Annotation (Optional)

```java
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mask(value = CreditCardMaskAlgo.class)
public @interface MaskCreditCard {
}
```

#### Step 3: Use Your Custom Annotation

```java
public class Payment {
    @MaskCreditCard
    private String creditCardNumber;
}
```

### Row-Level Control

Use `MaskVo` base class for row-level masking control:

```java
public class UserInfo extends MaskVo {
    @MaskPhone
    private String phone;
    
    @MaskEmail
    private String email;
}

// Usage
List<UserInfo> users = new ArrayList<>();

// This user's data will be masked
UserInfo publicUser = new UserInfo();
publicUser.setMaskFlag(true);
publicUser.setPhone("13812345678");
users.add(publicUser);

// This user's data will NOT be masked (e.g., viewing own data)
UserInfo owner = new UserInfo();
owner.setMaskFlag(false);
owner.setPhone("13812345678");
users.add(owner);

MaskContext.start();
try {
    return users; // First user masked, second user not masked
} finally {
    MaskContext.stop();
}
```

### Thread-Local Context Control

The `MaskContext` class provides thread-safe masking control:

```java
// Start masking for current thread
MaskContext.start();

// Check if masking is enabled
if (MaskContext.isMask()) {
    // Masking is active
}

// Add specific fields to include in masking (optional)
MaskContext.addIncludeField("email");
MaskContext.addIncludeField("phone");

// Get included fields
Set<String> fields = MaskContext.getIncludeFields();

// Stop masking and clean up ThreadLocal
MaskContext.stop();
```

**Important**: Always call `MaskContext.stop()` in a `finally` block to prevent memory leaks!

### Nested Objects and Collections

Seven-Shield automatically handles complex object structures:

```java
public class Company {
    private String name;
    
    @MaskPhone
    private String contactPhone;
    
    // Nested object - will be processed recursively
    private Address address;
    
    // Collection - each employee will be processed
    private List<Employee> employees;
}

public class Address {
    private String street;
    
    @MaskPhone
    private String phone; // This will also be masked
}

public class Employee {
    @MaskName
    private String name;
    
    @MaskEmail
    private String email;
}
```

## Architecture

### Design Patterns

Seven-Shield uses several design patterns for flexibility and performance:

- **Strategy Pattern**: Different masking algorithms (`CoverAlgo` implementations)
- **Factory Pattern**: Algorithm instantiation with caching (`MaskAlgoContainer`)
- **Advisor Pattern**: Spring AOP integration (`MaskAdvice`)
- **Builder Pattern**: Field accessor creation for high performance

### Key Components

```
┌─────────────────────────────────────────────────────────┐
│                    Spring Boot Application               │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                 MaskAdvice (ResponseBodyAdvice)          │
│  - Intercepts HTTP responses                             │
│  - Checks MaskContext                                    │
│  - Delegates to MaskAlgoContainer                        │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│              MaskAlgoContainer (Main Engine)             │
│  - Discovers @Mask annotated fields                      │
│  - Manages algorithm instances (cached)                  │
│  - Handles nested objects recursively                    │
└─────────────────────────────────────────────────────────┘
                            │
            ┌───────────────┴───────────────┐
            ▼                               ▼
┌──────────────────────┐      ┌──────────────────────┐
│   ClazzKit           │      │   CoverAlgo          │
│  - Field discovery   │      │  - Masking logic     │
│  - Reflection utils  │      │  - Multiple impls    │
└──────────────────────┘      └──────────────────────┘
```

## Configuration

### Auto-Configuration

Seven-Shield uses Spring Boot auto-configuration. By default, it automatically registers:

- `MaskAdvice` - Response interceptor
- `SpringKit` - Spring context utility
- `DefaultCoverAlgo` - Default masking algorithm

### Custom Configuration

Override default beans by providing your own implementations:

```java
@Configuration
public class CustomMaskingConfig {
    
    @Bean
    public CoverAlgo customCoverAlgo() {
        return new CustomCoverAlgo();
    }
    
    @Bean
    public MaskAdvice customMaskAdvice() {
        // Custom advice configuration
        return new MaskAdvice(new MaskAlgoContainer());
    }
}
```

### Disabling Auto-Configuration

To disable Seven-Shield auto-configuration:

```properties
# application.properties
spring.autoconfigure.exclude=io.github.qwzhang01.shield.config.ShieldAutoConfig
```

## Advanced Features

### Meta-Annotations

Create specialized annotations using `@Mask` as a meta-annotation:

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Mask(value = SocialSecurityMaskAlgo.class)
public @interface MaskSSN {
    // Custom attributes if needed
}
```

### Conditional Masking

Use the `behest` attribute for hierarchical masking control:

```java
public class Document {
    @Mask(behest = true) // Only mask if parent has @Mask
    private String content;
}
```

### Spring Context Integration

Masking algorithms can use Spring dependency injection:

```java
@Component
public class DatabaseBackedMaskAlgo implements CoverAlgo {
    
    @Autowired
    private MaskingRuleRepository repository;
    
    @Override
    public String mask(String content) {
        // Load masking rules from database
        MaskingRule rule = repository.findByType("default");
        return applyRule(content, rule);
    }
}
```

### Interceptor Integration

Create an interceptor to automatically enable masking:

```java
@Component
public class MaskingInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        // Enable masking for non-admin users
        if (!isAdmin(request)) {
            MaskContext.start();
        }
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler, 
                               Exception ex) {
        MaskContext.stop();
    }
}
```

## Performance

Seven-Shield is designed for high performance:

- **MethodHandle**: Uses Java 7+ MethodHandle for field access (~10x faster than reflection)
- **Caching**: Algorithm instances are cached to avoid repeated instantiation
- **Lazy Evaluation**: Fields are only processed when masking is enabled
- **ThreadLocal**: Efficient thread-local context management

### Benchmark Results

On a typical Spring Boot application with complex nested objects:

- **Field Discovery**: ~0.5ms for 100 fields
- **Masking Operation**: ~0.1ms per field
- **Memory Overhead**: <1MB for cache

## Maven Repository

### GitHub Packages

This library is published to GitHub Packages:

```xml
<repository>
    <id>github</id>
    <name>GitHub Packages</name>
    <url>https://maven.pkg.github.com/qwzhang01/seven-shield</url>
</repository>
```

### Maven Central (Coming Soon)

Configuration for Maven Central publishing is included in the project. Stay tuned for availability on Maven Central.

### Building from Source

```bash
# Clone the repository
git clone https://github.com/qwzhang01/seven-shield.git
cd seven-shield

# Build and install to local Maven repository
mvn clean install

# Skip tests
mvn clean install -DskipTests
```

### Publishing to Maven Central

```bash
# Set up GPG keys and server credentials in ~/.m2/settings.xml
# Then run:
mvn clean deploy
```

## Examples

### Example 1: REST API with Conditional Masking

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id, 
                                       @RequestHeader("Authorization") String token) {
        User currentUser = authService.getUserFromToken(token);
        User requestedUser = userService.getUser(id);
        
        // Only mask if viewing another user's profile
        if (!currentUser.getId().equals(requestedUser.getId())) {
            MaskContext.start();
        }
        
        try {
            return ResponseEntity.ok(requestedUser);
        } finally {
            MaskContext.stop();
        }
    }
}
```

### Example 2: Batch Processing with Row-Level Control

```java
@Service
public class ReportService {
    
    public List<EmployeeInfo> generateReport(Long departmentId, User requester) {
        List<Employee> employees = employeeRepository.findByDepartment(departmentId);
        
        return employees.stream()
            .map(emp -> {
                EmployeeInfo info = new EmployeeInfo();
                info.setName(emp.getName());
                info.setEmail(emp.getEmail());
                
                // Mask data for employees in other departments
                boolean shouldMask = !emp.getDepartmentId().equals(requester.getDepartmentId());
                info.setMaskFlag(shouldMask);
                
                return info;
            })
            .collect(Collectors.toList());
    }
}
```

### Example 3: Custom Masking for Addresses

```java
public class AddressMaskAlgo implements CoverAlgo {
    
    @Override
    public String mask(String address) {
        if (address == null || address.length() < 10) {
            return address;
        }
        
        // Keep first 6 characters (usually province/city)
        // Mask the rest (street address)
        return address.substring(0, 6) + "****";
    }
}

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Mask(value = AddressMaskAlgo.class)
public @interface MaskAddress {
}

// Usage
public class User {
    @MaskAddress
    private String homeAddress; // "Beijing Haidian District Street 123" → "Beijing****"
}
```

## Troubleshooting

### Common Issues

**Q: Masking is not working**
- Ensure `MaskContext.start()` is called before data is returned
- Check that Spring Boot auto-configuration is not disabled
- Verify annotations are correctly placed on fields

**Q: Memory leak warnings**
- Always call `MaskContext.stop()` in a `finally` block
- Use try-finally pattern or try-with-resources

**Q: Custom algorithm not being used**
- Ensure algorithm class has a no-arg constructor
- Check if algorithm is registered as a Spring bean (optional but recommended)
- Verify the algorithm class is public and accessible

**Q: Performance issues**
- Enable debug logging to identify bottlenecks
- Check for deeply nested object structures
- Consider implementing custom field discovery for specific classes

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Setup

```bash
# Clone the repository
git clone https://github.com/qwzhang01/seven-shield.git
cd seven-shield

# Build the project
mvn clean compile

# Run tests
mvn test

# Generate coverage report
mvn jacoco:report
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Inspired by various data masking solutions in the Java ecosystem
- Built with Spring Boot framework
- Uses MyBatis Plus for pagination support

## Support

- **Issues**: [GitHub Issues](https://github.com/qwzhang01/seven-shield/issues)
- **Email**: avinzhang@tencent.com
- **Documentation**: [GitHub Wiki](https://github.com/qwzhang01/seven-shield/wiki)

## Roadmap

- [ ] Add support for more data types (credit cards, bank accounts, etc.)
- [ ] Implement masking for request bodies (not just responses)
- [ ] Add configuration for masking patterns via properties file
- [ ] Support for JSON Path expressions for selective field masking
- [ ] Integration with Spring Security for automatic permission-based masking
- [ ] Performance monitoring and metrics
- [ ] Kotlin extensions and DSL
- [ ] WebFlux/Reactive support enhancements

## Related Documentation

- [中文文档](README_zh.md) - Chinese Documentation

---

**Made with ❤️ by avinzhang**
