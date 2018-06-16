# autocomplete-endpoint
I will complete you

### Build

1. Assemble the populator: `sbt populator/assembly`
2. Publish docker image: `sbt api/docker:publishLocal`

### Prerequisites

0. Make sure you can run elasticsearch [docker image](https://www.elastic.co/guide/en/elasticsearch/reference/6.2/docker.html). _The `vm.max_map_count` setting should be set permanently in `/etc/sysctl.conf`:_
```
$ grep vm.max_map_count /etc/sysctl.conf
vm.max_map_count=262144
```

### How to run

1. `docker-compose up -d`
2. Index ES: `docker exec -d $(docker-compose ps -q autocomplete) java -jar /populator/populator-assembly-0.0.2.jar --inputFile /populator/resources/famouspeople.txt --host elasticsearch --port 9200 --indexName $ES_INDEX_NAME --indexType $ES_INDEX_TYPE`. Check [example file](https://github.com/anuras/autocomplete-service/blob/master/populator/src/test/resources/famouspeople.txt) for reference on the file format (`<text><whitespace><weight>`).
3. Check ES is populated: `curl -X GET "localhost:9200/titles/_search"` or `curl -X GET "localhost:9200/titles/job/_count"`
4. Query the service: 
```
curl -XPOST "http://localhost:8088/autocomplete" -d'
    {
    "queryText": "mar",
    "numResults": 5
    }'
```
Should respond with suggestions:
```
{"queryText":"mar","suggestions":["maria gomes valentim","margaret skeete","maria do couto maia-lopes","maria giuseppa robucci","marie josephine gaudette"],"errorMessage":null}```