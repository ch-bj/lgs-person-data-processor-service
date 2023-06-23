# FAQ


## Which person-data should be delivered to the index?
The Grundbuchperson (not the Stammperson) should be delivered to the index (full sync and partial sync).


## What is PostregSQL needed for?
PostgreSQL is needed to save temporary data about the running transaction.


## What is RabbitMQ needed for?
RabbitMQ is a message queue. All received data (over REST) will be sent to RabbitMQ.
A secondary process will then consume these messages and create the files for sedex.


## What is Prometheus/Grafana needed for?
Prometheus collects information from the running process and prepares them for Grafana.
Grafana shows the collected information in a dashboard.

These componentes are optional. They do not necessarily have to be installed, but they are helpful to get some insights in the running system.


## Do I need to backup PostregSQL?
No, because it is only needed for temporary data.


## What do I have to do if the processor crashed?
You have to do the following steps:

- Check the logs to find the causing error
- Fix the error
- Restart the processor

The processor should recover automatically.


## What do I have to do if a full sync failed?
The status of the full syncs is available over REST and visible in the Grafana-Dashboard.
A full sync fails when one of the person-data wasn't correct.

You have to do the following steps:

- Fix the wrong person-data
- Reset the transaction (it deletes the already received data)
- Send the full-data again


## What do I have to do if a partial sync failed?
The status of the partial syncs is available over REST and visible in the Grafana-Dashboard.
A partial sync fails when the person-data wasn't correct.

You have to do the following steps:

- Fix the person-data
- Send the partial-data again


## How many resources are needed to run the processor?
The needed resources depend on the count of mutations to transfer.
To transfer one million mutations you need minimal:

- 4 (V-)CPUs
- 10 GB RAM
- 15 GB Disk Space (only temporarly until the Sedex-Message is sent)
