FROM amazonlinux:2017.03.1.20170812 as graalvm
# install graal to amazon linux.
ENV LANG=en_US.UTF-8

RUN yum install -y gcc gcc-c++ libc6-dev  zlib1g-dev curl bash zlib zlib-devel zip  \
    && rm -rf /var/cache/yum

# Install JDK8 with backported JVMCI
ENV JDK_FILE openjdk-8u242-jvmci-20.1-b01-linux-amd64
ENV JDK_DIR openjdk1.8.0_242-jvmci-20.1-b01
RUN curl -4 -L https://github.com/graalvm/openjdk8-jvmci-builder/releases/download/jvmci-20.1-b01/${JDK_FILE}.tar.gz -o /tmp/jdk.tar.gz
RUN tar -zxvf /tmp/jdk.tar.gz -C /tmp \
    && mv /tmp/${JDK_DIR} /usr/lib/jdk
ENV JAVA_HOME /usr/lib/jdk

# Download and install GraalVM 20.0.0 + native image
ENV GRAAL_VERSION 20.0.0
ENV GRAAL_FILENAME graalvm-ce-java8-linux-amd64-${GRAAL_VERSION}.tar.gz
RUN curl -4 -L https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-${GRAAL_VERSION}/${GRAAL_FILENAME} -o /tmp/${GRAAL_FILENAME}
RUN tar -zxvf /tmp/${GRAAL_FILENAME} -C /tmp \
    && mv /tmp/graalvm-ce-java8-${GRAAL_VERSION} /usr/lib/graalvm
RUN /usr/lib/graalvm/bin/gu install native-image

# Download and install maven
ENV MAVEN_VERSION 3.6.3
RUN curl -4 -L http://apache.forsale.plus/maven/maven-3/3.6.3/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -o /tmp/maven.tar.gz
RUN tar -zxvf /tmp/maven.tar.gz -C /tmp \
    && mv /tmp/apache-maven-${MAVEN_VERSION} /usr/lib/maven

RUN rm -rf /tmp/*

ENV PATH /usr/lib/graalvm/bin:/usr/lib/maven/bin:${PATH}

COPY . /usr/lib/sjc
RUN cd /usr/lib/sjc && mvn install -DskipTests -Djacoco.minCoverage=0.1

VOLUME ["/func"]
WORKDIR /func
ENTRYPOINT ["/func/scripts/graalvm-build.sh"]