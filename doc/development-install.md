# Installation of LWGS Person Data Processor Service (Development Setup)

This document will describe the setup of a development environment of the lwgs person data processor 
service, and therefore, this is not intended to be used for a productive setup without further
configuration.

## Prerequisites

You should make sure you've installed the following tools:
* docker, docker-compose (and a running docker daemon)
* git or a source tarball

### Maven Setup Github

As the service is using private maven repositories for some of its requirements, it will be
required to add these to the maven configuration.

Add or extend a `~/.m2/settings.xml` with the additional repositories for `banzai` and `lwgs-person-data-processor`.
In oder to access these, you need to have permissions to read the underlying private github repositories. 

Replace the `#USER#` and `#PASS#` placeholders with your github handle and a (encrypted) github
personal access token with permission `read:packages`.

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">

  <activeProfiles>
    <activeProfile>github</activeProfile>
  </activeProfiles>

  <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>https://repo1.maven.org/maven2</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </repository>
        <repository>
          <id>github-banzai</id>
          <name>Banzai Packages</name>
          <url>https://maven.pkg.github.com/datarocks-ag/banzai</url>
        </repository>
        <repository>
          <id>github-lwgs-pdp</id>
          <name>LWGS Person Data Processor Packages</name>
          <url>https://maven.pkg.github.com/datarocks-ag/lwgs-person-data-processor</url>
        </repository>
      </repositories>
    </profile>
  </profiles>

  <servers>
    <server>
      <id>github-banzai</id>
      <username>#USER#</username>
      <password>#PASS#</password>
    </server>
    <server>
      <id>github-lwgs-pdp</id>
      <username>#USER#</username>
      <password>#PASS#</password>
    </server>
 </servers>
</settings>
```

For additional security it is recommended to encrypt the passwords in the `settings.xml` document
as described here: https://maven.apache.org/guides/mini/guide-encryption.html.

## Run Install
In order to run the development version of the service, checkout the code and enter the source folder.
Now use docker-compose to run the system.
```
 # docker-compose -f docker-compose.dev.yml up -d rabbitmq
 # mvn spring-boot:run
```

Now you should be able to access the OpenAPI documentation under http://localhost:8080/swagger-ui.html