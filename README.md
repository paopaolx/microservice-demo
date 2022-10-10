> youtube视频学习：[https://www.youtube.com/watch?v=BnknNTN8icw](https://www.youtube.com/watch?v=BnknNTN8icw)

### 1. 项目整体架构
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665414752898-b54847d1-9158-46c6-b8bb-08bc2551cc87.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=525&id=u3378a2e8&margin=%5Bobject%20Object%5D&name=image.png&originHeight=580&originWidth=839&originalType=binary&ratio=1&rotation=0&showTitle=false&size=170506&status=done&style=none&taskId=u07d9abd4-08f9-477b-9e03-800dfbac00d&title=&width=759.8490839490021)
### 2. 项目编码实现
#### 2.1. 创建department-service （微服务）
直接浏览器访问：https://start.spring.io/，通过勾选填写项目配置信息，并在线搜索需要的第三方依赖，生成springboot项目源码
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665414773329-cf240ca4-f786-4536-ab27-555b2c657771.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=389&id=u7556a98d&margin=%5Bobject%20Object%5D&name=image.png&originHeight=429&originWidth=840&originalType=binary&ratio=1&rotation=0&showTitle=false&size=68482&status=done&style=none&taskId=u9dba50c6-c2e1-40ff-b7b1-15357135546&title=&width=760.7547443589532)
department-service微服务项目选用h2内存数据库，持久层框架选用JPA。
添加配置文件application.yml设置服务启动端口。
简单创建web项目的几个目录结构 controller、entity、repository、service。

创建两个rest api，分别是saveDepartment 和 findDepartmentById：
```java
@RestController
@RequestMapping("/departments")
@Slf4j
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @PostMapping("/")
    public Department saveDepartment(@RequestBody Department department){
        log.info("Inside saveDepartment method of DepartmentController");
        return departmentService.saveDepartment(department);
    }

    @GetMapping("/{id}")
    public Department findDepartmentById(@PathVariable("id") Long departmentId){
        log.info("Inside findDepartmentById method of DepartmentController");
        return departmentService.findDepartmentById(departmentId);
    }
```

通过postman测试api访问正常😀

#### 2.2. 创建user-service （微服务）
创建的步骤与上述2.1基本一致。
差异：
多创建了一个VO的目录，一个是Department的实体类，一个是包含Users和Department两个的实体类ResponseTemplateVO。

同样创建两个rest api，分别是saveUser和getUserWithDepartment：
```java
@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/")
    public Users saveUser(@RequestBody Users user){
        log.info("Inside saveUser of UserController");
        return userService.saveUser(user);
    }

    @GetMapping("/{id}")
    public ResponseTemplateVO getUserWithDepartment(@PathVariable("id") Long userId){
        log.info("Inside getUserWithDepartment of UserController");
        return userService.getUserWithDepartment(userId);
    }

}
```

其中，getUserWithDepartment接口是通过userId查询user和department的组合数据，因为user表中有外键departmentId，可以关联查询到用户的所属部门信息。但是由于查询部门信息的接口在department-service中，所以user-service中注册了一个restTemplete的bean，通过restTemplate.getForObject()方法请求远程department-service的接口，获取数据。
```java
@SpringBootApplication
public class UserServiceApplication {

   public static void main(String[] args) {
      SpringApplication.run(UserServiceApplication.class, args);
   }

   // 向Spring容器中注册restTemplate的bean
   @Bean
   RestTemplate restTemplate(){
      return new RestTemplate();
   }
}
```

```java
public ResponseTemplateVO getUserWithDepartment(Long userId) {
    log.info("Inside getUserWithDepartment of UserService");
    ResponseTemplateVO vo = new ResponseTemplateVO();
    Users user = userRepository.findByUserId(userId);
    // 通过RestTemplate请求远程服务上的get接口，获取用户的部门信息 [同步的RMI调用]
    Department department = restTemplate.getForObject("http://localhost:9001/departments/"+user.getDepartmentId(),
            Department.class);
    vo.setUser(user);
    vo.setDepartment(department);
    return vo;
}
```

⚠ 注意：新版本的springBoot中，启动会报错。因为创建了一个名为User的实体，可能跟springBoot内部的实体类名称冲突了，之后改成了Users，启动成功。

#### 2.3. 创建service-registry服务（注册中心）
service-registry即eureka服务端，是微服务系统中的注册。本质是一个普通的springboot-web项目。引入了eureka-server的依赖：
```xml
<dependency>
   <groupId>org.springframework.cloud</groupId>
   <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

而且需要在配置文件中添加eureka server相关的配置：
```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

还需要在启动类头部添加注解 [**@EnableEurekaServer **](/EnableEurekaServer )** **,项目启动时会激活eureka-server相关的配置。
```java
@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryApplication {

   public static void main(String[] args) {
      SpringApplication.run(ServiceRegistryApplication.class, args);
   }

}
```

还需在department-service和user-service中添加eureka client相关依赖，并在各自的application.yml中添加eureka client的相关配置。然后启动两个服务，它们会作为服务提供者，将服务的名称，访问地址等信息注册到service-registry中。

先启动service-registry服务，再启动department-service和user-service，浏览器访问：[http://localhost:8761/](http://localhost:8761/), 可以打开默认的Eureka可视化监控界面：
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665414837812-fcef606b-dde5-4bdd-91f3-75baea63b256.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=365&id=ucfd16cca&margin=%5Bobject%20Object%5D&name=image.png&originHeight=403&originWidth=844&originalType=binary&ratio=1&rotation=0&showTitle=false&size=63979&status=done&style=none&taskId=u929db551-4786-4d9e-ba18-14eeb9d01f7&title=&width=764.3773859987577)

现在，可以将user-service中的RMI调用换成：
```java
Department department = restTemplate.getForObject("http://DEPARTMENT-SERVICE/departments/"+user.getDepartmentId(),
        Department.class);
```

请求的ip和port换成了应用名，这样请求还不能生效，需要在restTemplete的方法上添加一个注解[**@LoadBalanced **](/LoadBalanced )** **,表示通过restTemplate发起的RMI请求会通过应用名进行请求的负载均衡。

修改这两处之后，先调用department的添加接口：
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665414857996-89758c2f-0832-48e0-9e66-143bcc471971.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=343&id=uf834993a&margin=%5Bobject%20Object%5D&name=image.png&originHeight=379&originWidth=835&originalType=binary&ratio=1&rotation=0&showTitle=false&size=63630&status=done&style=none&taskId=u08dcc951-30ad-4aa6-95b2-0ad0c30f639&title=&width=756.2264423091975)

再调用user的添加接口：
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665414870466-017e27ab-f392-4a23-a7aa-39d204a5800d.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=356&id=u7a94d6cc&margin=%5Bobject%20Object%5D&name=image.png&originHeight=393&originWidth=833&originalType=binary&ratio=1&rotation=0&showTitle=false&size=62256&status=done&style=none&taskId=u55300b5b-d3cd-4bc6-83a9-a2f96e176f9&title=&width=754.4151214892953)

最后调用user的查询接口：
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665414882381-6007fb32-547e-44f8-895c-4f4ab7d82d69.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=437&id=ud2fb41fc&margin=%5Bobject%20Object%5D&name=image.png&originHeight=483&originWidth=830&originalType=binary&ratio=1&rotation=0&showTitle=false&size=76356&status=done&style=none&taskId=u9918cc3a-75c8-4dce-aad7-0a8474e4bf3&title=&width=751.6981402594419)

请求响应正常，而且返回了user和department的数据。

#### 2.4. 创建cloud-gateway（网关）
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665414895984-3ccfa395-7e0e-4d50-8491-9edd98275e5b.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=374&id=u89aeae3b&margin=%5Bobject%20Object%5D&name=image.png&originHeight=413&originWidth=841&originalType=binary&ratio=1&rotation=0&showTitle=false&size=99680&status=done&style=none&taskId=u2b98f30d-d7c0-4b7f-a656-eb3ac1a488e&title=&width=761.6604047689044)
_注意：pom文件种引入的spring-cloud依赖，需要注意springboot版本和springcloud版本的匹配_

修改application.yml文件，添加路由配置：
```yaml
server:
  port: 9191

spring:
  application:
    name: API-GATEWAY
  cloud:
    gateway:
      routes:
        - id: USER-SERVICE
          uri: lb://USER-SERVICE
          predicates:
            - Path=/users/**
        - id: DEPARTMENT-SERVICE
          uri: lb://DEPARTMENT-SERVICE
          predicates:
            - Path=/departments/**

eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    hostname: localhost
```

启动类头部添加[**@EnableEurekaClient **](/EnableEurekaClient )** **注解

启动后，cloud-gateway也会作为一个微服务注册到eureka-server中。

由于在cloud-gateway中配置了department和user微服务的访问路由。客户端对于各个微服务接口的请求都通过网关服务请求，即可以通过网关服务的ip和port以及路由 ==> localhost:9191/xxx/xx 访问department或user服务的api。
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665414922977-48f7d433-35d6-4d8f-8d8a-55f6db00fa1f.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=405&id=ua05cadfe&margin=%5Bobject%20Object%5D&name=image.png&originHeight=447&originWidth=838&originalType=binary&ratio=1&rotation=0&showTitle=false&size=70589&status=done&style=none&taskId=ued92014e-effa-4905-a450-f0d2cccc7b3&title=&width=758.943423539051)
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665414932765-603b45f9-ffa2-4179-b0ba-a78286ffd18b.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=312&id=u42227663&margin=%5Bobject%20Object%5D&name=image.png&originHeight=344&originWidth=818&originalType=binary&ratio=1&rotation=0&showTitle=false&size=58499&status=done&style=none&taskId=u6192c01f-8662-4560-a6df-aff2082fd1a&title=&width=740.8302153400283)
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665414943531-b8175c98-18ff-4775-a05b-aa705a7290b6.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=398&id=udbe1e707&margin=%5Bobject%20Object%5D&name=image.png&originHeight=439&originWidth=829&originalType=binary&ratio=1&rotation=0&showTitle=false&size=66414&status=done&style=none&taskId=u2fc29844-b656-4dd6-a823-71183c92e47&title=&width=750.7924798494907)

为了防止某个微服务挂掉，导致网关请求失败，可以在网关服务中添加熔断配置。

1. 首先需引入hystrix依赖：
```xml
<dependency>
   <groupId>org.springframework.cloud</groupId>
   <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
   <version>2.2.10.RELEASE</version>
</dependency>
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
	<version>2.1.4</version>
</dependency>
```

2. 然后添加熔断相关的配置：
```yaml
hystrix:
  command:
    fallbackcmd:
      execution:
        isolation:
          thread:
            timeoutInMillseconds: 4000

management:
  endpoints:
    web:
      exposure:
        include: hystrix.stream
```

3.  需要在启动类头部添加一个注解 [**@EnableHystrix **](/EnableHystrix )** **。 
4.  创建一个熔断回调方法的controller，简单定义两个请求department-service和user-service服务失败的回调方法： 
```java
/**
 * 定义两个微服务Department-Service和User-Service的熔断方法
 * @author lixing
 * @since 2022/10/10
 */
@RestController
public class FallBackMethodController {

    @GetMapping("/userServiceFallBack")
    public String userServiceFallBackMethod(){
        return "User Service is taking longer than Expected. Please try again later";
    }

    @GetMapping("/departmentServiceFallBack")
    public String departmentServiceFallBackMethod(){
        return "Department Service is taking longer than Expected. Please try again later";
    }
}
```

如关闭department-service服务，然后在postman中通过GET请求 [http://localhost:9191/departments/1](http://localhost:9191/departments/1) ，会发现响应内容为 “Department Service is taking longer than Expected. Please try again later”，即调用了熔断回调方法

#### 2.5. 创建hystrix-dashboard (流量监控)
该项目核心依赖有两个：
```xml
<dependency>
   <groupId>org.springframework.cloud</groupId>
   <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
<dependency>
   <groupId>org.springframework.cloud</groupId>
   <artifactId>spring-cloud-starter-netflix-hystrix-dashboard</artifactId>
   <version>2.2.10.RELEASE</version>
</dependency>
```

启动类上需要添加两个注解：[@EnableEurekaClient ](/EnableEurekaClient ) 和 @EnableHystrixDashboard 

指定一个端口9295，启动成功后在浏览器访问：[http://localhost:9295/hystrix](http://localhost:9295/hystrix) 可以进入流量监控主页
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665414965559-f7460c58-6807-486c-905d-5c3318985fe3.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=403&id=ub15baa9a&margin=%5Bobject%20Object%5D&name=image.png&originHeight=445&originWidth=824&originalType=binary&ratio=1&rotation=0&showTitle=false&size=152844&status=done&style=none&taskId=u9d18eceb-0cfa-4aea-8ac2-41fc5384950&title=&width=746.2641777997351)

浏览器访问一个网关的端点：[http://localhost:9191/actuator/hystrix.stream](http://localhost:9191/actuator/hystrix.stream)
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665414976183-2c5130e5-5937-4222-81bc-5e400737deef.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=353&id=u2182ef5d&margin=%5Bobject%20Object%5D&name=image.png&originHeight=390&originWidth=836&originalType=binary&ratio=1&rotation=0&showTitle=false&size=66144&status=done&style=none&taskId=u915ab925-fa65-40d4-9e52-ea9b731de11&title=&width=757.1321027191487)

然后将端点地址输入到 hystrix dashboard中，点击下方按钮进行流监控。之后使用postman通过网关访问微服务接口，再去流监控面板，可以看到请求的流监控信息：
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665414986199-67975ded-4ccf-41c5-a997-27a111856e46.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=367&id=u776663fb&margin=%5Bobject%20Object%5D&name=image.png&originHeight=405&originWidth=827&originalType=binary&ratio=1&rotation=0&showTitle=false&size=128568&status=done&style=none&taskId=u97ba0fce-403c-4676-8e96-424220aeadf&title=&width=748.9811590295885)

#### 2.6. 创建cloud-server (分布式配置中心)
分布式配置中心是为了将多个微服务的配置进行集中管理，集中管理到github或其他仓库中。
可以在程序运行过程中，对配置进行动态调整。
可以关闭不同环境下的不同配置，对配置进行更新和部署。
如果系统配置文件发生了变化，无需重启服务器，配置中心就能够自动感知到变化，并将新的变化统一发送到相应程序上。

cloud-server项目核心配置有两个：
```yaml
<dependency>
   <groupId>org.springframework.cloud</groupId>
   <artifactId>spring-cloud-config-server</artifactId>
</dependency>
<dependency>
   <groupId>org.springframework.cloud</groupId>
   <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

启动类头部添加两个注解：[@EnableEurekaClient ](/EnableEurekaClient ) 和 [@EnableConfigServer ](/EnableConfigServer ) 
指定启动端口9296，和应用名称 CONFIG-SERVER

然后在github上创建一个仓库，添加一个公共配置文件 aplication.yml
```yaml
eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    hostname: localhost
```

将仓库的地址配置到cloud-server项目的配置文件中：
```yaml
server:
  port: 9296
spring:
  application:
    name: CONFIG-SERVER
  cloud:
    config:
      server:
        git:
          uri: https://github.com/paopaolx/config-server
          clone-on-start: true
```

然后在使用了公共配置的几个服务 [department-service，user-service，cloud-gateway，hystrix-dashboard] 中添加依赖：
```xml
<dependency>
   <groupId>org.springframework.cloud</groupId>
   <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

并创建一个新的配置文件 bootstrap.yml
```yaml
spring:
  cloud:
    config:
      enabled: true
      uri: http://localhost:9296
```

即使用cloud-service中指定仓库中的公共配置
later~~~
再次启动所有服务，通过网关访问微服务的接口，依然正常

#### 2.7. 使用zipkin（分布式链路追踪）
zipkin服务，直接在官网下载最新稳定版本的jar包：
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665415008070-2ae49e94-a89a-4adf-ba80-e7b914c4e25e.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=526&id=uf15f9a37&margin=%5Bobject%20Object%5D&name=image.png&originHeight=581&originWidth=830&originalType=binary&ratio=1&rotation=0&showTitle=false&size=162997&status=done&style=none&taskId=ua70afbe8-9c37-495e-8fa8-7310612d38d&title=&width=751.6981402594419)

然后通过 java -jar zipkin.jar 启动，启动成功后会出现一个访问地址：[http://127.0.0.1:9411/](http://127.0.0.1:9411/)
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665415017756-ce8c115d-601a-4f7f-81f9-1297652be319.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=233&id=uafcf4200&margin=%5Bobject%20Object%5D&name=image.png&originHeight=257&originWidth=825&originalType=binary&ratio=1&rotation=0&showTitle=false&size=37551&status=done&style=none&taskId=u10d31caa-76f1-4ae3-bd50-cda63f028ef&title=&width=747.1698382096862)

接下来，就是在微服务项目中进行zipkin相关的配置了。
在department-service和user-service中添加两个依赖：
```xml
<dependency>
   <groupId>org.springframework.cloud</groupId>
   <artifactId>spring-cloud-starter-zipkin</artifactId>
   <version>2.2.8.RELEASE</version>
</dependency>
<dependency>
   <groupId>org.springframework.cloud</groupId>
   <artifactId>spring-cloud-starter-sleuth</artifactId>
   <version>3.1.4</version>
</dependency>
```

然后在配置文件中配置zipkin的地址：
```yaml
spring:
  zipkin:
    base-url: http://127.0.0.1:9411/
```

重新启动，然后通过网关访问这两个微服务中的接口，观察微服务中的日志打印，其中会有链路请求的id
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665415034468-fc04f43b-b354-4951-a5b7-43c55a1972f7.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=211&id=u0ee60382&margin=%5Bobject%20Object%5D&name=image.png&originHeight=233&originWidth=830&originalType=binary&ratio=1&rotation=0&showTitle=false&size=209698&status=done&style=none&taskId=ufccb0931-7ef8-4696-b332-6716180f1f8&title=&width=751.6981402594419)

前面的序号id不同代表是两次不同的链路请求。而且在zipkin可视化界面中通过填写指定的服务名称，也可以可视化追踪请求调用的链路，全过程。
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665415043677-01ae797f-a6df-4b54-b037-bd07af250a13.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=197&id=ufba88f05&margin=%5Bobject%20Object%5D&name=image.png&originHeight=218&originWidth=818&originalType=binary&ratio=1&rotation=0&showTitle=false&size=40200&status=done&style=none&taskId=ufdf2a2e3-f723-4087-a8ed-f3a02bbbdfe&title=&width=740.8302153400283)
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665415055765-5f4f9b4f-5eff-475d-b6d4-260bfb15cbf1.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=336&id=ua75bfa1a&margin=%5Bobject%20Object%5D&name=image.png&originHeight=371&originWidth=813&originalType=binary&ratio=1&rotation=0&showTitle=false&size=111254&status=done&style=none&taskId=u7410133c-34d2-49d5-9d97-e92618678b4&title=&width=736.3019132902726)
![image.png](https://cdn.nlark.com/yuque/0/2022/png/2307211/1665415064373-00a0b607-71b4-4cf8-8298-6cdd0df8ad4c.png#clientId=u8aac5e5b-4ede-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=363&id=u94cb7406&margin=%5Bobject%20Object%5D&name=image.png&originHeight=401&originWidth=825&originalType=binary&ratio=1&rotation=0&showTitle=false&size=132636&status=done&style=none&taskId=u2990caec-1cca-4758-9cea-5d3227999d0&title=&width=747.1698382096862)

### 3. 总结
整个通过springboot开发微服务demo项目的过程其实并不复杂，每个模块主要的操作都是引入starter依赖和添加配置和注解，编码的过程并不多。主要是上层的组织，如何进行合理的配置将多个微服务模块组织起来，管理起来。
