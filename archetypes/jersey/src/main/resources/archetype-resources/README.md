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
# ${artifactId} serverless API
The ${artifactId} project, created with [`aws-serverless-java-container`](https://github.com/awslabs/aws-serverless-java-container).

The starter project defines a simple `/ping` resource that can accept `GET` requests with its tests.

The project folder also includes a `sam.yaml` file. You can use this [SAM](https://github.com/awslabs/serverless-application-model) file to deploy the project to AWS Lambda and Amazon API Gateway or test in local with [SAM Local](https://github.com/awslabs/aws-sam-local). 

## Building the project
Using [Maven](https://maven.apache.org/), you can create an AWS Lambda-compatible jar file simply by running the maven package command from the projct folder.

```bash
$ mvn archetype:generate -DartifactId=my-jersey-api -DarchetypeGroupId=com.amazonaws.serverless.archetypes -DarchetypeArtifactId=aws-serverless-jersey-archetype -DarchetypeVersion=1.0-SNAPSHOT -DgroupId=com.sapessi.jersey -Dversion=0.1 -Dinteractive=false
$ cd my-jersey-api
$ mvn clean package

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 6.546 s
[INFO] Finished at: 2018-02-15T08:39:33-08:00
[INFO] Final Memory: XXM/XXXM
[INFO] ------------------------------------------------------------------------
```

## Testing locally with SAM local
You can use [AWS SAM Local](https://github.com/awslabs/aws-sam-local) to start your project.

First, install SAM local:

```bash
$ npm install -g aws-sam-local
```

Next, from the project root folder - where the `sam.yaml` file is located - start the API with the SAM Local CLI.

```bash
$ sam local start-api --template sam.yaml

...
Mounting ${groupId}.StreamLambdaHandler::handleRequest (java8) at http://127.0.0.1:3000/{proxy+} [OPTIONS GET HEAD POST PUT DELETE PATCH]
...
```

Using a new shell, you can send a test ping request to your API:

```bash
$ curl -s http://127.0.0.1:3000/ping | python -m json.tool

{
    "pong": "Hello, World!"
}
``` 

## Deploying to AWS
You can use the [AWS CLI](https://aws.amazon.com/cli/) to quickly deploy your application to AWS Lambda and Amazon API Gateway with your SAM template.

You will need an S3 bucket to store the artifacts for deployment. Once you have created the S3 bucket, run the following command from the project's root folder - where the `sam.yaml` file is located:

```
$ aws cloudformation package --template-file sam.yaml --output-template-file output-sam.yaml --s3-bucket <YOUR S3 BUCKET NAME>
Uploading to xxxxxxxxxxxxxxxxxxxxxxxxxx  6464692 / 6464692.0  (100.00%)
Successfully packaged artifacts and wrote output template to file output-sam.yaml.
Execute the following command to deploy the packaged template
aws cloudformation deploy --template-file /your/path/output-sam.yaml --stack-name <YOUR STACK NAME>
```

As the command output suggests, you can now use the cli to deploy the application. Choose a stack name and run the `aws cloudformation deploy` command from the output of the package command.
 
```
$ aws cloudformation deploy --template-file output-sam.yaml --stack-name ServerlessJerseyApi --capabilities CAPABILITY_IAM
```

Once the application is deployed, you can describe the stack to show the API endpoint that was created. The endpoint should be the `ServerlessJerseyApi` key of the `Outputs` property:

```
$ aws cloudformation describe-stacks --stack-name ServerlessJerseyApi
{
    "Stacks": [
        {
            "StackId": "arn:aws:cloudformation:us-west-2:xxxxxxxx:stack/ServerlessJerseyApi/xxxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxx", 
            "Description": "AWS Serverless Jersey API - ${groupId}::${artifactId}", 
            "Tags": [], 
            "Outputs": [
                {
                    "Description": "URL for application",
                    "ExportName": "${resourceName}Api",  
                    "OutputKey": "${resourceName}Api",
                    "OutputValue": "https://xxxxxxx.execute-api.us-west-2.amazonaws.com/Prod/ping"
                }
            ], 
            "CreationTime": "2016-12-13T22:59:31.552Z", 
            "Capabilities": [
                "CAPABILITY_IAM"
            ], 
            "StackName": "ServerlessJerseyApi", 
            "NotificationARNs": [], 
            "StackStatus": "UPDATE_COMPLETE"
        }
    ]
}

```

Copy the `OutputValue` into a browser or use curl to test your first request:

```bash
$ curl -s https://xxxxxxx.execute-api.us-west-2.amazonaws.com/Prod/ping | python -m json.tool

{
    "pong": "Hello, World!"
}
```