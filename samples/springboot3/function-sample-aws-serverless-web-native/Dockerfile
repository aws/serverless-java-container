FROM arm64v8/amazonlinux:2

RUN yum -y update \
    && yum install -y tar unzip gzip bzip2-devel ed gcc gcc-c++ gcc-gfortran \
    less libcurl-devel openssl openssl-devel readline-devel xz-devel \
    zlib-devel glibc-static libcxx libcxx-devel llvm-toolset-7 zlib-static \
    && rm -rf /var/cache/yum

#ENV GRAAL_VERSION 22.3.1
ENV GRAAL_FOLDERNAME graalvm-community-openjdk-21.0.1+12.1
#ENV GRAAL_FILENAME graalvm-community-jdk-21.0.1_linux-x64_bin.tar.gz

RUN curl -4 -L https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-21.0.1/graalvm-community-jdk-21.0.1_linux-aarch64_bin.tar.gz | tar -xvz
RUN mv $GRAAL_FOLDERNAME /usr/lib/graalvm
RUN rm -rf $GRAAL_FOLDERNAME

# Graal maven plugin requires Maven 3.3.x
ENV MVN_VERSION 3.6.3
ENV MVN_FOLDERNAME apache-maven-${MVN_VERSION}
ENV MVN_FILENAME apache-maven-${MVN_VERSION}-bin.tar.gz
RUN curl -4 -L https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MVN_VERSION}/${MVN_FILENAME} | tar -xvz
RUN mv $MVN_FOLDERNAME /usr/lib/maven
RUN rm -rf $MVN_FOLDERNAME

# Gradle
ENV GRADLE_VERSION 7.4.1
ENV GRADLE_FOLDERNAME gradle-${GRADLE_VERSION}
ENV GRADLE_FILENAME gradle-${GRADLE_VERSION}-bin.zip
RUN curl -LO https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip	
RUN unzip gradle-${GRADLE_VERSION}-bin.zip
RUN mv $GRADLE_FOLDERNAME /usr/lib/gradle
RUN rm -rf $GRADLE_FOLDERNAME

VOLUME /project
WORKDIR /project



#RUN /usr/lib/graalvm/bin/gu install native-image
#RUN ln -s /usr/lib/graalvm/bin/native-image /usr/bin/native-image
RUN ln -s /usr/lib/maven/bin/mvn /usr/bin/mvn
RUN ln -s /usr/lib/gradle/bin/gradle /usr/bin/gradle

ENV JAVA_HOME /usr/lib/graalvm

WORKDIR /function-sample-aws-native
