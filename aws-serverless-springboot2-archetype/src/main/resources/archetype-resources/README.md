#set($resourceName = $artifactId)
#macro(replaceChar $originalName, $char)
  #if($originalName.contains($char))
    #set($tokens = $originalName.split($char))
    #set($newResourceName = "")
    #foreach($token in $tokens)
       #set($newResourceName = $newResourceName + $token.substring(0,1).toUpperCase() + $token.substring(1).toLowerCase())
    #end
    ${newResourceName}
  #else
    #set($newResourceName = $originalName.substring(0,1).toUpperCase() + $originalName.substring(1))
    ${newResourceName}
  #end
#end
#set($resourceName = "#replaceChar($resourceName, '-')")
#set($resourceName = "#replaceChar($resourceName, '.')")
#set($resourceName = $resourceName.replaceAll("\n", "").trim())
# \${artifactId} serverless API
The \${artifactId} project, created with [`aws-serverless-java-container`](https://github.com/awslabs/aws-serverless-java-container).

The starter project defines a simple `/ping` resource that can accept `GET` requests with its tests.

The project folder also includes a `template.yml` file. You can use this [SAM](https://github.com/awslabs/serverless-application-model) file to deploy the project to AWS Lambda and Amazon API Gateway or test in local with the [SAM CLI](https://github.com/awslabs/aws-sam-cli). 

#[[##]]# Pre-requisites
* [AWS CLI](https://aws.amazon.com/cli/)
* [SAM CLI](https://github.com/awslabs/aws-sam-cli)
* [Gradle](https://gradle.org/) or [Maven](https://maven.apache.org/)

#[[##]]# Building the project
You can use the SAM CLI to quickly build the project
```bash
$ mvn archetype:generate -DartifactId=\${artifactId} -DarchetypeGroupId=com.amazonaws.serverless.archetypes -DarchetypeArtifactId=aws-serverless-jersey-archetype -DarchetypeVersion=${project.version} -DgroupId=\${groupId} -Dversion=\${version} -Dinteractive=false
$ cd \${artifactId}
$ sam build
Building resource '\${resourceName}Function'
Running JavaGradleWorkflow:GradleBuild
Running JavaGradleWorkflow:CopyArtifacts

Build Succeeded

Built Artifacts  : .aws-sam/build
Built Template   : .aws-sam/build/template.yaml

Commands you can use next
=========================
[*] Invoke Function: sam local invoke
[*] Deploy: sam deploy --guided
```

#[[##]]# Testing locally with the SAM CLI

From the project root folder - where the `template.yml` file is located - start the API with the SAM CLI.

```bash
$ sam local start-api

...
Mounting ${groupId}.StreamLambdaHandler::handleRequest (java11) at http://127.0.0.1:3000/{proxy+} [OPTIONS GET HEAD POST PUT DELETE PATCH]
...
```

Using a new shell, you can send a test ping request to your API:

```bash
$ curl -s http://127.0.0.1:3000/ping | python -m json.tool

{
    "pong": "Hello, World!"
}
``` 

#[[##]]# Deploying to AWS
To deploy the application in your AWS account, you can use the SAM CLI's guided deployment process and follow the instructions on the screen

```
$ sam deploy --guided
```

Once the deployment is completed, the SAM CLI will print out the stack's outputs, including the new application URL. You can use `curl` or a web browser to make a call to the URL

```
...
-------------------------------------------------------------------------------------------------------------
OutputKey-Description                        OutputValue
-------------------------------------------------------------------------------------------------------------
\${resourceName}Api - URL for application            https://xxxxxxxxxx.execute-api.us-west-2.amazonaws.com/Prod/pets
-------------------------------------------------------------------------------------------------------------
```

Copy the `OutputValue` into a browser or use curl to test your first request:

```bash
$ curl -s https://xxxxxxx.execute-api.us-west-2.amazonaws.com/Prod/ping | python -m json.tool

{
    "pong": "Hello, World!"
}
```
