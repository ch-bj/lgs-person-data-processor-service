# Installation of LWGS Person Data Processor Service (Test Setup)

This document will describe the setup of a test environment of the lwgs person data processor
service, and therefore, this is not intended to be used for a productive setup without further
configuration.

## Prerequisites

You should make sure you've installed the following tools:
* docker, docker-compose (and a running docker daemon)
* git, github account with access to the `lwgs-person-data-processor-service`

## Preparation

Login into githubs docker repository (replacing `#USER#` with your github handle)

```
 # docker login ghcr.io -u #USER#
```

Checkout the project

```
 # git clone https://github.com/datarocks-ag/lwgs-person-data-processor-service.git
```

Create a copy of `.env` from the example file

```
 # cp example.env .env
```

Update credentials, sedex settings, etc. (make sure you're not using these example values in a
public accessible setting / in production).

```
RABBITMQ_USER=lwgs
RABBITMQ_PASSWORD=oor2ith5yae3ahc1eesequ0ho1die8Zo
REST_API_KEY=eidushufooxeeraeraePh1japeu6ahn9Atei0shu2o
SEDEX_SENDER_ID=123
SEDEX_RECIPIENT_ID=321
SEDEX_MESSAGE_TYPE=42
SEDEX_MESSAGE_CLASS=23
```

## Run Service

Simply run `docker-compose up` within the root of the project.

```
 # docker-compose up -d
 [+] Running 12/12
 ⠿ Network lwgs-person-data-processor-service_default         Created    0.1s
 ⠿ Network lwgs-person-data-processor-service_internal        Created    0.1s
 ⠿ Volume "lwgs-person-data-processor-service_rabbit-logs"    Created    0.0s
 ⠿ Volume "lwgs-person-data-processor-service_grafana-data"   Created    0.0s
 ⠿ Volume "lwgs-person-data-processor-service_log-data"       Created    0.0s
 ⠿ Volume "lwgs-person-data-processor-service_hsqldb-data"    Created    0.0s
 ⠿ Volume "lwgs-person-data-processor-service_sedex"          Created    0.0s
 ⠿ Volume "lwgs-person-data-processor-service_rabbit-data"    Created    0.0s
 ⠿ Container lwgs-person-data-processor-service-rabbitmq-1    Started    2.1s
 ⠿ Container lwgs-person-data-processor-service-backend-1     Started    2.0s
 ⠿ Container lwgs-person-data-processor-service-prometheus-1  Started    2.3s
 ⠿ Container lwgs-person-data-processor-service-grafana-1     Started    3.0s
```

The generated sedex files will be placed within the `lwgs-person-data-processor-service_sedex` 
volume.

You might want to change this to a bindmount within the `docker-compose.yml` file while
testing:
```diff
-      - sedex:/var/sedex
+      - /tmp/sedex:/var/sedex
```

## Provided Services

After docker is started you'll find the following services running:
* LWGS Person Data Processor Service API

  API Token as defined within the `.env`

  http://localhost:8080/api
* Swagger UI (API Documentation)

  http://localhost:8080/swagger-ui.html
* Grafana Metric Dashboards

  Initial password: admin/admin

  http://localhost:8081/d/HE81hll7k/lwgs-dashboard?orgId=1&from=now-3h&to=now&refresh=5s
* RabbitMQ Management Console

  Credentials as defined within the `.env`

  http://localhost:8082/
