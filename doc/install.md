# Installation of LWGS Person Data Processor Service

This document will describe the setup of a test environment of the lwgs person data processor 
service, and therefore, this is not intended to be used for a productive setup without further
configuration.

## Prerequisites

You should make sure you've installed the following tools:
* docker, docker-compose (and a running docker daemon)
* git or a source tarball

## Run Install
In order to run the development version of the service, checkout the code and enter the source folder.
Now use docker-compose to run the system.
```
 # docker-compose build
 # docker-compose up
```

Now you should be able to access the OpenAPI documentation under http://localhost:8080/swagger-ui.html