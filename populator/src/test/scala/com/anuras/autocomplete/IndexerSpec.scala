package com.anuras.autocomplete

import java.nio.file.Paths
import java.util.UUID

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.embedded.LocalNode
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.http.{HttpClient, RequestFailure, RequestSuccess}
import com.sksamuel.elastic4s.testkit.ElasticMatchers
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.duration._

class IndexerSpec extends FlatSpec
  with Matchers
  with ElasticMatchers
  with BeforeAndAfterAll {

  val name: String = UUID.randomUUID().toString
  val httpClient: HttpClient = LocalNode(name, s"/tmp/elastic/$name").http(true)
  val indexName = "testcatalog"
  val indexType = "testdocument"

  "startSequenceWithEveryWord" should "return all phrases with every word at the beginning" in {

    val input1 = "a b c"
    val input2 = "singleword"
    val input3 = "two words"
    val input4 = ""
    val input5 = "one two three four "

    val expectedOutput1 = Array("a b c", "b a c", "c a b")
    val expectedOutput2 = Array("singleword")
    val expectedOutput3 = Array("two words", "words two")
    val expectedOutput4 = Array("")
    val expectedOutput5 = Array(
      "one two three four",
      "two one three four",
      "three one two four",
      "four one two three"
    )

    Indexer.startSequenceWithEveryWord(input1) should contain theSameElementsAs(expectedOutput1)
    Indexer.startSequenceWithEveryWord(input2) should contain theSameElementsAs(expectedOutput2)
    Indexer.startSequenceWithEveryWord(input3) should contain theSameElementsAs(expectedOutput3)
    Indexer.startSequenceWithEveryWord(input4) should contain theSameElementsAs(expectedOutput4)
    Indexer.startSequenceWithEveryWord(input5) should contain theSameElementsAs(expectedOutput5)
  }

  "loadValues" should "read values with weights from file" in {
    val inputFile = "/famouspeople.txt"
    val localPath = getClass.getResource(inputFile).getPath

    val commonConfig = ConfigFactory.defaultReference()

    val titles: Array[(String, Int)] = Indexer.loadValues(localPath).toArray

    titles.size shouldBe 100
    titles.contains(("edna parker", 13)) shouldBe true
  }

  "prepareValues" should "prepare values for indexer" in {
    val inputFile = "/famouspeople.txt"
    val localPath = getClass.getResource(inputFile).getPath

    val titles = Indexer.loadValues(localPath).toArray
    val indexDefinitions = Indexer.prepareValues(titles.toIterator, indexName, indexType).toArray
    val nPermutations = titles.map{ case (phrase, weight) => phrase.trim.split(" ").size }.sum

    indexDefinitions.size shouldBe nPermutations
    indexDefinitions.contains(
      indexInto(indexName, indexType).fields(Map("title" -> "edna parker", "permtitle" -> "edna parker", "popularity" -> 13))
    ) shouldBe true
    indexDefinitions.contains(
      indexInto(indexName, indexType).fields(Map("title" -> "edna parker", "permtitle" -> "parker edna", "popularity" -> 13))
    ) shouldBe true

  }


  "populate" should "populate elastic search" in {
    val inputFile = "/famouspeople.txt"
    val localPath = getClass.getResource(inputFile).getPath
    val timeoutValue: Duration = 180.seconds

    val titles: Iterable[(String, Int)] = Indexer.loadValues(localPath).toIterable
    val nPermutations = titles.map{ case (phrase, weight) => phrase.trim.split(" ").size }.sum

    Indexer.populate(httpClient, localPath, indexName, indexType, timeoutValue)

//    Thread.sleep(2000)

    val resp: Either[RequestFailure, RequestSuccess[SearchResponse]] = httpClient.execute {
      search(s"$indexName/$indexType")
    }.await(timeoutValue)

    resp match {
      case Left(failure) => {
        "Request" shouldBe "Success"
        println(failure.error)
      }
      case Right(results) => results.result.hits.total shouldBe nPermutations
    }

  }
//
//  override def afterAll: Unit = {
//    httpClient.close()
//  }

}
