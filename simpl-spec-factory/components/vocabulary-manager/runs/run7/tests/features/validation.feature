Feature: Syntax and semantic validation of uploaded content
  Two steps on every POST /vocabularies and POST .../versions: syntax, then semantic (D9).
  Validation errors block with 400; validation warnings never block (D10).

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18170_AC3 @D9 @D10 @D14 @D8 @D65 @negative
  Scenario: A syntax error is rejected with one issue holding its line and nothing is stored
    When "ga-admin" creates vocabulary "Alpha" with the file "syntax_error.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | type                                                | in   | name           | detail                              | location                     |
      | urn:problem-type:belgif:input-validation:invalidInput | body | vocabularyFile | ^Encountered invalid Turtle syntax | ~^line 3(, column [0-9]+)?$ |
    And no vocabulary of this scenario is stored

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18173_AC2 @D9 @D8 @negative
  Scenario: A new version with a syntax error is rejected and nothing changes
    Given vocabulary "Alpha" exists
    When "ga-admin" creates a new version of "Alpha" with the file "syntax_error.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "vocabularyFile"
    And vocabulary "Alpha" has 1 version
    And version 1 of "Alpha" has status "ACTIVE"

  @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC1 @AC_SIMPL-18194_AC2 @D9 @D10 @D14
  Scenario: Valid content passes both steps and the response confirms creation without errors
    When "ga-admin" creates vocabulary "Alpha" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <http://example.org/${rid}/ok#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme"@en , "Schema"@nl .
      """
    Then the response status is 201
    And the response has no validation warnings
    And the response field "status" equals "ACTIVE"

  @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC2 @D9 @negative
  Scenario: Content with valid syntax goes on to semantic validation
    When "ga-admin" creates vocabulary "Alpha" with the file "semantic_s3.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | detail                                                 | location                                       |
      | Property declared as both object and datatype property | resource 'http://example.org/${rid}/s3#hasPart' |
    And no vocabulary of this scenario is stored

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @D9 @D10 @D65 @negative
  Scenario: S1 a literal as the object of an object property is a validation error
    When "ga-admin" creates vocabulary "Alpha" with this content
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <http://example.org/${rid}/s1#> .
      ex:hasPart a owl:ObjectProperty .
      ex:Thing ex:hasPart "a literal" .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | type                                                  | in   | name           | detail                            | location                                                                          |
      | urn:problem-type:belgif:input-validation:invalidInput | body | vocabularyFile | Literal value for object property | ^statement 'http://example.org/${rid}/s1#Thing http://example.org/${rid}/s1#hasPart |
    And no vocabulary of this scenario is stored

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: S2 a resource as the object of a datatype property is a validation error
    When "ga-admin" creates vocabulary "Alpha" with this content
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <http://example.org/${rid}/s2#> .
      ex:size a owl:DatatypeProperty .
      ex:Thing ex:size ex:Other .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | detail                              | location                                                                                                        |
      | Resource value for datatype property | statement 'http://example.org/${rid}/s2#Thing http://example.org/${rid}/s2#size http://example.org/${rid}/s2#Other' |

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: S4 two preferred labels with the same language tag are a validation error
    When "ga-admin" creates vocabulary "Alpha" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <http://example.org/${rid}/s4#> .
      ex:Concept a skos:Concept ;
        skos:prefLabel "First"@en , "Second"@en .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | detail                                         | location                                                     |
      | Duplicate preferred label for the same language | resource 'http://example.org/${rid}/s4#Concept', language 'en' |

  @AC_SIMPL-18183_AC2 @D9 @D53 @D65 @negative
  Scenario: S4 two untagged preferred labels count as one language
    When "ga-admin" creates vocabulary "Alpha" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <http://example.org/${rid}/s4#> .
      ex:Concept a skos:Concept ;
        skos:prefLabel "First" , "Second" .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | detail                                         | location                                                       |
      | Duplicate preferred label for the same language | resource 'http://example.org/${rid}/s4#Concept', language 'none' |

  @AC_SIMPL-18183_AC1 @D53
  Scenario: S4 one untagged and one tagged preferred label are not an error
    When "ga-admin" creates vocabulary "Alpha" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <http://example.org/${rid}/s4#> .
      ex:Concept a skos:Concept ;
        skos:prefLabel "Plain" , "Tagged"@en .
      """
    Then the response status is 201
    And the response has no validation warnings

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: S5 a resource typed with two disjoint classes is a validation error
    When "ga-admin" creates vocabulary "Alpha" with this content
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <http://example.org/${rid}/s5#> .
      ex:Cat a owl:Class ; owl:disjointWith ex:Dog .
      ex:Dog a owl:Class .
      ex:Felix a ex:Cat , ex:Dog .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | detail                                    | location                                     |
      | Resource is an instance of disjoint classes | resource 'http://example.org/${rid}/s5#Felix' |

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: S6 content with no triples is a validation error
    When "ga-admin" creates vocabulary "Alpha" with the file "comments_only.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | in   | name           | detail                            | location |
      | body | vocabularyFile | Vocabulary contains no statements | document |
    And no vocabulary of this scenario is stored

  @D70 @D9 @negative
  Scenario: A 0-byte file passes the field check and fails validation with S6
    When "ga-admin" creates vocabulary "Alpha" with the fields
      | field          | value   |
      | vocabularyFile | <empty> |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | name           | detail                            | location |
      | vocabularyFile | Vocabulary contains no statements | document |

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18173_AC2 @D9 @D10 @D8 @negative
  Scenario: All semantic errors are returned in one response
    Given vocabulary "Alpha" exists
    When "ga-admin" creates a new version of "Alpha" with this content
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <http://example.org/${rid}/multi#> .
      ex:hasPart a owl:ObjectProperty .
      ex:Thing ex:hasPart "a literal" .
      ex:mixed a owl:ObjectProperty , owl:DatatypeProperty .
      ex:Concept a skos:Concept ;
        skos:prefLabel "One"@fr , "Two"@fr .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | in   | name           | detail                                                 | location                                                         |
      | body | vocabularyFile | Literal value for object property                      | ^statement 'http://example.org/${rid}/multi#Thing                |
      | body | vocabularyFile | Property declared as both object and datatype property | resource 'http://example.org/${rid}/multi#mixed'                  |
      | body | vocabularyFile | Duplicate preferred label for the same language        | resource 'http://example.org/${rid}/multi#Concept', language 'fr' |
    And vocabulary "Alpha" has 1 version
    And version 1 of "Alpha" has status "ACTIVE"

  @AC_SIMPL-18184_AC3 @D12 @D47 @D65 @D9 @negative
  Scenario: S7 a reference to a version that does not exist blocks a new version
    Given vocabulary "Alpha" exists
    And vocabulary "Gamma" exists
    When "ga-admin" creates a new version of "Gamma" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix alpha: <${CB}/content/${V:Alpha}/versions/5#> .
      @prefix ex: <http://example.org/${rid}/gamma#> .
      ex:Concept a skos:Concept ;
        skos:related alpha:Concept .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | in   | name           | detail                                                             | location                                                  |
      | body | vocabularyFile | Referenced internal vocabulary '${V:Alpha}' version 5 does not exist. | prefix 'alpha' -> '${CB}/content/${V:Alpha}/versions/5#' |
    And vocabulary "Gamma" has 1 version
    And version 1 of "Gamma" has status "ACTIVE"

  @AC_SIMPL-18184_AC3 @D12 @D47 @D65 @negative
  Scenario: S7 a reference without a version to a vocabulary that does not exist blocks creation
    When "ga-admin" creates vocabulary "Beta" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ghost: <${CB}/content/${V:Ghost}#> .
      @prefix ex: <http://example.org/${rid}/beta#> .
      ex:Concept a skos:Concept ;
        skos:related ghost:Concept .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | detail                                                    | location                                     |
      | Referenced internal vocabulary '${V:Ghost}' does not exist. | prefix 'ghost' -> '${CB}/content/${V:Ghost}#' |
    And no vocabulary of this scenario is stored

  @D69 @D47 @negative
  Scenario: S7 a version segment that is not a positive integer is a missing reference
    Given vocabulary "Alpha" exists
    When "ga-admin" creates vocabulary "Beta" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix alpha: <${CB}/content/${V:Alpha}/versions/abc#> .
      @prefix ex: <http://example.org/${rid}/beta#> .
      ex:Concept a skos:Concept ;
        skos:related alpha:Concept .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | detail                                                               | location                                                    |
      | Referenced internal vocabulary '${V:Alpha}' version abc does not exist. | prefix 'alpha' -> '${CB}/content/${V:Alpha}/versions/abc#' |
    And no vocabulary named "Beta" is stored

  @D69 @D12
  Scenario: A content namespace with a name that breaks the name pattern is external
    When "ga-admin" creates vocabulary "Beta" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix odd: <${CB}/content/lower${rid}#> .
      @prefix ex: <http://example.org/${rid}/beta#> .
      ex:Concept a skos:Concept ;
        skos:related odd:Concept .
      """
    Then the response status is 201
    And the validation warnings are exactly
      | code                     | location                                      |
      | externalReferenceSkipped | prefix 'odd' -> '${CB}/content/lower${rid}#' |

  @AC_SIMPL-18184_AC1 @D11 @D12 @D5
  Scenario: References to ACTIVE versions give no deprecated-version warning
    Given vocabulary "Alpha" exists
    And vocabulary "Gamma" exists
    When "ga-admin" creates a new version of "Gamma" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix alpha: <${CB}/content/${V:Alpha}/versions/1#> .
      @prefix alphalatest: <${CB}/content/${V:Alpha}/> .
      @prefix ex: <http://example.org/${rid}/gamma#> .
      ex:Concept a skos:Concept ;
        skos:related alpha:Concept , alphalatest:Other .
      """
    Then the response status is 201
    And the response has no validation warnings
    And the response field "version" equals 2

  @AC_SIMPL-18184_AC2 @D11 @D47 @D3 @D65 @D10
  Scenario: A reference to a deprecated version warns and recommends the latest ACTIVE version
    Given vocabulary "Alpha" has 2 versions
    And vocabulary "Gamma" exists
    When "ga-admin" creates a new version of "Gamma" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix alpha: <${CB}/content/${V:Alpha}/versions/1#> .
      @prefix ex: <http://example.org/${rid}/gamma#> .
      ex:Concept a skos:Concept ;
        skos:related alpha:Concept .
      """
    Then the response status is 201
    And the validation warnings are exactly
      | code                          | description                                                                                         | location                                                  |
      | deprecatedVocabularyReference | Referenced internal vocabulary '${V:Alpha}' version 1 is deprecated. Use version 2, the latest ACTIVE version. | prefix 'alpha' -> '${CB}/content/${V:Alpha}/versions/1#' |
    And version 2 of "Gamma" has status "ACTIVE"
    And version 1 of "Alpha" has status "DEPRECATED"

  @AC_SIMPL-18184_AC2 @D11 @D47 @D3
  Scenario: A reference to a version of a DEPRECATED vocabulary warns without recommending a version
    Given vocabulary "Alpha" exists
    And vocabulary "Alpha" has status "DEPRECATED"
    When "ga-admin" creates vocabulary "Beta" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix alpha: <${CB}/content/${V:Alpha}/versions/1#> .
      @prefix ex: <http://example.org/${rid}/beta#> .
      ex:Concept a skos:Concept ;
        skos:related alpha:Concept .
      """
    Then the response status is 201
    And the validation warnings are exactly
      | code                          | description                                                                                | location                                                  |
      | deprecatedVocabularyReference | Referenced internal vocabulary '${V:Alpha}' version 1 is deprecated. The vocabulary '${V:Alpha}' is deprecated. | prefix 'alpha' -> '${CB}/content/${V:Alpha}/versions/1#' |

  @AC_SIMPL-18184_AC2 @D11 @D47 @D4
  Scenario: A reference to a deprecated version with no ACTIVE version left gives the first sentence only
    Given vocabulary "Alpha" exists
    And version 1 of "Alpha" has status "DEPRECATED"
    When "ga-admin" creates vocabulary "Beta" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix alpha: <${CB}/content/${V:Alpha}/versions/1#> .
      @prefix ex: <http://example.org/${rid}/beta#> .
      ex:Concept a skos:Concept ;
        skos:related alpha:Concept .
      """
    Then the response status is 201
    And the validation warnings are exactly
      | code                          | description                                                      | location                                                  |
      | deprecatedVocabularyReference | Referenced internal vocabulary '${V:Alpha}' version 1 is deprecated. | prefix 'alpha' -> '${CB}/content/${V:Alpha}/versions/1#' |

  @AC_SIMPL-18184_AC2 @D12 @D5 @D11 @D47
  Scenario: A reference without a version resolves to the latest version, whatever its status
    Given vocabulary "Alpha" has 2 versions
    And version 2 of "Alpha" has status "DEPRECATED"
    When "ga-admin" creates vocabulary "Beta" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix alpha: <${CB}/content/${V:Alpha}#> .
      @prefix ex: <http://example.org/${rid}/beta#> .
      ex:Concept a skos:Concept ;
        skos:related alpha:Concept .
      """
    Then the response status is 201
    And the validation warnings are exactly
      | code                          | description                                                      | location                                   |
      | deprecatedVocabularyReference | Referenced internal vocabulary '${V:Alpha}' version 2 is deprecated. | prefix 'alpha' -> '${CB}/content/${V:Alpha}#' |

  @D12
  Scenario: References to the vocabulary being versioned are ignored
    Given vocabulary "Alpha" has 2 versions
    When "ga-admin" creates a new version of "Alpha" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix old: <${CB}/content/${V:Alpha}/versions/1#> .
      @prefix future: <${CB}/content/${V:Alpha}/versions/9#> .
      @prefix ex: <http://example.org/${rid}/alpha3#> .
      ex:Concept a skos:Concept ;
        skos:related old:Concept , future:Concept .
      """
    Then the response status is 201
    And the response has no validation warnings

  @D12
  Scenario: References to the vocabulary being created are ignored
    When "ga-admin" creates vocabulary "Beta" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix self: <${CB}/content/${V:Beta}/versions/4#> .
      @prefix ex: <http://example.org/${rid}/beta#> .
      ex:Concept a skos:Concept ;
        skos:related self:Concept .
      """
    Then the response status is 201
    And the response has no validation warnings

  @D57 @D29 @D12
  Scenario: Relative IRIs resolve against the content URL of the new version
    When "ga-admin" creates vocabulary "Beta" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix : <#> .
      :Concept a skos:Concept ;
        skos:prefLabel "Relative"@en .
      """
    Then the response status is 201
    And the response has no validation warnings
    When "ga-admin" downloads version 1 of "Beta"
    Then the response body is the content sent for version 1 of "Beta"

  @AC_SIMPL-18183_AC1 @D10 @D12 @D65
  Scenario: An unregistered external namespace gives a warning and the vocabulary is created
    When "ga-admin" creates vocabulary "Beta" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix unk: <http://unregistered.example.org/${rid}/ns#> .
      @prefix ex: <http://example.org/${rid}/beta#> .
      ex:Concept a skos:Concept ;
        skos:related unk:Thing .
      """
    Then the response status is 201
    And the validation warnings are exactly
      | code                     | location                                                    |
      | externalReferenceSkipped | prefix 'unk' -> 'http://unregistered.example.org/${rid}/ns#' |
    When "ga-admin" gets version 1 of "Beta"
    Then the response status is 200
    And the response field "validationWarnings" is absent

  @D12
  Scenario: W3C core namespaces and the own namespace give no warning
    When "ga-admin" creates vocabulary "Beta" with this content
      """
      @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
      @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <http://example.org/${rid}/beta#> .
      ex:Class a owl:Class ; rdfs:label "Class"@en .
      ex:count a owl:DatatypeProperty ; rdfs:range xsd:integer .
      ex:Concept a skos:Concept ; rdf:value "1"^^xsd:integer .
      """
    Then the response status is 201
    And the response has no validation warnings

  @D12 @D30 @D42
  Scenario: A registered external namespace or alias gives no warning, compared without trailing '#' or '/'
    Given external vocabulary "Dc" is registered with the fields
      | field     | value                                 |
      | aliasUris | http://alias.example.org/${rid}/dc/   |
    When "ga-admin" creates vocabulary "Beta" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix dc: <${NS:Dc}/> .
      @prefix dcalias: <http://alias.example.org/${rid}/dc#> .
      @prefix ex: <http://example.org/${rid}/beta#> .
      ex:Concept a skos:Concept ;
        skos:related dc:title , dcalias:creator .
      """
    Then the response status is 201
    And the response has no validation warnings

  @D10
  Scenario: Warning codes come from the closed set
    Given vocabulary "Alpha" has 2 versions
    When "ga-admin" creates vocabulary "Beta" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix alpha: <${CB}/content/${V:Alpha}/versions/1#> .
      @prefix unk: <http://unregistered.example.org/${rid}/other/> .
      @prefix ex: <http://example.org/${rid}/beta#> .
      ex:Concept a skos:Concept ;
        skos:related alpha:Concept , unk:Thing .
      """
    Then the response status is 201
    And every validation warning code is one of "externalReferenceSkipped, deprecatedVocabularyReference, semanticReasoningSkipped"
    And the validation warnings include
      | code                          |
      | externalReferenceSkipped      |
      | deprecatedVocabularyReference |

  @AC_SIMPL-18181_AC2 @D9 @negative
  Scenario: After a syntax error semantic validation does not run
    # The file breaks S3 on line 3 and Turtle syntax on line 4; only the syntax issue may be returned.
    When "ga-admin" creates vocabulary "Alpha" with the file "syntax_after_semantic.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | in   | name           | detail                              | location                     |
      | body | vocabularyFile | ^Encountered invalid Turtle syntax | ~^line 4(, column [0-9]+)?$ |
    And no vocabulary of this scenario is stored

  @D12 @D10 @D65
  Scenario: A namespace declared with SPARQL-style PREFIX is a vocabulary reference
    When "ga-admin" creates vocabulary "Beta" with this content
      """
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      PREFIX unk: <http://unregistered.example.org/${rid}/sparql#>
      PREFIX ex: <http://example.org/${rid}/beta#>
      ex:Concept a skos:Concept ;
        skos:related unk:Thing .
      """
    Then the response status is 201
    And the validation warnings are exactly
      | code                     | location                                                        |
      | externalReferenceSkipped | prefix 'unk' -> 'http://unregistered.example.org/${rid}/sparql#' |

  @AC_SIMPL-18184_AC2 @D11 @D47 @D65
  Scenario: Each reference to a deprecated version gives its own warning
    Given vocabulary "Alpha" has 2 versions
    And vocabulary "Gamma" has 2 versions
    When "ga-admin" creates vocabulary "Beta" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix alpha: <${CB}/content/${V:Alpha}/versions/1#> .
      @prefix gamma: <${CB}/content/${V:Gamma}/versions/1#> .
      @prefix ex: <http://example.org/${rid}/beta#> .
      ex:Concept a skos:Concept ;
        skos:related alpha:Concept , gamma:Concept .
      """
    Then the response status is 201
    And the validation warnings are exactly
      | code                          | description                                                                                         | location                                                  |
      | deprecatedVocabularyReference | Referenced internal vocabulary '${V:Alpha}' version 1 is deprecated. Use version 2, the latest ACTIVE version. | prefix 'alpha' -> '${CB}/content/${V:Alpha}/versions/1#' |
      | deprecatedVocabularyReference | Referenced internal vocabulary '${V:Gamma}' version 1 is deprecated. Use version 2, the latest ACTIVE version. | prefix 'gamma' -> '${CB}/content/${V:Gamma}/versions/1#' |
