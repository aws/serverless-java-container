# Quarkus Native Pet store example  

The [Quarkus framework](https://quarkus.io/) is compatible with Spring's annotations and makes it easy to use [GraalVM](https://www.graalvm.org/) to build application images into native binaries. Further, Micronaut includes builtin support for AWS Lambda.

This demo application shows how to use Quarkus to compile our standard pet store example, using Spring annotations, into a native binary with GraalVM and execute it in AWS Lambda. To run this demo, you will need to have [Maven](https://maven.apache.org/) installed as well as [Docker](https://www.docker.com/) to build GraalVM native image.

With all the pre-requisites installed including:

* JDK 8 or above
* Maven 3.5.x
 
You should be able to build a native image of the application by running mvn from the repository's root.

```bash
$ mvn clean install -Pnative
```

The output of the build is a deployable zip called `function.zip` in the `target` folder.

To run the lambda locally, you can utilize the SAM cli. This should start up the listeners in the `PetsController`, and you can test locally with your preferred http client.

```bash
sam local start-api -t sam.native.yaml
```

For example, to test the GET /pets endpoint via curl:
```bash
curl localhost:3000/pets
```

You should see JSON output of pets.

To deploy the application to AWS Lambda you can use the pre-configured `sam-native.yaml` file included in the repo. Using the AWS or SAM CLI, run the following commands:

```bash
sam deploy -g -t sam.native.yaml
```

You should see the stack deployed successfully:

```bash
Stack quarkus-sample-pet-store outputs:
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
OutputKey-Description                                                                                              OutputValue                                                                                                      
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
PetStoreNativeApi - URL for application                                                                            https://xxxxxxxxxx.execute-api.xx-xxxx-1.amazonaws.com/Prod/                                                     
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

Successfully created/updated stack - quarkus-sample-pet-store in xx-xxxx-1

```

Make a test request to the API endpoint using curl or your preferred http client. 

For example, to check the GET /pets endpoint via curl:
```bash
curl https://xxxxxxxxxx.execute-api.xx-xxxx-1.amazonaws.com/Prod/pets
```
