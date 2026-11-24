Feature: Vocabulary references and validation warnings
  A vocabulary reference is a namespace declared with @prefix or PREFIX (D12). Internal references point
  at <contentBase>/content/<Name>[/versions/<n>]; a missing one is error S7 (D12, D47, D69, D146, D192),
  an effectively deprecated one gives warning deprecatedVocabularyReference (D11, D47).
  Other namespaces give externalReferenceSkipped unless registered, W3C or the vocabulary's own (D12, D198).
  Warnings never block and are returned only in the 201 response (D10).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18184_AC1 @D11 @D12 @D3
  Scenario: A reference to an ACTIVE internal version passes without any deprecated warning
    Given a vocabulary "$Beta" exists
    And a vocabulary "$Alpha" exists
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix b: <$CB/content/$Beta/versions/1#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related b:Scheme .
      """
    When I create a new version of "$Alpha"
    Then the response status is 201
    And the response has no validation warnings
    And version 2 of vocabulary "$Alpha" has version status "ACTIVE"

  @AC_SIMPL-18184_AC1 @D12
  Scenario: A reference to the latest version of an ACTIVE vocabulary passes on creation
    Given a vocabulary "$Beta" exists
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix b: <$CB/content/$Beta#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related b:Scheme .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has no validation warnings

  @AC_SIMPL-18184_AC2 @D11 @D47 @D65 @D171 @D14
  Scenario: A reference to a deprecated version warns, recommends the latest ACTIVE version and does not block
    Given vocabulary "$Beta" has 2 versions
    And a vocabulary "$Alpha" exists
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix b: <$CB/content/$Beta/versions/1#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related b:Scheme .
      """
    When I create a new version of "$Alpha"
    Then the response status is 201
    And the response has 1 validation warning
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '$Beta' version 1 is deprecated. Use version 2, the latest ACTIVE version."
    And the response has a validation warning "deprecatedVocabularyReference" with location "prefix 'b' -> '$CB/content/$Beta/versions/1#'"
    And vocabulary "$Alpha" has 2 versions
    And version 1 of vocabulary "$Beta" has version status "DEPRECATED"
    And version 2 of vocabulary "$Beta" has version status "ACTIVE"

  @AC_SIMPL-18184_AC2 @D11 @D47 @D3
  Scenario: A reference to a version of a DEPRECATED vocabulary recommends no version
    Given vocabulary "$Beta" has 2 versions
    And vocabulary "$Beta" has vocabulary status "DEPRECATED"
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix b: <$CB/content/$Beta/versions/1#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related b:Scheme .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has 1 validation warning
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '$Beta' version 1 is deprecated. The vocabulary '$Beta' is deprecated."

  @AC_SIMPL-18184_AC2 @D11 @D47 @D3 @D5
  Scenario: The latest version of a DEPRECATED vocabulary is effectively deprecated
    Given a vocabulary "$Beta" exists
    And vocabulary "$Beta" has vocabulary status "DEPRECATED"
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix b: <$CB/content/$Beta#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related b:Scheme .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has 1 validation warning
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '$Beta' version 1 is deprecated. The vocabulary '$Beta' is deprecated."
    And the response has a validation warning "deprecatedVocabularyReference" with location "prefix 'b' -> '$CB/content/$Beta#'"

  @D11 @D47
  Scenario: Without another ACTIVE version the warning recommends nothing
    Given vocabulary "$Beta" has 2 versions
    And version 2 of vocabulary "$Beta" has version status "DEPRECATED"
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix b: <$CB/content/$Beta/versions/1#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related b:Scheme .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '$Beta' version 1 is deprecated."

  @D11 @D47 @D5 @D12
  Scenario: A reference without a version resolves to the latest version, whatever its status
    Given vocabulary "$Beta" has 2 versions
    And version 2 of vocabulary "$Beta" has version status "DEPRECATED"
    And version 1 of vocabulary "$Beta" has version status "ACTIVE"
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix b: <$CB/content/$Beta/> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related b:Scheme .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has 1 validation warning
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '$Beta' version 2 is deprecated. Use version 1, the latest ACTIVE version."

  @D11 @D10
  Scenario: Each deprecated reference gives its own warning
    Given vocabulary "$Beta" has 2 versions
    And vocabulary "$Gamma" has 2 versions
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix b: <$CB/content/$Beta/versions/1#> .
      @prefix g: <$CB/content/$Gamma/versions/1#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related b:Scheme , g:Scheme .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has 2 validation warnings
    And the response has a validation warning "deprecatedVocabularyReference" with location "prefix 'b' -> '$CB/content/$Beta/versions/1#'"
    And the response has a validation warning "deprecatedVocabularyReference" with location "prefix 'g' -> '$CB/content/$Gamma/versions/1#'"

  @AC_SIMPL-18184_AC3 @D12 @D47 @D65 @D8 @negative
  Scenario: A reference to a version that does not exist is error S7 and no version is created
    Given a vocabulary "$Beta" exists
    And a vocabulary "$Alpha" exists
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix b: <$CB/content/$Beta/versions/7#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related b:Scheme .
      """
    When I create a new version of "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And every issue is in "body" named "vocabularyFile" with type "urn:problem-type:belgif:input-validation:invalidInput"
    And issue 1 has detail "Referenced internal vocabulary '$Beta' version 7 does not exist."
    And issue 1 has location "prefix 'b' -> '$CB/content/$Beta/versions/7#'"
    And vocabulary "$Alpha" has 1 version
    And version 1 of vocabulary "$Alpha" has version status "ACTIVE"

  @AC_SIMPL-18184_AC3 @D12 @D47 @negative
  Scenario: A reference to a vocabulary that does not exist is error S7
    Given the content base is known
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix m: <$CB/content/$Missing#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related m:Scheme .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 has detail "Referenced internal vocabulary '$Missing' does not exist."
    And issue 1 has location "prefix 'm' -> '$CB/content/$Missing#'"
    And 0 vocabularies have a name containing "$Alpha"

  @D69 @D47 @negative
  Scenario: A version segment that is not a positive integer is error S7
    Given a vocabulary "$Beta" exists
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix b: <$CB/content/$Beta/versions/latest#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related b:Scheme .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 has detail "Referenced internal vocabulary '$Beta' version latest does not exist."

  @D192 @D69 @negative
  Scenario: A namespace ending in /versions/ has an empty version and is error S7
    Given a vocabulary "$Beta" exists
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix b: <$CB/content/$Beta/versions/> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related b:Scheme .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 has a detail starting with "Referenced internal vocabulary '$Beta' version"
    And issue 1 has location "prefix 'b' -> '$CB/content/$Beta/versions/'"

  @D146
  Scenario: Any further path under an existing internal name is a reference to its latest version
    Given a vocabulary "$Beta" exists
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix b: <$CB/content/$Beta/terms/> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related b:Scheme .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has no validation warnings

  @D146 @D47 @negative
  Scenario: A further path under an unknown internal name is error S7 without a version
    Given the content base is known
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix m: <$CB/content/$Missing/terms/> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related m:Scheme .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And issue 1 has detail "Referenced internal vocabulary '$Missing' does not exist."

  @D69 @D12
  Scenario Outline: A content URL without a valid name is an external reference: <namespace>
    Given the content base is known
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix z: <<namespace>> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related z:Scheme .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has 1 validation warning
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'z' -> '<namespace>'"

    Examples:
      | namespace                  |
      | $CB/content/               |
      | $CB/content/lower$RUN#     |
      | $CB/content/X-$RUN#        |

  @D12 @D171
  Scenario: A reference to the vocabulary being versioned is ignored
    Given a vocabulary "$Alpha" exists
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix me: <$CB/content/$Alpha#> .
      @prefix mev: <$CB/content/$Alpha/versions/9#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related me:Scheme , mev:Scheme .
      """
    When I create a new version of "$Alpha"
    Then the response status is 201
    And the response has no validation warnings

  @D12
  Scenario: A reference to the vocabulary being created is ignored
    Given the content base is known
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix me: <$CB/content/$Alpha/versions/1#> .
      me:Scheme a skos:ConceptScheme ; skos:prefLabel "Self"@en .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has no validation warnings

  @D12 @D10 @D65 @D171
  Scenario: An unregistered external namespace gives warning externalReferenceSkipped and the vocabulary is created
    Given the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix dct: <$EXT/terms/> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; dct:title "Alpha" .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has 1 validation warning
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'dct' -> '$EXT/terms/'"
    And every validation warning code is one of "externalReferenceSkipped, deprecatedVocabularyReference"
    And 1 vocabulary has a name containing "$Alpha"

  @D12 @D10
  Scenario: SPARQL-style PREFIX declarations are references too
    Given the vocabulary file is:
      """
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      PREFIX dct: <$EXT/sparql/>
      PREFIX ex: <https://example.org/$RUN/alpha#>
      ex:Scheme a skos:ConceptScheme ; dct:title "Alpha" .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'dct' -> '$EXT/sparql/'"

  @D12 @D30 @D41
  Scenario Outline: A registered external namespace or alias gives no warning: <declared>
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/reg" and aliases "$EXT/alias#"
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix r: <<declared>> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related r:Thing .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has no validation warnings

    Examples:
      | declared     |
      | $EXT/reg     |
      | $EXT/reg#    |
      | $EXT/reg/    |
      | $EXT/alias   |
      | $EXT/alias/  |

  @D42 @D12
  Scenario: After the external vocabulary is removed the namespace gives the warning again
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/gone#"
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix r: <$EXT/gone#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related r:Thing .
      """
    And a vocabulary "$Alpha" exists with that file
    And the external vocabulary "$Ext" is removed
    When I create vocabulary "$Beta"
    Then the response status is 201
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'r' -> '$EXT/gone#'"
    When I send a GET request to "/vocabularies/$Alpha/versions/1"
    Then the response status is 200
    And the response field "status" equals "ACTIVE"
    When I send a GET request to "/content/$Alpha/versions/1"
    Then the response body is the uploaded file of "$Alpha" version 1

  @D42 @D12
  Scenario: Registering the external vocabulary later clears the warning on the next version
    Given the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix r: <$EXT/later#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related r:Thing .
      """
    When I create vocabulary "$Alpha"
    Then the response has a validation warning "externalReferenceSkipped" with location "prefix 'r' -> '$EXT/later#'"
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/later"
    When I create a new version of "$Alpha" with description "Now resolved."
    Then the response status is 201
    And the response has no validation warnings

  @D198 @D12
  Scenario: W3C namespaces are recognised by IRI, under any prefix name
    Given the vocabulary file is:
      """
      @prefix k: <http://www.w3.org/2004/02/skos/core#> .
      @prefix r: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
      @prefix s: <http://www.w3.org/2000/01/rdf-schema#> .
      @prefix o: <http://www.w3.org/2002/07/owl#> .
      @prefix d: <http://www.w3.org/2001/XMLSchema#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme r:type k:ConceptScheme ; s:label "A" ; s:comment "1"^^d:string .
      ex:C a o:Class .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has no validation warnings

  @D198 @D12
  Scenario: A W3C prefix name bound to another namespace is not a W3C namespace
    Given the vocabulary file is:
      """
      @prefix skos: <$EXT/not-skos#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme skos:prefLabel "A" .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'skos' -> '$EXT/not-skos#'"

  @D12
  Scenario: A namespace that prefixes a subject is the vocabulary's own namespace
    Given the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix one: <https://example.org/$RUN/one#> .
      @prefix two: <https://example.org/$RUN/two#> .
      one:Scheme a skos:ConceptScheme .
      two:Concept a skos:Concept ; skos:inScheme one:Scheme .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has no validation warnings

  @D145 @D65
  Scenario: A namespace declared under two prefixes gives one warning at its first declaration
    Given the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix first: <$EXT/twice#> .
      @prefix second: <$EXT/twice#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related first:A , second:B .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has 1 validation warning
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'first' -> '$EXT/twice#'"

  @D10
  Scenario: Warnings are not stored and not returned by other operations
    Given the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix dct: <$EXT/terms/> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; dct:title "Alpha" .
      """
    When I create vocabulary "$Alpha"
    Then the response has 1 validation warning
    When I send a GET request to "/vocabularies/$Alpha/versions/1"
    Then the response field "validationWarnings" is absent
    When I send a GET request to "/vocabularies/$Alpha"
    Then the response field "validationWarnings" is absent
    When I send a GET request to "/vocabularies/$Alpha/versions"
    Then the response field "items[0].validationWarnings" is absent
