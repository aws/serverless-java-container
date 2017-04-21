# Serverless Java container [![Build Status](https://travis-ci.org/awslabs/aws-serverless-java-container.svg?branch=master)](https://travis-ci.org/awslabs/aws-serverless-java-container) [![Help](http://img.shields.io/badge/help-gitter-E91E63.svg?style=flat-square)](https://gitter.im/awslabs/aws-serverless-java-container)
The `aws-serverless-java-container` is collection of interfaces and their implementations that let you run Java application written with frameworks such as [Jersey](https://jersey.java.net/) or [Spark](http://sparkjava.com/) in [AWS Lambda](https://aws.amazon.com/lambda/).

The library contains a core artifact called `aws-serverless-java-container-core` that defines the interfaces and base classes required as well as default implementation of the Java servlet `HttpServletRequest` and `HttpServletResponse`.
The library also includes two initial implementations of the interfaces to support Jersey apps (`aws-serverless-java-container-jersey`) and Spark (`aws-serverless-java-container-spark`).

To include the library in your Maven project, add the desired implementation to your `pom.xml` file, for example:

```
<dependency>
    <groupId>com.amazonaws.serverless</groupId>
    <artifactId>aws-serverless-java-container-jersey</artifactId>
    <version>0.4</version>
</dependency>
```

## Integrating with Lambda
The simplest way to run your application serverlessly is to configure [API Gateway](https://aws.amazon.com/api-gateway/) to use the
[`AWS_PROXY`](http://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-set-up-simple-proxy.html#api-gateway-set-up-lambda-proxy-integration-on-proxy-resource) integration type and
configure your desired `LambdaContainerHandler` implementation to use `AwsProxyRequest`/`AwsProxyResponse` readers and writers. Both Spark and Jersey implementations provide static helper methods that
pre-configure this for you.

### Jersey support
The library expects to receive a valid [JAX-RS](https://jax-rs-spec.java.net) application object. For the Jersey implementation this is the `ResourceConfig` object.

```java
public class LambdaHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {
    private ResourceConfig jerseyApplication = new ResourceConfig().packages("my.jersey.app.package");
    private JerseyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler
        = JerseyLambdaContainerHandler.getAwsProxyHandler(jerseyApplication);

    public AwsProxyResponse handleRequest(AwsProxyRequest awsProxyRequest, Context context) {
        return handler.proxy(awsProxyRequest, context);
    }
}
```

### Spring support
The library supports Spring applications that are configured using annotations (in code) rather than in an XML file. The simplest possible configuration uses the `@ComponentScan` annotation to load all controller classes from a package. For example, our unit test application has the following configuration class.

```java
@Configuration
@ComponentScan("com.amazonaws.serverless.proxy.spring.echoapp")
public class EchoSpringAppConfig {
}
```

Once you have declared a configuration class, you can initialize the library with the class name:
```java
public class LambdaHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {
    SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler = 
        SpringLambdaContainerHandler.getAwsProxyHandler(EchoSpringAppConfig.class);
    
    public AwsProxyResponse handleRequest(AwsProxyRequest awsProxyRequest, Context context) {
        return handler.proxy(awsProxyRequest, context);
    }
}
```

#### Spring Profiles
You can enable Spring Profiles (as defined with the `@Profile` annotation) by using the `SpringLambdaContainerHandler.activateSpringProfiles(String...)` method - common drivers of this might be the AWS Lambda stage that you're deployed under, or stage variables.  See [@Profile documentation](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/context/annotation/Profile.html) for details.

### Spark support
The library also supports applications written with the [Spark framework](http://sparkjava.com/). When using the library with Spark, it's important to initialize the `SparkLambdaContainerHandler` before defining routes.

```java
public class LambdaHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {
    private SparkLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler = 
            SparkLambdaContainerHandler.getAwsProxyHandler();
    private boolean initialized = false;
    
    public AwsProxyResponse handleRequest(AwsProxyRequest awsProxyRequest, Context context) {
        if (!initialized) {
            defineRoutes();
            initialized = true;
        }
        return handler.proxy(awsProxyRequest, context);
    }
    
    private void defineRoutes() {
        get("/hello", (req, res) -> "Hello World");
    }
}
```

## Security context
The `aws-serverless-java-container-core` contains a default implementation of the `SecurityContextWriter` that supports API Gateway's proxy integration. The generated security context uses the API Gateway `$context` object to establish the request security context. The context looks for the following values in order and returns the first matched type:

1. Cognito My User Pools
2. Custom authorizers
3. IAM auth.

The String values for these are exposed as static variables in the `AwsProxySecurityContext` object.

1. `AUTH_SCHEME_COGNITO_POOL`
2. `AUTH_SCHEME_CUSTOM`
3. `AUTH_SCHEME_IAM`

## Supporting other event types
The `RequestReader` and `ResponseWriter` interfaces in the core package can be used to support event types and generate different responses. For example, ff you have configured mapping templates in
API Gateway to create a custom event body or response you can create your own implementation of the `RequestReader` and `ResponseWriter` to handle these.

The `LambdaContainerHandler` also requires a `SecurityContextWriter` and an `ExceptionHandler`. You can also create custom implementations of these interfaces.
 
The `RequestReader`, `ResponseWriter`, `SecurityContextWriter`, and `ExceptionHandler` objects are passed to the constructor of the `LambdaContainerHandler` implementation:
 
```java
JerseyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler =
    new JerseyLambdaContainerHandler<>(new MyCustomRequestReader(),
                                       new MyCustomResponseWriter(),
                                       new MyCustomSecurityContextWriter(),
                                       new MyCustomExceptionHandler(),
                                       jaxRsApplication);
```
 
## Jersey Servlet request
The `aws-serverless-java-container-jersey` includes a Jersey factory class to produce `HttpServletRequest` objects for your methods. First, you will need to register the factory with your Jersey application.

```java
ResourceConfig app = new ResourceConfig()
    .packages("com.amazonaws.serverless.proxy.test.jersey")
    .register(new AbstractBinder() {
        @Override
        protected void configure() {
            bindFactory(JerseyAwsProxyServletRequestFactory.class)
                .to(HttpServletRequest.class)
                .in(RequestScoped.class);
        }
    });
```

Once the factory is registered, you can receive `HttpServletRequest` objects in your methods.

```java
@Path("/my-servlet") @GET
public String echoServletHeaders(@Context HttpServletRequest context) {
    Enumeration<String> headerNames = context.getHeaderNames();
    while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();
    }
    return "servlet";
}
```

## Servlet Filters
You can register [`Filter`](https://docs.oracle.com/javaee/7/api/javax/servlet/Filter.html) implementations by implementing a `StartupsHandler` as defined in the `AwsLambdaServletContainerHandler` class. The `onStartup` methods receives a reference to the current `ServletContext`.

```java
handler.onStartup(c -> {
    FilterRegistration.Dynamic registration = c.addFilter("CustomHeaderFilter", CustomHeaderFilter.class);
    // update the registration to map to a path
    registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
    // servlet name mappings are disabled and will throw an exception
});
```
