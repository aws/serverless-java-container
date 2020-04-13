# Serverless Spring Boot 2 example for GraalVM Native Image
This experimental Pet Store example uses [Spring Boot's](https://projects.spring.io/spring-boot/) support for native compilation with [GraalVM](https://www.graalvm.org/) and executes as a custom runtime in AWS Lambda.

The application uses Docker to compile a native executable compatible with AWS Lambda's runtime environment and can be deployed in an AWS account using the [Serverless Application Model](https://github.com/awslabs/serverless-application-model). The `template.yml` file in the root folder contains the application definition.

## Pre-requisites
* [Docker](https://www.docker.com/)
* [AWS CLI](https://aws.amazon.com/cli/)
* [SAM CLI](https://github.com/awslabs/aws-sam-cli)

## Deployment
To make it easy to build our samples with GraalVM, We have published a Docker image that includes GraalVM version 20.0.0, the JDK 8 with backported compiler interfaces (JVMCI), Maven 3.6.3, and the latest snapshot version of Serverless Java Container.

The container image expects the Maven project - the directory containing the `pom.xml` file - to be mounted to the `/func` volume and will execute `/func/scripts/graalvm-build.sh` as it entrypoint.

From the `native-pet-store` sample folder, use a shell to run the build process with Docker
```bash
$ docker run --rm -v $PWD:/func sapessi/aws-serverless-java-container-graalvm-build
```

To deploy the application in your AWS account, you can use the SAM CLI's guided deployment process and follow the instructions on the screen

```
$ sam deploy --guided
```

Once the deployment completes, the SAM CLI will print out the stack's outputs, including the new application URL. You can use `curl` or a web browser to make a call to the URL

```
...
---------------------------------------------------------------------------------------------------------
OutputKey-Description                        OutputValue
---------------------------------------------------------------------------------------------------------
PetStoreApi - URL for application            https://xxxxxxxxxx.execute-api.us-west-2.amazonaws.com/pets
---------------------------------------------------------------------------------------------------------

$ curl https://xxxxxxxxxx.execute-api.us-west-2.amazonaws.com/pets
```