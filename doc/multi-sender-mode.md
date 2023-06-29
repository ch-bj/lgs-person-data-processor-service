# Multi Sender Mode

In case you want to use the lgs person data processor service to seed from
multiple sources (land register instances), you can do so by assigning multiple
sedex sender ids (comma separated values) to the SEDEX_SENDER_ID environment variable within the `.env`.

With this the multi-sender mode is active, which has the following implications
/ changes in the default behaviour of the service:

* REST Calls will expect a `X-LGS-Sender-Id` header to be set to identify the 
  source system calling
* In any other state than READY or COMPLETED the full sync is locked to one active 
  source system identified by its sedex sender id exclusively.
* Incremental updates can be performed from multiple source system in parallel.
* With the multi-sender-mode active, the sedex folder structure will be changed as followed 
  (all paths relative to the sedex base directory):
  * `SEDEX_BASE_DIRECTY/outbox` -> `SEDEX_BASE_DIRECTY/<SEDEX_SENDER_ID>/outbox`
  * `SEDEX_BASE_DIRECTY/receipt` -> `SEDEX_BASE_DIRECTY/<SEDEX_SENDER_ID>/receipt`
  
  You'll need to configure multiple sedex clients, one for each sedex sender id.
* Paging of incremental updates is on best effort and will be dependent on the mix of mutation 
  within the queue. Nevertheless, the max page size will be guaranteed, only a minimum fill
  size can't be guaranteed. 
