package com.anuras.autocomplete

import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient

case class PopulateConfig(
  inputFile: String = "",
  elasticHostname: String = "localhost",
  elasticPort: Int = 9200,
  indexName: String = "catalog",
  indexType: String = "document"
)

object Populate extends App {

  val parser = new scopt.OptionParser[PopulateConfig]("scopt") {
    head("scopt", "3.x")

    opt[String]('f', "inputFile").required().valueName("<inputFile>")
      .action( (x, c) => c.copy(inputFile = x) ).text("Input file")

    opt[String]('h', "host").required().valueName("<host>").
      action( (x, c) => c.copy(elasticHostname = x) ).
      text("Name of Elasticsearch host")

    opt[Int]('p', "port").required().valueName("<port>").
      action( (x, c) => c.copy(elasticPort = x) ).
      text("Elasticsearch port")

    opt[String]('n', "indexName").required().valueName("<indexName>").
      action( (x, c) => c.copy(indexName = x) ).
      text("Index Name in Elasticsearch")

    opt[String]('t', "indexType").required().valueName("<indexType>").
      action( (x, c) => c.copy(indexType = x) ).
      text("Index Type in Elasticsearch")

  }


  parser.parse(args, PopulateConfig()) match {
    case Some(config) => {

      assert(config.indexName == config.indexName.toLowerCase)
      assert(config.indexType == config.indexType.toLowerCase)

      val client = HttpClient(ElasticsearchClientUri(config.elasticHostname, config.elasticPort))

      Indexer.populate(client, config.inputFile, config.indexName, config.indexType)

      client.close()
    }

    case None => println("Program interrupted")
  }

}
