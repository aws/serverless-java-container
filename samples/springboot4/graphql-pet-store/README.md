# Serverless Spring Boot 4 with GraphQL example
A basic pet store written with the [Spring Boot 4 framework](https://projects.spring.io/spring-boot/) and Spring Framework 7.0. Unlike older examples, this example uses the [Spring for GraphQl](https://docs.spring.io/spring-graphql/reference/) library. 


The application can be deployed in an AWS account using the [Serverless Application Model](https://github.com/awslabs/serverless-application-model). The `template.yml` file in the root folder contains the application definition.

## Pre-requisites
* [AWS CLI](https://aws.amazon.com/cli/)
* [SAM CLI](https://github.com/awslabs/aws-sam-cli)
* [Gradle](https://gradle.org/) or [Maven](https://maven.apache.org/)

## Deployment
In a shell, navigate to the sample's folder and use the SAM CLI to build a deployable package
```
$ sam build
```

This command compiles the application and prepares a deployment package in the `.aws-sam` sub-directory.

To deploy the application in your AWS account, you can use the SAM CLI's guided deployment process and follow the instructions on the screen

```
$ sam deploy --guided
```

Once the deployment is completed, the SAM CLI will print out the stack's outputs, including the new application URL. You can use `curl` to make a call to the URL

```
...
---------------------------------------------------------------------------------------------------------
OutputKey-Description                        OutputValue
---------------------------------------------------------------------------------------------------------
PetStoreApi - URL for application            https://xxxxxxxxxx.execute-api.us-west-2.amazonaws.com/graphQl
---------------------------------------------------------------------------------------------------------

$ curl -X POST https://xxxxxxxxxx.execute-api.us-west-2.amazonaws.com/graphQl -d '{"query":"query petDetails {\n  petById(id: \"pet-1\") {\n      id\n      name\n      breed\n      owner {\n          id\n          firstName\n          lastName\n      }\n  }\n}","operationName":"petDetails"}' -H "Content-Type: application/json"

```