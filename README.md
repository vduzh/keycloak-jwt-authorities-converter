# Keycloak JWT Authorities Converter

A lightweight Spring Security library that extracts granted authorities from Keycloak JWT tokens.

Converts Keycloak realm/client roles into Spring Security `GrantedAuthority` objects with the `ROLE_` prefix. Supports nested JWT claims via dot notation (e.g. `realm_access.roles`) using SpEL expressions.

## Requirements

- Java 25+
- Spring Boot 3.5+

## Installation

### Gradle

```groovy
implementation 'io.github.vduzh:keycloak-jwt-authorities-converter:0.1.0'
```

### Maven

```xml
<dependency>
    <groupId>io.github.vduzh</groupId>
    <artifactId>keycloak-jwt-authorities-converter</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Usage

Configure the converter in your Spring Security configuration:

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    var authoritiesConverter = new KeycloakJwtGrantedAuthoritiesConverter();
    authoritiesConverter.setClaimNames(List.of("realm_access.roles"));

    var jwtConverter = new JwtAuthenticationConverter();
    jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return jwtConverter;
}
```

The converter supports multiple claim names to merge authorities from different sources:

```java
authoritiesConverter.setClaimNames(List.of(
    "realm_access.roles",
    "resource_access.my-client.roles"
));
```

Given a Keycloak JWT token with:

```json
{
  "realm_access": {
    "roles": ["admin", "user"]
  }
}
```

The converter produces: `ROLE_admin`, `ROLE_user`.

## Building

```bash
./gradlew build      # Build + tests
./gradlew test       # Run tests
./gradlew publish    # Publish to Nexus
./gradlew publish -Pcentral  # Publish to Nexus + Maven Central
```

## License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
