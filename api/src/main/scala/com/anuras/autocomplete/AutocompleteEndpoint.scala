package com.anuras.autocomplete

import java.net.InetAddress

import com.anuras.autocomplete.schemas.{AutocompleteRequest, AutocompleteResponse}
import com.twitter.finagle.Http
import com.twitter.util.Await
import com.typesafe.config.ConfigFactory
import io.circe.generic.semiauto.deriveDecoder
import io.finch.circe.decodeCirce
import io.finch._
import io.finch.syntax._
import io.finch.circe._
import io.circe.Decoder
import io.circe.generic.auto._
import org.elasticsearch.client.Client
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient

object AutocompleteEndpoint extends App {

  implicit val responseDecoder: Decoder[AutocompleteResponse] = deriveDecoder[AutocompleteResponse]
  implicit val requestDecoder: Decoder[AutocompleteRequest] = deriveDecoder[AutocompleteRequest]
  implicit val finchResponseDecode = decodeCirce(responseDecoder)
  implicit val finchRequestDecode = decodeCirce(requestDecoder)

  val config = ConfigFactory.load()

  val settings = Settings.builder().put("cluster.name", config.getString("autocomplete.esClusterName")).put("client.transport.sniff", true).build()
  val client: Client = new PreBuiltTransportClient(settings).addTransportAddress(new TransportAddress(InetAddress.getByName(config.getString("autocomplete.esHost")), 9300))
  val indexName = config.getString("autocomplete.esIndexName")
  val indexType = config.getString("autocomplete.esIndexType")

  val autocomplete: Endpoint[AutocompleteResponse] = post("autocomplete" :: jsonBody[AutocompleteRequest]) {
    r: AutocompleteRequest => Ok(Retriever.getSuggestions(client, r, indexName, indexType))
  }

  Await.result(Http.server.serve(":8088", autocomplete.toService))

}
