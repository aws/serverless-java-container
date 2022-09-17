# Serverless Struts example
A basic pet store written with the [Apache Struts framework](https://struts.apache.org). The `StrutsLambdaHandler` object provided by the `aws-serverless-java-container-struts` is the main entry point for Lambda.

The application can be deployed in an AWS account using the [Serverless Application Model](https://github.com/awslabs/serverless-application-model). The `template.yml` file in the root folder contains the application definition

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

Once the deployment is completed, the SAM CLI will print out the stack's outputs, including the new application URL. You can use `curl` or a web browser to make a call to the URL

```
...
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
Outputs                                                                                                                                                                                                    
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
Key                 StrutsPetStoreApi                                                                                                                                                                      
Description         URL for application                                                                                                                                                                    
Value               https://n60c1ycwa2.execute-api.eu-central-1.amazonaws.com/pets                                                                                                                         
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
```
## Test

### JSON Request:
```
$ curl https://xxxxxxxxxx.execute-api.us-west-2.amazonaws.com/pets.json
```

### XML Request
```
$ curl https://xxxxxxxxxx.execute-api.us-west-2.amazonaws.com/pets.xml
```