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

### Partial Sync fails with 404
The partial sync may fail with a message like the following:\
`call failed with: 404 - {"timestamp":"1970-01-01T00:00:00.000+00:00","status":404,"error":"Not Found","message":"No message available","path":"/app/lgs/api/v1/sync/partial/person-data"}`

This may indicate that:
- No data could be sent to RabbitMQ
- The backend-service ([lgs-person-data-processor](https://github.com/ch-bj/lgs-person-data-processor)) could not fetch RabbitMQ data
- No new data was available since the last sync

The first two points may indicate network problems between the applications or additionally implemented security measures like ambassador proxies, CGROUP limitations or service meshes.\
Please make sure that all involved applications can see and reach their destination interface through the network, and that data is received on the other end.

The third point usually indicates that no data was sent since the last sync and the RabbitMQ queue was empty. This can be verified by ensuring in the logs, that some of the syncs succeed.\
This may happen when the sync frequency of the [sync options](../README.md) is too high. In these cases, the message can be safely ignored. If the error occurs too often, relaxing the sync frequency is recommended. One daily sync is usually a good frequency.

## How many resources are needed to run the processor?
The needed resources depend on the count of mutations to transfer.
To transfer one million mutations you need minimal:

- 4 (V-)CPUs
- 10 GB RAM
- 15 GB Disk Space (only temporarly until the Sedex-Message is sent)
