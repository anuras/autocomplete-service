package com.anuras.autocomplete.schemas

case class AutocompleteResponse(queryText: String, suggestions: Array[String], errorMessage: Option[String] = None)
