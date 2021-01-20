# LWGS Person Data Processor Service

Reference implementation of wrapping the lwgs-person-data-processor library. Service allows to process and seed
personData to the central search index, part of Landesweite Grundstückssuche (LWGS). Data is consumed via
a RESTfull interface, and passed on through Sedex after successful processing. The service allows either full or 
partial syncs.

## System Architecture
![Full Sync Seed States](doc/images/lwgs-person-data-processor-service.png)


## Getting Started

### Full Sync Flow
In case of a full sync the services allows the seeding party to explicitly enable, fill and submit a seeding job.
The following diagram shows the states of the seeding process in the full sync scenario.

![Full Sync Seed States](doc/images/full-sync-seed-states.png)

#### Process
*  Set up a new full sync job with a call to the start
   ```sh
   curl --request PUT \
   --url http://<HOST>/api/v1/sync/full/trigger/start \
   --header 'Authorization: token <API_KEY>' \
   --header 'Content-Type: application/json'
   ```
   In return you'll get the following JSON confirming the new state and the ID of the newly created sync job.
   ```json
   {
      "jobId": "b2630b7d-d8ec-4ffc-9e16-fc9415a972e7",
      "seedStatus": "SEEDING"
   }
   ```
*  In the state `SEEDING` you'll now send all person data which should be transmitted to the central index.
   ```sh
   curl --request POST \
   --url http://<HOST>/api/v1/sync/full/person-data \
   --header 'Authorization: token <API_KEY>' \
   --header 'Content-Type: application/json'
   --data '{
   "metaData": {
   "personType": "NATUERLICHE_PERSON",
   "eventType": "INSERT"
   },
   "natuerlichePerson": {
   "egpId": "egpId",
   "name": "Smith",
   "vorname": "John",
   "jahrgang": "1970"
   }
   }'
   ```
   Each mutation will be confirmed with a transactionId.
   ```json
   {
      "transactionId": "d19b0584-5c4f-4686-8b37-f618e6a90ccb"
   }
   ```
*  After all data is sent to the job, it can be submitted for further processing.
   ```sh
   curl --request PUT \
   --url http://<HOST>/api/v1/sync/full/trigger/submit \
   --header 'Authorization: token <API_KEY>' \
   --header 'Content-Type: application/json'
   ```
   In return you'll get the following JSON confirming the new state and the ID of the newly created sync job.
   ```json
   {
      "jobId": "b2630b7d-d8ec-4ffc-9e16-fc9415a972e7",
      "seedStatus": "SENDING"
   }
   ```
* After setting the state to `SENDING` and processing all data, the aggregated, processed person data will then be 
  wrapped in a Sedex message and written to the filesystem with the next run of the `FullSyncService`.


### Partial Sync Flow

#### Process
*  For all mutations you want to propagate to the central index, you directly seed the person data which should be 
   transmitted.
   ```sh
   curl --request POST \
   --url http://<HOST>/api/v1/sync/partial/person-data \
   --header 'Authorization: token <API_KEY>' \
   --header 'Content-Type: application/json'
   --data '{
   "metaData": {
   "personType": "NATUERLICHE_PERSON",
   "eventType": "INSERT"
   },
   "natuerlichePerson": {
   "egpId": "egpId",
   "name": "Smith",
   "vorname": "John",
   "jahrgang": "1970"
   }
   }'
   ```
   Each mutation will be confirmed with a transactionId.
   ```json
   {
      "transactionId": "d19b0584-5c4f-4686-8b37-f618e6a90ccb"
   }
   ```
* The next time the configured implementation of the `PartialSyncService` will run, the processed seeds will be
  aggregated and written to a Sedex message.


### Sedex Integration

## Components Description

### SedexFileWriterService

The SedexFileWriter service will receive the JobCollectedPersonData objects from the
lwgs.sedex.outbox and create a zip file as Sedex payload. Each zip file will contain `[1..n]` zip
entries of GBPersonEvents.

#### Relevant configuration parameters

<dl>
    <dt><strong>lwgs.searchindex.client.sedex.base-path</strong></dt>
        <dd>Sedex base path. Default is /tmp/sedex</dd>
    <dt><strong>lwgs.searchindex.client.sedex.receipt-path</strong></dt>
        <dd>Sedex receipt path. Default is receipts</dd>
    <dt><strong>lwgs.searchindex.client.sedex.outbox-path</strong></dt>
        <dd>Sedex outbox path. Default is outbox</dd>
    <dt><strong>lwgs.searchindex.client.sedex.create-directories</strong></dt>
        <dd>Controls automatic creation of Sedex directories. Default is false.</dd>
    <dt><strong>lwgs.searchindex.client.sedex.sender-id</strong></dt>
        <dd>Configures the senderId of the outgoing sedex message.</dd>
    <dt><strong>lwgs.searchindex.client.sedex.recipient-id</strong></dt>
        <dd>Configures the recipientId of the outgoing sedex message.</dd>
    <dt><strong>lwgs.searchindex.client.sedex.message.type</strong></dt>
        <dd>Configures the messageType of the outgoing sedex message.</dd>
    <dt><strong>lwgs.searchindex.client.sedex.message.class</strong></dt>
        <dd>Configures the messageClass of the outgoing sedex message.</dd>
    <dt><strong>lwgs.searchindex.client.sedex.file-writer.failure.throttling.base</strong></dt>
        <dd>Base for throttling interval in case of errors. Throttling interval doubles on each failed retry (until _max_ is reached). Default is set to 1'000 ms.</dd>
    <dt><strong>lwgs.searchindex.client.sedex.file-writer.failure.throttling.max</strong></dt>
        <dd>Controls max throttling interval in ms. Default is set to 3'600'000 ms (1h).</dd>
    <dt><strong>lwgs.searchindex.client.sedex.file-writer.fixed-delay</strong></dt>
        <dd>Execution interval of the SedexFileWriterService in ms. Default is set to 1'000 ms.</dd>
</dl>

#### Error Handling and resolution

If the sedex/outbox folder is not accessible or in case of any other IOException the SedexFileWriter
service will requeue the message and continue processing.

In cases of an exception the SedexFileWriter service will throttle processing until it successfully
processed a message.

Throttling can be configured by the following configuration keys:

`lwgs.searchindex.client.sedex.file-writer.failure.throttling.base` (default: 1000 ms)

`lwgs.searchindex.client.sedex.file-writer.failure.throttling.max` (default: 3600000 ms)

The throttling interval is calculated as following:

`waitingTime = min(2^retryCount * throttlingBase, throttlingMax)`

The issues need to be resolved manually by the administrator.

##### Proposed error resolution activities

- Check if Sedex outbox exists
- Check if Sedex outbox is accessible for search-index-client-service
- Check if enough disk space is available for volume containing Sedex outbox
- Check user quotas for volume

