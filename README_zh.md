# Seven-Shield（七彩石）

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-green.svg)](https://spring.io/projects/spring-boot)

一个强大的、基于注解的 Spring Boot 应用数据脱敏库。Seven-Shield 提供自动化且灵活的 API 响应数据脱敏功能，支持多种数据类型，包括手机号、邮箱、身份证号和姓名等。

## 目录

- [特性](#特性)
- [安装](#安装)
- [快速开始](#快速开始)
- [使用指南](#使用指南)
  - [基础用法](#基础用法)
  - [内置注解](#内置注解)
  - [自定义脱敏算法](#自定义脱敏算法)
  - [行级控制](#行级控制)
  - [线程上下文控制](#线程上下文控制)
- [架构设计](#架构设计)
- [配置](#配置)
- [高级特性](#高级特性)
- [性能](#性能)
- [Maven 仓库](#maven-仓库)
- [贡献](#贡献)
- [许可证](#许可证)

## 特性

- 🎯 **基于注解**: 使用注解进行简单的声明式脱敏
- 🔒 **多种脱敏算法**: 内置支持手机号、邮箱、身份证、姓名脱敏
- 🚀 **高性能**: 使用 MethodHandle 实现字段访问，比传统反射快 10 倍
- 🔧 **可扩展**: 轻松创建自定义脱敏算法
- 🌳 **深度对象支持**: 自动处理嵌套对象、集合、数组和 Map
- 🎛️ **细粒度控制**: 支持行级和线程级脱敏控制
- ⚡ **Spring Boot 自动配置**: 零配置集成 Spring Boot
- 🧵 **线程安全**: 基于并发设计
- 📦 **轻量级**: 最小化依赖

## 安装

### Maven

在 `pom.xml` 中添加以下依赖：

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

### 环境要求

- Java 17 或更高版本
- Spring Boot 3.1.5 或更高版本
- Maven 3.6+（从源码构建时需要）

## 快速开始

### 1. 添加依赖

将 Seven-Shield 添加到你的 Spring Boot 项目（参见[安装](#安装)）。

### 2. 在数据模型上添加注解

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
    
    // 标准的 getters 和 setters
}
```

### 3. 启用脱敏上下文

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")
    public ResponseEntity<UserInfo> getUser(@PathVariable Long id) {
        // 为当前请求启用脱敏
        MaskContext.start();
        try {
            UserInfo user = userService.getUser(id);
            return ResponseEntity.ok(user);
        } finally {
            MaskContext.stop(); // 重要：始终清理资源
        }
    }
}
```

### 4. 查看效果

**原始数据：**
```json
{
    "phoneNumber": "13812345678",
    "email": "user@example.com",
    "idCard": "110101199001011234",
    "name": "张三"
}
```

**脱敏后的响应：**
```json
{
    "phoneNumber": "138****5678",
    "email": "u***r@example.com",
    "idCard": "110101********1234",
    "name": "张*"
}
```

## 使用指南

### 基础用法

#### 步骤 1：使用注解定义模型

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
    
    private String department; // 此字段不会被脱敏
}
```

#### 步骤 2：使用 MaskContext 控制脱敏

Seven-Shield 使用 ThreadLocal 上下文来控制何时应用脱敏：

```java
@Service
public class EmployeeService {
    
    public Employee getEmployeeForPublic(Long id) {
        // 为公开访问启用脱敏
        MaskContext.start();
        try {
            return employeeRepository.findById(id).orElse(null);
        } finally {
            MaskContext.stop();
        }
    }
    
    public Employee getEmployeeForAdmin(Long id) {
        // 管理员访问不脱敏
        return employeeRepository.findById(id).orElse(null);
    }
}
```

### 内置注解

Seven-Shield 为常见脱敏场景提供了多个预配置注解：

#### @MaskPhone

脱敏手机号（中国手机号格式）。

```java
@MaskPhone
private String phone; // 13812345678 → 138****5678
```

**规则**：保留前 3 位和后 4 位，中间 4 位脱敏。

#### @MaskEmail

脱敏邮箱地址。

```java
@MaskEmail
private String email; // user@example.com → u***r@example.com
```

**规则**：保留用户名的首尾字符，中间部分脱敏，域名保持不变。

#### @MaskId

脱敏身份证/国民身份证号码。

```java
@MaskId
private String idCard; // 110101199001011234 → 110101********1234
```

**规则**：
- 18 位身份证：保留前 6 位和后 4 位
- 15 位身份证：保留前 6 位和后 3 位

#### @MaskName

脱敏中文姓名。

```java
@MaskName
private String name; // 张三 → 张*
```

**规则**：
- 单字姓名：不脱敏
- 两字姓名：保留首字
- 三字及以上：保留首尾字符

#### @MaskNameEn

脱敏英文姓名。

```java
@MaskNameEn
private String englishName; // John → J**n
```

**规则**：保留首尾字符，中间字符脱敏。

#### @Mask（通用）

通用脱敏注解，允许指定自定义算法。

```java
@Mask(CustomCoverAlgo.class)
private String customField;
```

### 自定义脱敏算法

通过实现 `CoverAlgo` 接口创建自己的脱敏算法：

#### 步骤 1：实现 CoverAlgo

```java
public class CreditCardMaskAlgo implements CoverAlgo {
    
    @Override
    public String mask(String content) {
        if (content == null || content.length() < 8) {
            return content;
        }
        
        // 脱敏信用卡：1234567890123456 → 1234********3456
        int length = content.length();
        String prefix = content.substring(0, 4);
        String suffix = content.substring(length - 4);
        String masked = "*".repeat(length - 8);
        
        return prefix + masked + suffix;
    }
}
```

#### 步骤 2：创建自定义注解（可选）

```java
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mask(value = CreditCardMaskAlgo.class)
public @interface MaskCreditCard {
}
```

#### 步骤 3：使用自定义注解

```java
public class Payment {
    @MaskCreditCard
    private String creditCardNumber;
}
```

### 行级控制

使用 `MaskVo` 基类实现行级脱敏控制：

```java
public class UserInfo extends MaskVo {
    @MaskPhone
    private String phone;
    
    @MaskEmail
    private String email;
}

// 使用示例
List<UserInfo> users = new ArrayList<>();

// 此用户的数据将被脱敏
UserInfo publicUser = new UserInfo();
publicUser.setMaskFlag(true);
publicUser.setPhone("13812345678");
users.add(publicUser);

// 此用户的数据不会被脱敏（例如查看自己的数据）
UserInfo owner = new UserInfo();
owner.setMaskFlag(false);
owner.setPhone("13812345678");
users.add(owner);

MaskContext.start();
try {
    return users; // 第一个用户脱敏，第二个用户不脱敏
} finally {
    MaskContext.stop();
}
```

### 线程上下文控制

`MaskContext` 类提供线程安全的脱敏控制：

```java
// 为当前线程启动脱敏
MaskContext.start();

// 检查是否启用了脱敏
if (MaskContext.isMask()) {
    // 脱敏已激活
}

// 添加要包含在脱敏中的特定字段（可选）
MaskContext.addIncludeField("email");
MaskContext.addIncludeField("phone");

// 获取包含的字段
Set<String> fields = MaskContext.getIncludeFields();

// 停止脱敏并清理 ThreadLocal
MaskContext.stop();
```

**重要提示**：始终在 `finally` 块中调用 `MaskContext.stop()` 以防止内存泄漏！

### 嵌套对象和集合

Seven-Shield 自动处理复杂的对象结构：

```java
public class Company {
    private String name;
    
    @MaskPhone
    private String contactPhone;
    
    // 嵌套对象 - 将递归处理
    private Address address;
    
    // 集合 - 每个员工都将被处理
    private List<Employee> employees;
}

public class Address {
    private String street;
    
    @MaskPhone
    private String phone; // 这也会被脱敏
}

public class Employee {
    @MaskName
    private String name;
    
    @MaskEmail
    private String email;
}
```

## 架构设计

### 设计模式

Seven-Shield 使用多种设计模式以提供灵活性和性能：

- **策略模式**：不同的脱敏算法（`CoverAlgo` 实现）
- **工厂模式**：带缓存的算法实例化（`MaskAlgoContainer`）
- **通知模式**：Spring AOP 集成（`MaskAdvice`）
- **构建器模式**：高性能字段访问器创建

### 核心组件

```
┌─────────────────────────────────────────────────────────┐
│                    Spring Boot 应用                      │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                 MaskAdvice (ResponseBodyAdvice)          │
│  - 拦截 HTTP 响应                                        │
│  - 检查 MaskContext                                      │
│  - 委托给 MaskAlgoContainer                              │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│              MaskAlgoContainer (主引擎)                  │
│  - 发现 @Mask 注解的字段                                 │
│  - 管理算法实例（缓存）                                  │
│  - 递归处理嵌套对象                                      │
└─────────────────────────────────────────────────────────┘
                            │
            ┌───────────────┴───────────────┐
            ▼                               ▼
┌──────────────────────┐      ┌──────────────────────┐
│   ClazzKit           │      │   CoverAlgo          │
│  - 字段发现           │      │  - 脱敏逻辑          │
│  - 反射工具           │      │  - 多种实现          │
└──────────────────────┘      └──────────────────────┘
```

## 配置

### 自动配置

Seven-Shield 使用 Spring Boot 自动配置。默认情况下，它自动注册：

- `MaskAdvice` - 响应拦截器
- `SpringKit` - Spring 上下文工具
- `DefaultCoverAlgo` - 默认脱敏算法

### 自定义配置

通过提供自己的实现来覆盖默认 Bean：

```java
@Configuration
public class CustomMaskingConfig {
    
    @Bean
    public CoverAlgo customCoverAlgo() {
        return new CustomCoverAlgo();
    }
    
    @Bean
    public MaskAdvice customMaskAdvice() {
        // 自定义通知配置
        return new MaskAdvice(new MaskAlgoContainer());
    }
}
```

### 禁用自动配置

禁用 Seven-Shield 自动配置：

```properties
# application.properties
spring.autoconfigure.exclude=io.github.qwzhang01.shield.config.ShieldAutoConfig
```

## 高级特性

### 元注解

使用 `@Mask` 作为元注解创建专用注解：

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Mask(value = SocialSecurityMaskAlgo.class)
public @interface MaskSSN {
    // 如需要可添加自定义属性
}
```

### 条件脱敏

使用 `behest` 属性实现层级脱敏控制：

```java
public class Document {
    @Mask(behest = true) // 仅在父级有 @Mask 时才脱敏
    private String content;
}
```

### Spring 上下文集成

脱敏算法可以使用 Spring 依赖注入：

```java
@Component
public class DatabaseBackedMaskAlgo implements CoverAlgo {
    
    @Autowired
    private MaskingRuleRepository repository;
    
    @Override
    public String mask(String content) {
        // 从数据库加载脱敏规则
        MaskingRule rule = repository.findByType("default");
        return applyRule(content, rule);
    }
}
```

### 拦截器集成

创建拦截器以自动启用脱敏：

```java
@Component
public class MaskingInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        // 为非管理员用户启用脱敏
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

## 性能

Seven-Shield 专为高性能设计：

- **MethodHandle**：使用 Java 7+ MethodHandle 进行字段访问（比反射快约 10 倍）
- **缓存**：算法实例被缓存以避免重复实例化
- **延迟求值**：仅在启用脱敏时处理字段
- **ThreadLocal**：高效的线程本地上下文管理

### 性能基准

在具有复杂嵌套对象的典型 Spring Boot 应用中：

- **字段发现**：100 个字段约 0.5ms
- **脱敏操作**：每个字段约 0.1ms
- **内存开销**：缓存小于 1MB

## Maven 仓库

### GitHub Packages

此库发布到 GitHub Packages：

```xml
<repository>
    <id>github</id>
    <name>GitHub Packages</name>
    <url>https://maven.pkg.github.com/qwzhang01/seven-shield</url>
</repository>
```

### Maven Central（即将推出）

项目中已包含 Maven Central 发布配置。敬请关注 Maven Central 的可用性。

### 从源码构建

```bash
# 克隆仓库
git clone https://github.com/qwzhang01/seven-shield.git
cd seven-shield

# 构建并安装到本地 Maven 仓库
mvn clean install

# 跳过测试
mvn clean install -DskipTests
```

### 发布到 Maven Central

```bash
# 在 ~/.m2/settings.xml 中设置 GPG 密钥和服务器凭据
# 然后运行：
mvn clean deploy
```

## 示例

### 示例 1：带条件脱敏的 REST API

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
        
        // 仅在查看其他用户资料时脱敏
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

### 示例 2：带行级控制的批处理

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
                
                // 为其他部门的员工脱敏数据
                boolean shouldMask = !emp.getDepartmentId().equals(requester.getDepartmentId());
                info.setMaskFlag(shouldMask);
                
                return info;
            })
            .collect(Collectors.toList());
    }
}
```

### 示例 3：自定义地址脱敏

```java
public class AddressMaskAlgo implements CoverAlgo {
    
    @Override
    public String mask(String address) {
        if (address == null || address.length() < 10) {
            return address;
        }
        
        // 保留前 6 个字符（通常是省市）
        // 脱敏其余部分（街道地址）
        return address.substring(0, 6) + "****";
    }
}

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Mask(value = AddressMaskAlgo.class)
public @interface MaskAddress {
}

// 使用
public class User {
    @MaskAddress
    private String homeAddress; // "北京市海淀区中关村大街123号" → "北京市海淀****"
}
```

## 故障排除

### 常见问题

**问：脱敏不起作用**
- 确保在返回数据之前调用了 `MaskContext.start()`
- 检查 Spring Boot 自动配置是否未被禁用
- 验证注解是否正确放置在字段上

**问：内存泄漏警告**
- 始终在 `finally` 块中调用 `MaskContext.stop()`
- 使用 try-finally 模式或 try-with-resources

**问：自定义算法未被使用**
- 确保算法类有无参构造函数
- 检查算法是否注册为 Spring Bean（可选但推荐）
- 验证算法类是公共的且可访问

**问：性能问题**
- 启用调试日志以识别瓶颈
- 检查深度嵌套的对象结构
- 考虑为特定类实现自定义字段发现

## 贡献

欢迎贡献！请遵循以下指南：

1. Fork 仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 打开 Pull Request

### 开发环境设置

```bash
# 克隆仓库
git clone https://github.com/qwzhang01/seven-shield.git
cd seven-shield

# 构建项目
mvn clean compile

# 运行测试
mvn test

# 生成覆盖率报告
mvn jacoco:report
```

## 许可证

本项目根据 MIT 许可证授权 - 详见 [LICENSE](LICENSE) 文件。

## 致谢

- 受 Java 生态系统中各种数据脱敏解决方案的启发
- 使用 Spring Boot 框架构建
- 使用 MyBatis Plus 提供分页支持

## 支持

- **问题反馈**：[GitHub Issues](https://github.com/qwzhang01/seven-shield/issues)
- **邮箱**：avinzhang@tencent.com
- **文档**：[GitHub Wiki](https://github.com/qwzhang01/seven-shield/wiki)

## 路线图

- [ ] 添加更多数据类型支持（信用卡、银行账户等）
- [ ] 实现请求体脱敏（不仅仅是响应）
- [ ] 通过属性文件配置脱敏模式
- [ ] 支持 JSON Path 表达式进行选择性字段脱敏
- [ ] 与 Spring Security 集成实现基于权限的自动脱敏
- [ ] 性能监控和指标
- [ ] Kotlin 扩展和 DSL
- [ ] WebFlux/响应式支持增强

## 相关文档

- [English Documentation](README.md) - 英文文档

---

**由 avinzhang 用 ❤️ 制作**
