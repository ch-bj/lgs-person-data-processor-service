# Multi Sender Mode

In case you want to use the LGS person data processor service to seed from multiple sources (land register instances).

When the multi-sender mode is active, this has the following implications / changes in the default behaviour of the service:
* REST calls will expect a `X-LGS-Sender-Id` header to be set to identify the source system calling.
* In any state other than `READY` or `COMPLETED`, the full sync is locked to one active source system identified by its `X-LGS-Sender-Id` exclusively.
* Incremental updates from multiple source systems can be performed in parallel.
* With the multi-sender-mode active, the sedex folder structure will be changed as follows (all paths relative to the sedex base directory):
  * `SEDEX_BASE_DIRECTORY/outbox` -> `SEDEX_BASE_DIRECTORY/<SEDEX_SENDER_ID>/outbox`
  * `SEDEX_BASE_DIRECTORY/receipt` -> `SEDEX_BASE_DIRECTORY/<SEDEX_SENDER_ID>/receipt`
* Paging of incremental updates is on best effort and will be dependent on the mix of mutations within the queue. Nevertheless, the max page size will be guaranteed, only a minimum fill size can't be guaranteed. 

When the single-sender mode is active this had the implementation:
* REST call may have a `X-LGS-Sender-Id` header to identify the land register. The source system calling is the single register and it is set through the `SEDEX_SENDER_ID` environment variable.
* In any state other than `READY` or `COMPLETED`, the full sync is locked to the one active source system identified by the `SEDEX_SENDER_ID` environment variable.
* Incremental updates from multiple land registers can be performed in parallel.
* With the single-sender-mode active, the sedex folder structure will be changed as follows (all paths relative to the sedex base directory):
  * `SEDEX_BASE_DIRECTORY/outbox` -> the directory when no Land Register(`X-LGS-Sender-Id` header) is specified
  * `SEDEX_BASE_DIRECTOR/outbox/<LAND_REGISTER>` -> the directory when Land Register(`X-LGS-Sender-Id` header) is specified
  * `SEDEX_BASE_DIRECTORY/receipt` -> remains the same
* Paging of incremental updates is on best effort and will be dependent on the mix of mutations within the queue. Nevertheless, the max page size will be guaranteed, only a minimum fill size can't be guaranteed.

You can choose between 2 modes:
* Mode 1: multiple sedex sender ids with one sedex client per sender id.
* Mode 2: a single sedex sender id with a single sedex client and "virtual land registers". This mode can significantly reduce the sedex costs.

Mode 1: 
* You assign multiple sedex sender ids (comma separated values) to the `SEDEX_SENDER_ID` environment variable within the `.env`.
* You configure multiple sedex clients, one for each sedex sender id.
* The `X-LGS-Sender-Id` must contain one of the sender ids in the list.
* The `X-LGS-Sender-Id` is the sender id.

Mode 2: 
* You assign a single sedex sender id to the `SEDEX_SENDER_ID` environment variable within the `.env`.
* You configure one sedex client for this sedex sender id.
* The `X-LGS-Sender-Id` contains a string identifying the source land register. It is recommended to send the actual name of the land register, e.g. "Kreuzlingen" for easy identification and troubleshooting. This **must** be the same name that is used to register the GBDBS URL with LGS.
* The `X-LGS-Sender-Id` is the land register and the sender id is the one specified in `SEDEX_SENDER_ID` environment variable.