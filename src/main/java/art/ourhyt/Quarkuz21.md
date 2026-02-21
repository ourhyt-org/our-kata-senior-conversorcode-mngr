# Quarkus - Supersonic Subatomic Java

Quarkus is a Kubernetes-native Java framework tailored for GraalVM and HotSpot, designed for building cloud-native applications with incredibly fast startup times and low memory footprint. It unifies imperative and reactive programming models under a single framework, leveraging standards like Jakarta REST, CDI, Hibernate ORM, and Eclipse MicroProfile while providing a developer-friendly experience with features like live reload and Dev Services.

The framework is built around a powerful extension system that allows seamless integration with databases, messaging systems, security providers, and cloud platforms. Quarkus applications can run in JVM mode for development flexibility or be compiled to native executables using GraalVM for production deployments requiring minimal resource consumption. The build-time optimization approach moves as much processing as possible from runtime to build time, resulting in applications that start in milliseconds and consume significantly less memory than traditional Java applications.

## Creating a New Quarkus Project

Bootstrap a new Quarkus application using the Maven plugin or Quarkus CLI. The project generator creates a complete application structure with REST endpoints, configuration files, and Docker support out of the box.

```bash
# Using Maven plugin
mvn io.quarkus.platform:quarkus-maven-plugin:3.17.0:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=getting-started \
    -Dextensions="rest,rest-jackson"

cd getting-started

# Using Quarkus CLI
quarkus create app org.acme:getting-started --extensions="rest,rest-jackson"

# Run in development mode with live reload
./mvnw quarkus:dev
# Or with Quarkus CLI
quarkus dev

# Build for production
./mvnw package

# Build native executable
./mvnw package -Dnative

# Run the packaged application
java -jar target/quarkus-app/quarkus-run.jar
```

## REST Endpoints with Jakarta REST

Create RESTful web services using Jakarta REST annotations. Quarkus REST (formerly RESTEasy Reactive) provides high-performance, non-blocking HTTP handling with automatic JSON serialization through Jackson or JSON-B.

```java
package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

@Path("/fruits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FruitResource {

    private Set<Fruit> fruits = Collections.newSetFromMap(
        Collections.synchronizedMap(new LinkedHashMap<>()));

    public FruitResource() {
        fruits.add(new Fruit("Apple", "Winter fruit"));
        fruits.add(new Fruit("Pineapple", "Tropical fruit"));
    }

    @GET
    public Set<Fruit> list() {
        return fruits;
    }

    @GET
    @Path("/{name}")
    public Response getByName(@PathParam("name") String name) {
        return fruits.stream()
            .filter(f -> f.name.equals(name))
            .findFirst()
            .map(f -> Response.ok(f).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    public Set<Fruit> add(Fruit fruit) {
        fruits.add(fruit);
        return fruits;
    }

    @DELETE
    public Set<Fruit> delete(Fruit fruit) {
        fruits.removeIf(existingFruit ->
            existingFruit.name.contentEquals(fruit.name));
        return fruits;
    }
}

// Model class - public fields auto-serialize to JSON
public class Fruit {
    public String name;
    public String description;

    public Fruit() {}

    public Fruit(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
```

```bash
# Test the REST API
curl http://localhost:8080/fruits
# Output: [{"name":"Apple","description":"Winter fruit"},{"name":"Pineapple","description":"Tropical fruit"}]

curl -X POST -H "Content-Type: application/json" \
    -d '{"name":"Banana","description":"Yellow fruit"}' \
    http://localhost:8080/fruits

curl http://localhost:8080/fruits/Apple
# Output: {"name":"Apple","description":"Winter fruit"}
```

## Dependency Injection with CDI

Use Contexts and Dependency Injection (CDI) for managing application components. Quarkus ArC is a build-time optimized CDI implementation that provides dependency injection, lifecycle management, and interceptors.

```java
package org.acme;

import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

// Application-scoped bean - single instance for entire application
@ApplicationScoped
public class GreetingService {

    @Inject
    TranslationService translator;  // Field injection

    private String prefix;

    @PostConstruct
    void init() {
        prefix = "Hello";
    }

    public String greet(String name) {
        return translator.translate(prefix) + ", " + name + "!";
    }

    @PreDestroy
    void cleanup() {
        // Resource cleanup
    }
}

// Constructor injection (recommended)
@ApplicationScoped
public class TranslationService {

    private final Dictionary dictionary;

    // @Inject is optional when there's only one constructor
    public TranslationService(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    public String translate(String text) {
        return dictionary.lookup(text);
    }
}

// Request-scoped bean - new instance per HTTP request
@RequestScoped
public class RequestContext {
    private String requestId;

    @PostConstruct
    void init() {
        requestId = java.util.UUID.randomUUID().toString();
    }

    public String getRequestId() {
        return requestId;
    }
}

// Producer method for complex object creation
@ApplicationScoped
public class Producers {

    @Produces
    @Singleton
    public Dictionary createDictionary() {
        return new EnglishDictionary();
    }
}

// Using the service in a REST endpoint
@Path("/greet")
public class GreetingResource {

    @Inject
    GreetingService service;

    @GET
    @Path("/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String greet(@PathParam("name") String name) {
        return service.greet(name);
    }
}
```

## Hibernate ORM with Panache

Simplify database operations using Hibernate ORM with Panache, which provides Active Record or Repository patterns for entity management with minimal boilerplate code.

```java
package org.acme;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Active Record Pattern - entity with built-in operations
@Entity
public class Person extends PanacheEntity {
    public String name;
    public LocalDate birth;

    @Enumerated(EnumType.STRING)
    public Status status;

    // Custom finder methods
    public static Person findByName(String name) {
        return find("name", name).firstResult();
    }

    public static List<Person> findAlive() {
        return list("status", Status.Alive);
    }

    public static long deleteByStatus(Status status) {
        return delete("status", status);
    }

    // Query with parameters
    public static List<Person> findByNameAndStatus(String name, Status status) {
        return list("name = ?1 and status = ?2", name, status);
    }

    // Named parameters
    public static List<Person> search(String term) {
        return list("name like :term",
            java.util.Map.of("term", "%" + term + "%"));
    }
}

public enum Status { Alive, Deceased }

// Repository Pattern alternative
@ApplicationScoped
public class PersonRepository implements PanacheRepository<Person> {

    public Person findByName(String name) {
        return find("name", name).firstResult();
    }

    public List<Person> findAlive() {
        return list("status", Status.Alive);
    }
}

// REST resource using Panache entities
@Path("/persons")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PersonResource {

    @GET
    public List<Person> list() {
        return Person.listAll();
    }

    @GET
    @Path("/{id}")
    public Person get(@PathParam("id") Long id) {
        Person person = Person.findById(id);
        if (person == null) {
            throw new NotFoundException();
        }
        return person;
    }

    @POST
    @Transactional
    public Person create(Person person) {
        person.persist();
        return person;
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Person update(@PathParam("id") Long id, Person person) {
        Person entity = Person.findById(id);
        if (entity == null) {
            throw new NotFoundException();
        }
        entity.name = person.name;
        entity.birth = person.birth;
        entity.status = person.status;
        return entity;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void delete(@PathParam("id") Long id) {
        Person entity = Person.findById(id);
        if (entity == null) {
            throw new NotFoundException();
        }
        entity.delete();
    }

    @GET
    @Path("/search/{name}")
    public List<Person> search(@PathParam("name") String name) {
        return Person.search(name);
    }
}
```

```properties
# application.properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=quarkus
quarkus.datasource.password=quarkus
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb

# Auto-create schema in dev/test mode
%dev.quarkus.hibernate-orm.schema-management.strategy=drop-and-create
%test.quarkus.hibernate-orm.schema-management.strategy=drop-and-create
%prod.quarkus.hibernate-orm.schema-management.strategy=none
```

## REST Client

Consume external REST APIs using the type-safe REST Client with automatic JSON serialization and CDI integration.

```java
package org.acme.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import io.quarkus.rest.client.reactive.ClientQueryParam;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import io.smallrye.mutiny.Uni;

// Define the REST client interface
@Path("/extensions")
@RegisterRestClient(configKey = "extensions-api")
@Produces(MediaType.APPLICATION_JSON)
public interface ExtensionsService {

    @GET
    Set<Extension> getAll();

    @GET
    Set<Extension> getById(@QueryParam("id") String id);

    @GET
    @Path("/{id}")
    Extension getExtension(@PathParam("id") String id);

    // Async support with CompletionStage
    @GET
    CompletionStage<Set<Extension>> getAllAsync();

    // Reactive support with Mutiny
    @GET
    Uni<Set<Extension>> getAllReactive();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Extension create(Extension extension);
}

// Extension model
public class Extension {
    public String id;
    public String name;
    public String shortName;
    public java.util.List<String> keywords;
}

// Using the REST client
@ApplicationScoped
public class ExtensionService {

    @Inject
    @RestClient
    ExtensionsService extensionsService;

    public Set<Extension> getAllExtensions() {
        return extensionsService.getAll();
    }

    public Uni<Set<Extension>> getAllExtensionsReactive() {
        return extensionsService.getAllReactive();
    }
}

// REST resource using the client
@Path("/api/extensions")
@Produces(MediaType.APPLICATION_JSON)
public class ExtensionResource {

    @Inject
    @RestClient
    ExtensionsService client;

    @GET
    public Set<Extension> list() {
        return client.getAll();
    }

    @GET
    @Path("/reactive")
    public Uni<Set<Extension>> listReactive() {
        return client.getAllReactive();
    }
}
```

```properties
# application.properties
# REST Client configuration
quarkus.rest-client.extensions-api.url=https://stage.code.quarkus.io/api
quarkus.rest-client.extensions-api.scope=jakarta.inject.Singleton

# Connection and timeout settings
quarkus.rest-client.extensions-api.connect-timeout=5000
quarkus.rest-client.extensions-api.read-timeout=10000
```

## Apache Kafka Messaging

Integrate with Apache Kafka for event-driven messaging using SmallRye Reactive Messaging with automatic serialization and consumer/producer management.

```java
package org.acme.kafka;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import io.smallrye.reactive.messaging.kafka.Record;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CompletionStage;

// Simple consumer - receive payload directly
@ApplicationScoped
public class PriceConsumer {

    @Incoming("prices")
    public void consume(double price) {
        System.out.println("Received price: " + price);
    }
}

// Consumer with message metadata
@ApplicationScoped
public class OrderConsumer {

    @Incoming("orders")
    public CompletionStage<Void> consume(Message<Order> message) {
        Order order = message.getPayload();
        System.out.println("Processing order: " + order.id);
        // Manual acknowledgment
        return message.ack();
    }
}

// Consumer with Kafka record access
@ApplicationScoped
public class EventConsumer {

    @Incoming("events")
    public void consume(Record<String, Event> record) {
        String key = record.key();
        Event event = record.value();
        System.out.println("Event " + key + ": " + event.type);
    }
}

// Producer with @Outgoing
@ApplicationScoped
public class PriceGenerator {

    private Random random = new Random();

    @Outgoing("generated-prices")
    public Multi<Double> generate() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
            .map(tick -> random.nextDouble() * 100);
    }
}

// Processor - consume and produce
@ApplicationScoped
public class PriceProcessor {

    @Incoming("raw-prices")
    @Outgoing("processed-prices")
    public double process(double price) {
        return price * 1.1; // Add 10% markup
    }
}

// Producer with Emitter for imperative sending
@ApplicationScoped
public class OrderService {

    @Inject
    @Channel("orders-out")
    Emitter<Order> orderEmitter;

    public void placeOrder(Order order) {
        orderEmitter.send(order);
    }
}

// REST endpoint to send messages
@Path("/prices")
public class PriceResource {

    @Inject
    @Channel("price-stream")
    Emitter<Double> priceEmitter;

    @Inject
    @Channel("prices")
    Multi<Double> prices;

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void sendPrice(Double price) {
        priceEmitter.send(price);
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<Double> stream() {
        return prices;
    }
}

// Model classes
public class Order {
    public String id;
    public String product;
    public int quantity;
}

public class Event {
    public String type;
    public String data;
}
```

```properties
# application.properties
# Kafka broker configuration
%prod.kafka.bootstrap.servers=kafka:9092

# Incoming channel (consumer)
mp.messaging.incoming.prices.connector=smallrye-kafka
mp.messaging.incoming.prices.topic=prices
mp.messaging.incoming.prices.value.deserializer=org.apache.kafka.common.serialization.DoubleDeserializer

# Outgoing channel (producer)
mp.messaging.outgoing.generated-prices.connector=smallrye-kafka
mp.messaging.outgoing.generated-prices.topic=prices
mp.messaging.outgoing.generated-prices.value.serializer=org.apache.kafka.common.serialization.DoubleSerializer

# JSON serialization for complex types
mp.messaging.incoming.orders.connector=smallrye-kafka
mp.messaging.incoming.orders.value.deserializer=io.quarkus.kafka.client.serialization.ObjectMapperDeserializer
```

## Scheduled Tasks

Create periodic tasks using cron expressions or fixed intervals with the Quarkus Scheduler extension.

```java
package org.acme.scheduler;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class ScheduledTasks {

    private AtomicInteger counter = new AtomicInteger();

    @Inject
    Scheduler scheduler;

    // Fixed interval - every 10 seconds
    @Scheduled(every = "10s")
    void incrementCounter() {
        counter.incrementAndGet();
        System.out.println("Counter: " + counter.get());
    }

    // Cron expression - every day at 10:15 AM
    @Scheduled(cron = "0 15 10 * * ?")
    void dailyTask(ScheduledExecution execution) {
        System.out.println("Daily task executed at: " +
            execution.getScheduledFireTime());
    }

    // Cron from configuration
    @Scheduled(cron = "{cron.expr}")
    void configuredTask() {
        System.out.println("Configurable cron task executed");
    }

    // Delayed start
    @Scheduled(every = "1m", delayed = "30s")
    void delayedTask() {
        System.out.println("Task with 30s initial delay");
    }

    // Named scheduler for programmatic control
    @Scheduled(identity = "myTask", every = "5s")
    void namedTask() {
        System.out.println("Named task");
    }

    // Pause/resume programmatically
    public void pauseMyTask() {
        scheduler.pause("myTask");
    }

    public void resumeMyTask() {
        scheduler.resume("myTask");
    }

    public int getCount() {
        return counter.get();
    }
}
```

```properties
# application.properties
cron.expr=*/5 * * * * ?

# Change cron syntax (default is Quartz)
# quarkus.scheduler.cron-type=unix
```

## Security with Basic Authentication and RBAC

Secure REST endpoints using role-based access control with Jakarta Security annotations and identity providers.

```java
package org.acme.security;

import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.DenyAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import java.security.Principal;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class SecuredResource {

    @Inject
    SecurityIdentity securityIdentity;

    // Public endpoint - no authentication required
    @GET
    @Path("/public")
    @PermitAll
    public String publicEndpoint() {
        return "Public content";
    }

    // Authenticated users only
    @GET
    @Path("/user")
    @Authenticated
    public String userEndpoint() {
        return "Hello, " + securityIdentity.getPrincipal().getName();
    }

    // Role-based access
    @GET
    @Path("/admin")
    @RolesAllowed("admin")
    public String adminEndpoint() {
        return "Admin content";
    }

    // Multiple roles allowed
    @GET
    @Path("/manager")
    @RolesAllowed({"admin", "manager"})
    public String managerEndpoint() {
        return "Manager content";
    }

    // Using SecurityContext
    @GET
    @Path("/me")
    @Authenticated
    public UserInfo getCurrentUser(@Context SecurityContext ctx) {
        Principal principal = ctx.getUserPrincipal();
        return new UserInfo(
            principal.getName(),
            securityIdentity.getRoles()
        );
    }

    // Block all access
    @GET
    @Path("/blocked")
    @DenyAll
    public String blockedEndpoint() {
        return "Never reached";
    }
}

public class UserInfo {
    public String username;
    public java.util.Set<String> roles;

    public UserInfo(String username, java.util.Set<String> roles) {
        this.username = username;
        this.roles = roles;
    }
}
```

```properties
# application.properties
# Enable Basic Authentication
quarkus.http.auth.basic=true

# Embedded users (for development)
quarkus.security.users.embedded.enabled=true
quarkus.security.users.embedded.plain-text=true
quarkus.security.users.embedded.users.admin=admin123
quarkus.security.users.embedded.users.user=user123
quarkus.security.users.embedded.roles.admin=admin,user
quarkus.security.users.embedded.roles.user=user

# Policy-based security
quarkus.http.auth.permission.admin-policy.paths=/api/admin/*
quarkus.http.auth.permission.admin-policy.policy=admin-role
quarkus.http.auth.policy.admin-role.roles-allowed=admin
```

```bash
# Test secured endpoints
curl http://localhost:8080/api/public
# Output: Public content

curl -u user:user123 http://localhost:8080/api/user
# Output: Hello, user

curl -u admin:admin123 http://localhost:8080/api/admin
# Output: Admin content

curl -u user:user123 http://localhost:8080/api/admin
# Output: 403 Forbidden
```

## Configuration Management

Configure Quarkus applications using properties files, environment variables, or programmatic configuration with profile support for different environments.

```java
package org.acme.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;

// Simple property injection
@ApplicationScoped
public class GreetingService {

    @ConfigProperty(name = "greeting.message")
    String message;

    @ConfigProperty(name = "greeting.suffix", defaultValue = "!")
    String suffix;

    @ConfigProperty(name = "greeting.name")
    Optional<String> name;

    public String greet() {
        return message + name.orElse("World") + suffix;
    }
}

// Config mapping interface (recommended)
@ConfigMapping(prefix = "app")
public interface AppConfig {

    String name();

    @WithDefault("8080")
    int port();

    @WithName("api-key")
    String apiKey();

    Optional<String> description();

    List<String> features();

    DatabaseConfig database();

    interface DatabaseConfig {
        String host();
        int port();
        String name();
        Optional<String> username();
    }
}

// Using config mapping
@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigResource {

    @Inject
    AppConfig config;

    @Inject
    @ConfigProperty(name = "app.name")
    String appName;

    @GET
    public ConfigInfo getConfig() {
        return new ConfigInfo(
            config.name(),
            config.port(),
            config.features(),
            config.database().host()
        );
    }
}

public class ConfigInfo {
    public String name;
    public int port;
    public List<String> features;
    public String dbHost;

    public ConfigInfo(String name, int port, List<String> features, String dbHost) {
        this.name = name;
        this.port = port;
        this.features = features;
        this.dbHost = dbHost;
    }
}
```

```properties
# application.properties

# Simple properties
greeting.message=Hello,
greeting.suffix=!

# App configuration
app.name=My Quarkus App
app.port=8080
app.api-key=secret-key-123
app.features=feature1,feature2,feature3

# Nested configuration
app.database.host=localhost
app.database.port=5432
app.database.name=mydb

# Profile-specific configuration
%dev.greeting.message=Hello DEV,
%test.greeting.message=Hello TEST,
%prod.greeting.message=Hello PROD,

# Environment-specific database
%dev.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/devdb
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://prod-db:5432/proddb

# Secrets from environment variables
app.api-key=${API_KEY:default-key}
```

```bash
# Override via system properties
java -Dgreeting.message="Custom " -jar target/quarkus-app/quarkus-run.jar

# Override via environment variables
export GREETING_MESSAGE="Environment "
java -jar target/quarkus-app/quarkus-run.jar

# Run with specific profile
java -Dquarkus.profile=prod -jar target/quarkus-app/quarkus-run.jar

# Multiple profiles
java -Dquarkus.profile=prod,ssl -jar target/quarkus-app/quarkus-run.jar
```

## Building Native Executables

Compile Quarkus applications to native executables using GraalVM for fast startup and low memory consumption, ideal for serverless and containerized deployments.

```bash
# Build native executable (requires GraalVM installed)
./mvnw package -Dnative

# Build using container (no local GraalVM needed)
./mvnw package -Dnative -Dquarkus.native.container-build=true

# Specify GraalVM builder image
./mvnw package -Dnative \
    -Dquarkus.native.container-build=true \
    -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21

# Run native executable
./target/getting-started-1.0.0-SNAPSHOT-runner

# Build native container image
./mvnw package -Dnative -Dquarkus.container-image.build=true

# Docker build with native executable
docker build -f src/main/docker/Dockerfile.native -t myapp .
docker run -i --rm -p 8080:8080 myapp
```

```dockerfile
# src/main/docker/Dockerfile.native
FROM quay.io/quarkus/quarkus-micro-image:2.0
WORKDIR /work/
COPY target/*-runner /work/application
RUN chmod 775 /work
EXPOSE 8080
CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
```

```properties
# application.properties - Native build configuration
quarkus.native.additional-build-args=--initialize-at-build-time=org.myorg.MyClass

# Resources to include in native image
quarkus.native.resources.includes=**/*.json,**/*.xml

# Enable HTTPS in native mode
quarkus.ssl.native=true
```

## Testing Quarkus Applications

Write unit and integration tests using JUnit 5 with Quarkus test extensions that provide automatic application lifecycle management and REST-assured integration.

```java
package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class FruitResourceTest {

    @Test
    public void testListEndpoint() {
        given()
            .when().get("/fruits")
            .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("$.size()", greaterThan(0))
                .body("name", hasItems("Apple", "Pineapple"));
    }

    @Test
    public void testAddFruit() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"name\":\"Banana\",\"description\":\"Yellow fruit\"}")
            .when().post("/fruits")
            .then()
                .statusCode(200)
                .body("name", hasItem("Banana"));
    }

    @Test
    public void testGetByName() {
        given()
            .when().get("/fruits/Apple")
            .then()
                .statusCode(200)
                .body("name", equalTo("Apple"));
    }

    @Test
    public void testNotFound() {
        given()
            .when().get("/fruits/NonExistent")
            .then()
                .statusCode(404);
    }
}

// Test with mock
@QuarkusTest
public class GreetingResourceTest {

    @InjectMock
    GreetingService greetingService;

    @BeforeEach
    public void setup() {
        Mockito.when(greetingService.greet("test"))
            .thenReturn("Mocked Hello, test!");
    }

    @Test
    public void testGreeting() {
        given()
            .when().get("/greet/test")
            .then()
                .statusCode(200)
                .body(equalTo("Mocked Hello, test!"));
    }
}

// Native image test
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class FruitResourceIT extends FruitResourceTest {
    // Runs the same tests against native executable
}

// Test with test profile configuration
@QuarkusTest
@TestProfile(CustomTestProfile.class)
public class CustomProfileTest {
    // Uses custom test configuration
}

public class CustomTestProfile implements io.quarkus.test.junit.QuarkusTestProfile {
    @Override
    public java.util.Map<String, String> getConfigOverrides() {
        return java.util.Map.of(
            "greeting.message", "Test Hello, "
        );
    }
}
```

```xml
<!-- pom.xml test dependencies -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit5</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit5-mockito</artifactId>
    <scope>test</scope>
</dependency>
```

```bash
# Run tests
./mvnw test

# Run integration tests (native)
./mvnw verify -Dnative

# Run specific test
./mvnw test -Dtest=FruitResourceTest

# Run with test containers
./mvnw test -Dquarkus.test.integration-test-profile=test
```

Quarkus serves as a comprehensive platform for building modern Java applications, particularly suited for microservices architectures, serverless functions, and Kubernetes deployments. The framework's extension ecosystem covers virtually every common enterprise requirement including REST APIs, database persistence, messaging, security, observability, and cloud-native integrations. Applications benefit from Dev Services that automatically provision test containers during development, live reload that instantly reflects code changes, and unified configuration that works seamlessly across development, testing, and production environments.

The combination of build-time optimization and native compilation support makes Quarkus ideal for scenarios requiring fast startup times and minimal resource consumption, such as AWS Lambda functions, Kubernetes pods with aggressive scaling policies, or edge computing deployments. For traditional application servers or situations requiring maximum runtime flexibility, the JVM mode provides the full power of the Java ecosystem with hot deployment capabilities. Whether building REST APIs, event-driven systems, or complex enterprise applications, Quarkus provides a productive developer experience while delivering production-ready applications optimized for cloud-native infrastructure.
