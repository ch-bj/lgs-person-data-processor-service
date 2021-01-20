curl -v -d "@natural-person.json" -H "Authorization: token test-key" -H "Content-Type: application/json" -X POST http://localhost:8080/api/v1/sync/full/person-data/
echo $?
