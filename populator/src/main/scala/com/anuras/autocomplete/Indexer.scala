package com.anuras.autocomplete

import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexDefinition
import com.sksamuel.elastic4s.searches.SearchDefinition

import scala.io.Source

import scala.concurrent.duration._

object Indexer {

  def startSequenceWithEveryWord(input: String): Array[String] = {
    val splitInput = input.trim.split(" ")
    val reorderings = Iterator.range(0, splitInput.length).map(index => wordReorder(splitInput, index))
    reorderings.map(arr => arr.mkString(" ")).toArray
  }

  def wordReorder(wordArray: Array[String], indexToHead: Int): Array[String] = {
    assert(indexToHead < wordArray.length)
    Array(wordArray(indexToHead)) ++ wordArray.take(indexToHead) ++ wordArray.drop(indexToHead + 1)
  }

//  val indexName = "values"
//  val indexType = "titles"

  def loadValues(filePath: String): Iterator[(String, Int)] = {
    val values = Source.fromFile(filePath).getLines()
      .map( line => {
        val reverseSplit = line.split(" ").reverse
        val weight = reverseSplit.head.toDouble.toInt
        val title = reverseSplit.tail.reverse.mkString(" ").trim.toLowerCase
        (title, weight)
      })
    values
  }

  def prepareValues(values: Iterator[(String, Int)], indexName: String, indexType: String): Iterator[IndexDefinition] = {
    values
      .flatMap{ case (phrase, weight) => startSequenceWithEveryWord(phrase).map(permPhrase => (phrase, permPhrase, weight))}
      .map {
        case (phrase, permPhrase, weight) => indexInto(indexName, indexType)
          .fields(Map("title" -> phrase, "permtitle" -> permPhrase, "popularity" -> weight))
      }
  }

  def populate(client: HttpClient, filePath: String, indexName: String, indexType: String, populationTimeout: Duration = 60.seconds): Unit = {

    val populateValues = prepareValues(loadValues(filePath), indexName: String, indexType: String).toIterable

    client.execute { createIndex(indexName).mappings(mapping(indexType).fields(Seq(completionField("permtitle").analyzer("standard"))))}.await
    client.execute{ bulk(populateValues).refresh(RefreshPolicy.IMMEDIATE) }.await(populationTimeout)

  }

}
