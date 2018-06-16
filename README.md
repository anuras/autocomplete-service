# autocomplete-endpoint
I will complete you

### Build

1. Assembly the populator: `sbt populator/assembly`
2. Publish docker image: `sbt api/docker:publishLocal`

### How to run

0. Make sure you can run elasticsearch [docker image](https://www.elastic.co/guide/en/elasticsearch/reference/6.2/docker.html). _The `vm.max_map_count` setting should be set permanently in `/etc/sysctl.conf`:_
```
$ grep vm.max_map_count /etc/sysctl.conf
vm.max_map_count=262144
```
1. `docker-compose up -d`
2. Index ES: `docker exec -d $(docker-compose ps -q autocomplete) java -jar /populator/populator-assembly-0.0.2.jar --inputFile /populator/resources/famouspeople.txt --host elasticsearch --port 9200 --indexName $ES_INDEX_NAME --indexType $ES_INDEX_TYPE`
3. Check ES is populated: `curl -X GET "localhost:9200/titles/_search"` or `curl -X GET "localhost:9200/titles/job/_count"`
4. Query the service: ```
curl -XPOST "http://localhost:8088/autocomplete" -d'
    {
    "queryText": "mar",
    "numResults": 5
    }'
```