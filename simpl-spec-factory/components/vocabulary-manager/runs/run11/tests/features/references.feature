Feature: Vocabulary references in uploaded content
  A vocabulary reference is a namespace declared with @prefix or PREFIX. It is internal when it
  starts with '<contentBase>/content/<Name>' (D12). A reference to an effectively deprecated
  internal version gives a warning, not an error (D11, D47). An unknown internal reference is
  error S7. An unregistered external namespace gives the warning externalReferenceSkipped.

  Background:
    Given I am a vocabulary administrator

  @AC_SIMPL-18184_AC1 @D11 @D12
  Scenario: A new version that references only ACTIVE internal versions gets no deprecated warning
    Given a vocabulary "Ref" exists
    And a vocabulary "Other" exists
    And a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix ref: <${CB}/content/${Ref}/versions/1#> .
      PREFIX other: <${CB}/content/${Other}#>
      ex:Concept1 a skos:Concept ; skos:related ref:Concept1 , other:Concept1 .
      """
    When I submit the request
    Then the response status is 201
    And the response has no validation warnings

  @AC_SIMPL-18184_AC2 @D11 @D47 @D65 @D10
  Scenario: A reference to a version deprecated by a newer version warns and recommends the latest ACTIVE version
    Given a vocabulary "Ref" exists
    And vocabulary "Ref" has a new version
    And a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix ref: <${CB}/content/${Ref}/versions/1#> .
      ex:Concept1 a skos:Concept ; skos:related ref:Concept1 .
      """
    When I submit the request
    Then the response status is 201
    And the response has exactly 1 validation warnings
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '${Ref}' version 1 is deprecated. Use version 2, the latest ACTIVE version." and location "prefix 'ref' -> '${CB}/content/${Ref}/versions/1#'"
    And version 2 of vocabulary "A" has status "ACTIVE"
    And version 1 of vocabulary "Ref" has status "DEPRECATED"

  @AC_SIMPL-18184_AC2 @D11 @D3 @D47
  Scenario: A reference to an ACTIVE version of a DEPRECATED vocabulary warns without a recommendation
    Given a vocabulary "Ref" exists
    And the status of vocabulary "Ref" is changed to "DEPRECATED"
    And a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix ref: <${CB}/content/${Ref}/versions/1#> .
      ex:Concept1 a skos:Concept ; skos:related ref:Concept1 .
      """
    When I submit the request
    Then the response status is 201
    And the response has exactly 1 validation warnings
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '${Ref}' version 1 is deprecated. The vocabulary '${Ref}' is deprecated." and location "prefix 'ref' -> '${CB}/content/${Ref}/versions/1#'"

  @AC_SIMPL-18184_AC2 @D11 @D47
  Scenario: A reference to a deprecated version with no other ACTIVE version warns without a recommendation
    Given a vocabulary "Ref" exists
    And the status of version 1 of vocabulary "Ref" is changed to "DEPRECATED"
    And a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix ref: <${CB}/content/${Ref}/versions/1#> .
      ex:Concept1 a skos:Concept ; skos:related ref:Concept1 .
      """
    When I submit the request
    Then the response status is 201
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '${Ref}' version 1 is deprecated." and location "prefix 'ref' -> '${CB}/content/${Ref}/versions/1#'"

  @AC_SIMPL-18184_AC2 @D5 @D11 @D12 @D47
  Scenario: A reference without a version resolves to the latest version, whatever its status
    Given a vocabulary "Ref" exists
    And vocabulary "Ref" has a new version
    And the status of version 2 of vocabulary "Ref" is changed to "DEPRECATED"
    And the status of version 1 of vocabulary "Ref" is changed to "ACTIVE"
    And a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix ref: <${CB}/content/${Ref}#> .
      ex:Concept1 a skos:Concept ; skos:related ref:Concept1 .
      """
    When I submit the request
    Then the response status is 201
    And the response has exactly 1 validation warnings
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '${Ref}' version 2 is deprecated. Use version 1, the latest ACTIVE version." and location "prefix 'ref' -> '${CB}/content/${Ref}#'"

  @AC_SIMPL-18184_AC2 @D11 @D12
  Scenario: Each deprecated reference gives its own warning, also on creation
    Given a vocabulary "Ref" exists
    And vocabulary "Ref" has a new version
    And a vocabulary "Old" exists
    And the status of vocabulary "Old" is changed to "DEPRECATED"
    And a create vocabulary request for "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix ref: <${CB}/content/${Ref}/versions/1/> .
      @prefix old: <${CB}/content/${Old}#> .
      ex:Concept1 a skos:Concept ; skos:related ref:Concept1 , old:Concept1 .
      """
    When I submit the request
    Then the response status is 201
    And the response has exactly 2 validation warnings
    And the response has a validation warning "deprecatedVocabularyReference" with location "prefix 'ref' -> '${CB}/content/${Ref}/versions/1/'"
    And the response has a validation warning "deprecatedVocabularyReference" with location "prefix 'old' -> '${CB}/content/${Old}#'"

  @AC_SIMPL-18184_AC3 @D12 @D47 @D65 @D8 @negative
  Scenario: A reference to an internal version that does not exist is error S7
    Given a vocabulary "Ref" exists
    And a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix ref: <${CB}/content/${Ref}/versions/9#> .
      ex:Concept1 a skos:Concept ; skos:related ref:Concept1 .
      """
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And every issue is in "body" named "vocabularyFile" with type "urn:problem-type:belgif:input-validation:invalidInput"
    And the problem has an issue with detail "Referenced internal vocabulary '${Ref}' version 9 does not exist." and location "prefix 'ref' -> '${CB}/content/${Ref}/versions/9#'"
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has status "ACTIVE"

  @AC_SIMPL-18184_AC3 @D47 @D12 @negative
  Scenario: A reference to an internal vocabulary name that does not exist is error S7
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix ghost: <${CB}/content/${Ghost}#> .
      ex:Concept1 a skos:Concept ; skos:related ghost:Concept1 .
      """
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue with detail "Referenced internal vocabulary '${Ghost}' does not exist." and location "prefix 'ghost' -> '${CB}/content/${Ghost}#'"
    And no vocabulary "A" is stored

  @D69 @D47 @negative
  Scenario: A version part that is not a positive integer is error S7 with the version as given
    Given a vocabulary "Ref" exists
    And a create vocabulary request for "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix ref: <${CB}/content/${Ref}/versions/abc#> .
      ex:Concept1 a skos:Concept ; skos:related ref:Concept1 .
      """
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with detail "Referenced internal vocabulary '${Ref}' version abc does not exist." and location "prefix 'ref' -> '${CB}/content/${Ref}/versions/abc#'"
    And no vocabulary "A" is stored

  @D69 @D12
  Scenario: A content-base namespace whose name breaks the naming rules is external
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix odd: <${CB}/content/${lower:A}x#> .
      ex:Concept1 a skos:Concept ; skos:related odd:Concept1 .
      """
    When I submit the request
    Then the response status is 201
    And the response has exactly 1 validation warnings
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'odd' -> '${CB}/content/${lower:A}x#'"

  @D12
  Scenario: A reference to the vocabulary being created is ignored
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix self: <${CB}/content/${A}/versions/5#> .
      ex:Concept1 a skos:Concept ; skos:related self:Concept1 .
      """
    When I submit the request
    Then the response status is 201
    And the response has no validation warnings

  @D12
  Scenario: A reference to the vocabulary being versioned is ignored, even to a deprecated version
    Given a vocabulary "A" exists
    And vocabulary "A" has a new version
    And a new version request for vocabulary "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix prev: <${CB}/content/${A}/versions/1#> .
      ex:Concept1 a skos:Concept ; skos:related prev:Concept1 .
      """
    When I submit the request
    Then the response status is 201
    And the response has no validation warnings

  @AC_SIMPL-18183_AC1 @D10 @D12 @D65
  Scenario: An unregistered external namespace gives the warning externalReferenceSkipped and the vocabulary is created
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix unknown: <${URI:Unregistered}/> .
      ex:Concept1 a skos:Concept ; skos:related unknown:Thing .
      """
    When I submit the request
    Then the response status is 201
    And the response has exactly 1 validation warnings
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'unknown' -> '${URI:Unregistered}/'"
    And every validation warning code is in the closed set
    And version 1 of vocabulary "A" has status "ACTIVE"

  @D12
  Scenario: W3C namespaces and the own namespace give no warning
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
      @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      ex:Thing a owl:Class ; rdfs:label "Thing"^^xsd:string ; rdf:value "1" .
      ex:Concept1 a skos:Concept .
      """
    When I submit the request
    Then the response status is 201
    And the response has no validation warnings

  @D12 @D30 @D42
  Scenario: A registered external namespace gives no warning, also with or without a trailing separator
    Given an external vocabulary "Ext" is registered with namespace "${URI:Ext}"
    And an external vocabulary "Ali" is registered with namespace "${URI:Ali}" and aliases "${URI:AliAlias}/"
    And a create vocabulary request for "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix ext: <${URI:Ext}#> .
      @prefix ali: <${URI:AliAlias}#> .
      ex:Concept1 a skos:Concept ; skos:related ext:Term , ali:Term .
      """
    When I submit the request
    Then the response status is 201
    And the response has no validation warnings

  @D42 @D41
  Scenario: Removing an external vocabulary brings the warning back for later uploads and keeps stored versions
    Given an external vocabulary "Ext" is registered with namespace "${URI:Ext}"
    And a vocabulary "A" exists with the Turtle
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix ext: <${URI:Ext}#> .
      ex:Concept1 a skos:Concept ; skos:related ext:Term .
      """
    When I send a DELETE request to "/external-vocabularies/${Ext}"
    Then the response status is 204
    Given a new version request for vocabulary "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix ext: <${URI:Ext}#> .
      ex:Concept1 a skos:Concept ; skos:related ext:Term , ex:Concept2 .
      """
    When I submit the request
    Then the response status is 201
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'ext' -> '${URI:Ext}#'"
    When I send a GET request to "/content/${A}/versions/1"
    Then the response status is 200
    And the response body equals the content of version 1 of vocabulary "A"

  @D42 @D12
  Scenario: A vocabulary created before its external vocabulary is registered keeps the warning until a new version
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix late: <${URI:Late}#> .
      ex:Concept1 a skos:Concept ; skos:related late:Term .
      """
    When I submit the request
    Then the response status is 201
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'late' -> '${URI:Late}#'"
    Given an external vocabulary "Late" is registered with namespace "${URI:Late}"
    And a new version request for vocabulary "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix late: <${URI:Late}#> .
      ex:Concept1 a skos:Concept ; skos:related late:Term , ex:Concept2 .
      """
    When I submit the request
    Then the response status is 201
    And the response has no validation warnings

  @D69 @D12
  Scenario: A content-base namespace without a name is external
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix x: <${CB}/content/> .
      ex:Concept1 a skos:Concept ; skos:related x:Thing .
      """
    When I submit the request
    Then the response status is 201
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'x' -> '${CB}/content/'"
