# FAQ

## What is PostregSQL needed for?
PostgreSQL is needed to safe temporary data about the running transaction.

## What is RabbitMQ needed for?
RabbitMQ is a message queue. All received data (over REST) will be sent to RabbitMQ.
A secondary process will then consume these messages and create the files for sedex.

## What is Prometheus/Grafana needed for?
Prometheus collects information from the running process and prepares them for Grafana.
Grafana shows the collected information in a dashboard.

They do not necessarily have to be installed, but they are helpful to get some insights in the running system.

## Do I need to backup PostregSQL?
No, because it is only needed for temporary data.

## What do I have to do if the processor crashes during a transaction?
If the processor crashes during a transaction it is best to reset the transaction and start a new one with the same person-data.
