#!/bin/bash
VALID_DATA_PATH=./person-data/valid/*
for FILE in $VALID_DATA_PATH
do
  RESPONSE=$(curl -v -d"$FILE" -H "Authorization: token test-key" -H "Content-Type: application/json" -X POST http://localhost:8080/api/v1/sync/partial 2>/dev/null)
  echo -e "Processing $FILE\t$RESPONSE"
  if [ $? -ne 0 ]; then
    echo "Rest returned failure"
    exit
  fi
  echo $RESPONSE
done
