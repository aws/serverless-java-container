In this sample, you'll build a native GraalVM image for running web workloads in AWS Lambda.


## To build the sample

You first need to build the function, then you will deploy it to AWS Lambda.

Please note that the sample is for `x86` architectures. In case you want to build and run it on ARM, e.g. Apple Mac M1, M2, ... 
you must change the according line in the `Dockerfile` to `ENV ARCHITECTURE aarch64`. 
In addition, uncomment the `arm64` Architectures section in `template.yml`.

### Step 1 - Build the native image

Before starting the build, you must clone or download the code in **pet-store-native**.

1. Change into the project directory: `samples/springboot3/pet-store-native`
2. Run the following to build a Docker container image which will include all the necessary dependencies to build the application 
   ```
   docker build -t al2023-graalvm21:native-web .
   ```
3. Build the application within the previously created build image
   ```
   docker run -it -v `pwd`:`pwd` -w `pwd` -v ~/.m2:/root/.m2 al2023-graalvm21:native-web ./mvnw clean -Pnative package -DskipTests
   ```
4. After the build finishes, you need to deploy the function:
 ```
   sam deploy --guided
 ```

This will deploy your application and will attach an AWS API Gateway
Once the deployment is finished you should see the following:
```
Key                  ServerlessWebNativeApi
Description          URL for application
Value                https://xxxxxxxx.execute-api.us-east-2.amazonaws.com/pets 
```

You can now simply execute GET on this URL and see the listing fo all pets. 
