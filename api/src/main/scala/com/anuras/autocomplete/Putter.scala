package com.anuras.autocomplete

import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest
import org.elasticsearch.client.Client
import org.elasticsearch.common.xcontent.XContentType

//TODO
//uses java elastic client, switch out elastic4s
//use built in weights in elastic suggester
object Putter {
  val phrase = "Joe Surname"
  val suggestionPhrase = "Surname Joe"
  val popularity = 13
  val indexName = "peoples"
  val indexType = "persons"

  val client: Client = ???

  val content =
    s"""
       |{
       |   "text": "$phrase",
       |   "suggestion" : {
       |       "input": "$suggestionPhrase",
       |       "weight" : $popularity
       |   }
       |}
     """.stripMargin

  val suggestionMapping =
    s"""
       |{
       |       "properties": {
       |           "suggestion" : {
       |               "type": "completion"
       |           },
       |           "title": {
       |               "type": "keyword"
       |           }
       |       }
       |}
    """.stripMargin

  val mappingRequest = new PutMappingRequest(indexName).`type`(indexType).source(suggestionMapping, XContentType.JSON)
  val indexResponse = client.admin().indices().prepareCreate(indexName).execute().actionGet()
  val mappingResponse =  client.admin().indices().putMapping(mappingRequest).actionGet()

  val response = client.prepareIndex(indexName, indexType).setSource(content, XContentType.JSON).get()

}