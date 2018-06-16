# autocomplete-endpoint
I will complete you

### Build

1. Assembly the populator: `sbt populator/assembly`
2. Publish docker image: `sbt api/docker:publishLocal`

### How to run

`docker-compose up -d`
1. Index ES: `docker exec -d $(docker-compose ps -q redis) java -jar /populator/populator-assembly-0.0.2.jar -f -h -p -n -t`
Query the service: ```
curl -XPOST "http://localhost:8088/autocomplete" -d'
    {
    "queryText": "senior",
    "numResults": 10
    }'
```