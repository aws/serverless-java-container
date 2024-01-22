In this sample, you'll build a native GraalVM image for running web workloads in AWS Lambda.


## To build the sample on macOS (Apple silicon arm64)

You first need to build the function, then you will deploy it to AWS Lambda.

### Step 1 - Build the native image

Before starting the build, you must clone or download the code in **function-sample-aws-native**.

1. Change into the project directory: `samples/springboot3/function-sample-aws-serverless-web-native`
2. Run the following to build a Docker container image which will be used to create the Lambda function zip file. 
   ```
   docker build -t "al2-graalvm21:native-function" .
   ```
3. Start the container
   ```
   docker run -dit -v `pwd`:`pwd` -w `pwd` -v ~/.m2:/root/.m2 al2-graalvm21:native-function
   ```
4. In Docker, open the image terminal. 

   > Your working directory should default to the project root. Verify by running `ls` to view the files.

6. From inside the container, build the Lambda function:
   ```
   ./mvnw clean -Pnative native:compile -DskipTests
   ```

After the build finishes, you need to deploy the function.
You can do it manually or you can use SAM (AWS Serverless Application Model) with the included template.yaml file.
If you chose SAM simply execute the following command.
 ```
   sam depploy --guided
 ```
 This will deploy your application and will attach an AWS API Gateway
Once the deployment is finished you shouild see the following:
```
Key                  ServerlessWebNativeApi
Description          URL for application
Value                https://xxxxxxxx.execute-api.us-east-2.amazonaws.com/pets 
```

You can now simply execute GET on this URL and see the listing fo all pets. 
