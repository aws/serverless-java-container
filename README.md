# Serverless Java container [![Build Status](https://github.com/aws/serverless-java-container/workflows/Continuous%20Integration/badge.svg)](https://github.com/aws/serverless-java-container/actions) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.amazonaws.serverless/aws-serverless-java-container/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.amazonaws.serverless/aws-serverless-java-container) [![Help](http://img.shields.io/badge/help-gitter-E91E63.svg?style=flat-square)](https://gitter.im/aws/serverless-java-container)
The `aws-serverless-java-container` makes it easy to run Java applications written with frameworks such as [Spring](https://spring.io/), [Spring Boot](https://projects.spring.io/spring-boot/), [Apache Struts](http://struts.apache.org/), [Jersey](https://jersey.java.net/), or [Spark](http://sparkjava.com/) in [AWS Lambda](https://aws.amazon.com/lambda/).

Serverless Java Container natively supports API Gateway's proxy integration models for requests and responses, you can create and inject custom models for methods that use custom mappings.

Currently the following versions are maintained:

| Version | Branch | Java Enterprise support | Spring versions | JAX-RS/ Jersey version | Struts support | Spark support |
|---------|--------|-------------------------|-----------------|------------------------|----------------|---------------|
| 1.x                      | [1.x](https://github.com/aws/serverless-java-container/tree/1.x)    | Java EE (javax.*)       | 5.x (Boot 2.x)  | 2.x                    | :white_check_mark: | :white_check_mark: |
| 2.x     | [main](https://github.com/aws/serverless-java-container/tree/main)   | Jakarta EE (jakarta.*)  | 6.x (Boot 3.x)  | 3.x                    | :x:            | :x:           |

Follow the quick start guides in [our wiki](https://github.com/aws/serverless-java-container/wiki) to integrate Serverless Java Container with your project:
* [Spring quick start](https://github.com/aws/serverless-java-container/wiki/Quick-start---Spring)
* [Spring Boot 2 quick start](https://github.com/aws/serverless-java-container/wiki/Quick-start---Spring-Boot2)
* [Spring Boot 3 quick start](https://github.com/aws/serverless-java-container/wiki/Quick-start---Spring-Boot3)
* [Apache Struts quick start](https://github.com/aws/serverless-java-container/wiki/Quick-start---Struts)
* [Jersey quick start](https://github.com/aws/serverless-java-container/wiki/Quick-start---Jersey)
* [Spark quick start](https://github.com/aws/serverless-java-container/wiki/Quick-start---Spark)

Below is the most basic AWS Lambda handler example that launches a Spring application. You can also take a look at the [samples](https://github.com/aws/serverless-java-container/tree/master/samples) in this repository, our main wiki page includes a [step-by-step guide](https://github.com/aws/serverless-java-container/wiki#deploying-the-sample-applications) on how to deploy the various sample applications using Maven and [SAM](https://github.com/awslabs/serverless-application-model).

```java
public class StreamLambdaHandler implements RequestStreamHandler {
    private static final SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    static {
        try {
            handler = SpringLambdaContainerHandler.getAwsProxyHandler(PetStoreSpringAppConfig.class);
        } catch (ContainerInitializationException e) {
            // if we fail here. We re-throw the exception to force another cold start
            e.printStackTrace();
            throw new RuntimeException("Could not initialize Spring framework", e);
        }
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}
``` 

## Public Examples

### Blogs

- [Re-platforming Java applications using the updated AWS Serverless Java Container](https://aws.amazon.com/blogs/compute/re-platforming-java-applications-using-the-updated-aws-serverless-java-container/)

### Workshops

- [Java on AWS Lambda](https://catalog.workshops.aws/java-on-aws-lambda) From Serverful to Serverless Java with AWS Lambda in 2 hours

### Videos

- [Spring on AWS Lambda](https://www.youtube.com/watch?v=A1rYiHTy9Lg&list=PLCOG9xkUD90IDm9tcY-5nMK6X6g8SD-Sz) YouTube Playlist from [@plantpowerjames](https://twitter.com/plantpowerjames)

### Java samples with different frameworks 

- [Dagger, Micronaut, Quarkus, Spring Boot](https://github.com/aws-samples/serverless-java-frameworks-samples/)
