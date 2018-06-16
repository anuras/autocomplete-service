package com.anuras.autocomplete

import com.anuras.autocomplete.schemas.{AutocompleteRequest, AutocompleteResponse}
import org.elasticsearch.client.Client
import org.elasticsearch.search.suggest.SuggestBuilder
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.decode
import io.circe.generic.auto._

case class ValueTitle(title: String, permtitle: String, popularity: Double)
case class InnerSuggestion(text: String, _index: String, _type: String, _id: String, _score: Double, _source: ValueTitle)
case class AutocompleteJson(text: String, offset: Int, length: Int, options: Array[InnerSuggestion])
case class Suggestion(autocomplete: Array[AutocompleteJson])
case class SuggestionResponse(suggest: Suggestion)
case class IndexerErrorMessage(msg: String)

object Retriever {

  implicit val responseDecoder: Decoder[AutocompleteResponse] = deriveDecoder[AutocompleteResponse]
  implicit val requestDecoder: Decoder[AutocompleteRequest] = deriveDecoder[AutocompleteRequest]

//  val indexName = "values"
//  val indexType = "titles"
  val minimumStringLength = 3

    def suggest(client: Client, input: String, n: Int, indexName: String, indexType: String): (Array[String], Option[IndexerErrorMessage]) = {

      if (input.length >= minimumStringLength) {
        try {

          val completion = new CompletionSuggestionBuilder("permtitle").prefix(input)
          val suggestBuilder = new SuggestBuilder().addSuggestion("autocomplete", completion)

          val search = client.prepareSearch(indexName)
            .setTypes(indexType)
            .suggest(suggestBuilder)
            .execute().actionGet()

          val (parsedResponse, errorMsg) = decode[SuggestionResponse](search.getSuggest.toString) match {
            case Left(failure) => (None, Some(IndexerErrorMessage("Error parsing ElasticSearch response")))
            case Right(resp) => (Some(resp), None)
          }

          val autocompleteResults = parsedResponse.map(
            sr => {
              val suggestions: Array[ValueTitle] = sr.suggest.autocomplete
                .flatMap(ac => ac.options.map(sugg => sugg._source))
              val titlesWithScores = suggestions.map(gv => (gv.title, gv.popularity)).distinct
              val titlesWithScoresSorted = titlesWithScores.sortBy { case (title, popularity) => -popularity }
              titlesWithScoresSorted.take(n).map { case (title, popularity) => title }
            }
          ).getOrElse(Array())

          (autocompleteResults, errorMsg)
        } catch {
          case e: Exception => {
            (Array[String](), Some(IndexerErrorMessage(e.getMessage)))
          }
        }
      } else (Array[String](), Some(IndexerErrorMessage(msg = s"Minimum input string length required: $minimumStringLength (given '${input}' with length ${input.length})")))
    }

    def getSuggestions(client: Client, request: AutocompleteRequest, indexName: String, indexType: String): AutocompleteResponse = {
      val (suggestions, errorMsg) = suggest(client, request.queryText, request.numResults.toInt, indexName, indexType)
      AutocompleteResponse(queryText = request.queryText, suggestions = suggestions, errorMessage = errorMsg.map(iem => iem.msg))
    }

}