curl -v -H "Authorization: token test-key" -H "Content-Type: application/json" -X GET http://localhost:8080/api/v1/sync/jobs
curl -v -H "Authorization: token test-key" -H "Content-Type: application/json" -X GET http://localhost:8080/api/v1/sync/transactions
echo $?
