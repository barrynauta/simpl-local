Feature: Syntax and semantic validation of uploaded vocabulary content
  Every POST /vocabularies and POST .../versions validates the file: syntax first, then the
  semantic rules S1 to S7. Every error blocks and nothing is stored (D8, D9, D10, D65).

  Background:
    Given I am a vocabulary administrator

  @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC1 @AC_SIMPL-18182_AC1 @AC_SIMPL-18182_AC2 @AC_SIMPL-18194_AC2 @D9 @D10 @D14
  Scenario: Content with valid syntax and semantics passes and the vocabulary is created
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
      @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      ex:hasPart a owl:ObjectProperty .
      ex:age a owl:DatatypeProperty .
      ex:Cat a owl:Class ; owl:disjointWith ex:Dog .
      ex:Dog a owl:Class .
      ex:Felix a ex:Cat ; ex:hasPart ex:Tail ; ex:age "5"^^xsd:integer ;
          skos:prefLabel "Felix"@en , "Félix"@fr , "Felix" .
      """
    When I submit the request
    Then the response status is 201
    And the response has no validation warnings
    And the response field "status" is "ACTIVE"
    And the response field "issues" is absent

  @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC1 @AC_SIMPL-18194_AC2 @D9
  Scenario: A new version with valid content passes validation
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    When I submit the request
    Then the response status is 201
    And the response field "version" is 2
    And the response has no validation warnings

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18170_AC3 @D9 @D10 @D65 @D8 @negative
  Scenario: A syntax error is rejected with one issue at its line
    Given a create vocabulary request for "A"
    And the request file is the fixture "syntax-error.ttl"
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And every issue is in "body" named "vocabularyFile" with type "urn:problem-type:belgif:input-validation:invalidInput"
    And the problem has an issue with detail starting "Encountered invalid Turtle syntax" and location matching "line 3(, column \d+)?"
    And no vocabulary "A" is stored

  @AC_SIMPL-18181_AC2 @D9 @D60 @negative
  Scenario: Only the first syntax error is reported
    Given a create vocabulary request for "A"
    And the request file is the fixture "two-syntax-errors.ttl"
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue with detail starting "Encountered invalid Turtle syntax" and location matching "line 4(, column \d+)?"

  @AC_SIMPL-18181_AC2 @D9 @negative
  Scenario: Semantic validation does not run when the syntax is invalid
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <${NS:A}> .
      ex:hasPart a owl:ObjectProperty .
      ex:Thing1 ex:hasPart "literal value" .
      ex:Thing2 ex:hasPart .
      """
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue with detail starting "Encountered invalid Turtle syntax" and location matching "line 5(, column \d+)?"
    And no vocabulary "A" is stored

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18173_AC2 @D8 @D9 @negative
  Scenario: A new version with a syntax error creates nothing and deprecates nothing
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request file is the fixture "syntax-error.ttl"
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue with detail starting "Encountered invalid Turtle syntax" and location matching "line 3(, column \d+)?"
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has status "ACTIVE"

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18182_AC3 @AC_SIMPL-18194_AC1 @D9 @D65 @D140 @negative
  Scenario Outline: Rule S1, a literal as object of an object property: <literal>
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
      @prefix ex: <${NS:A}> .
      ex:hasPart a owl:ObjectProperty .
      ex:Thing1 ex:hasPart <literal> .
      """
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And every issue is in "body" named "vocabularyFile" with type "urn:problem-type:belgif:input-validation:invalidInput"
    And the problem has an issue with detail "Literal value for object property" and location "statement '${NS:A}Thing1 ${NS:A}hasPart <expected>'"
    And no vocabulary "A" is stored

    Examples:
      | literal          | expected                                                 |
      | "plain text"    | \"plain text\"                                           |
      | "chat"@fr       | \"chat\"@fr                                              |
      | "5"^^xsd:integer | \"5\"^^<http://www.w3.org/2001/XMLSchema#integer>       |

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @D9 @D65 @negative
  Scenario: Rule S2, a resource as object of a datatype property
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <${NS:A}> .
      ex:age a owl:DatatypeProperty .
      ex:Thing1 ex:age ex:Thing2 .
      """
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue with detail "Resource value for datatype property" and location "statement '${NS:A}Thing1 ${NS:A}age ${NS:A}Thing2'"
    And no vocabulary "A" is stored

  @AC_SIMPL-18183_AC2 @D9 @negative
  Scenario: Rule S2 also applies to a blank node object
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <${NS:A}> .
      ex:age a owl:DatatypeProperty .
      ex:Thing1 ex:age [ ex:value "5" ] .
      """
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue with detail "Resource value for datatype property"
    And no vocabulary "A" is stored

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: Rule S3, a property declared both object and datatype property
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <${NS:A}> .
      ex:mixed a owl:ObjectProperty , owl:DatatypeProperty .
      """
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue with detail "Property declared as both object and datatype property" and location "resource '${NS:A}mixed'"
    And no vocabulary "A" is stored

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: Rule S4, two preferred labels with the same language tag
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      ex:Concept1 a skos:Concept ;
          skos:prefLabel "First"@en , "Second"@en , "Premier"@fr .
      """
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue with detail "Duplicate preferred label for the same language" and location "resource '${NS:A}Concept1', language 'en'"
    And no vocabulary "A" is stored

  @AC_SIMPL-18183_AC2 @D53 @D65 @negative
  Scenario: Rule S4, two preferred labels without a language tag
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      ex:Concept1 a skos:Concept ;
          skos:prefLabel "First" , "Second" .
      """
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue with detail "Duplicate preferred label for the same language" and location "resource '${NS:A}Concept1', language 'none'"
    And no vocabulary "A" is stored

  @AC_SIMPL-18183_AC1 @D53
  Scenario: One untagged and one tagged preferred label are allowed
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      ex:Concept1 a skos:Concept ;
          skos:prefLabel "Label" , "Label"@en .
      """
    When I submit the request
    Then the response status is 201
    And the response has no validation warnings

  @AC_SIMPL-18183_AC2 @D65 @negative
  Scenario: Rule S4 gives one issue per subject and language
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      ex:Concept1 skos:prefLabel "One"@en , "Two"@en , "Three"@en , "Un"@fr , "Deux"@fr .
      ex:Concept2 skos:prefLabel "One"@en , "Two"@en .
      """
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 3 issues
    And the problem has an issue with detail "Duplicate preferred label for the same language" and location "resource '${NS:A}Concept1', language 'en'"
    And the problem has an issue with detail "Duplicate preferred label for the same language" and location "resource '${NS:A}Concept1', language 'fr'"
    And the problem has an issue with detail "Duplicate preferred label for the same language" and location "resource '${NS:A}Concept2', language 'en'"

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: Rule S5, a resource typed with two disjoint classes
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <${NS:A}> .
      ex:Cat a owl:Class ; owl:disjointWith ex:Dog .
      ex:Dog a owl:Class .
      ex:Felix a ex:Cat , ex:Dog .
      """
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue with detail "Resource is an instance of disjoint classes" and location "resource '${NS:A}Felix'"
    And no vocabulary "A" is stored

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: Rule S6, a file with prefixes and comments but no statements
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      # nothing else
      """
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue with detail "Vocabulary contains no statements" and location "document"
    And no vocabulary "A" is stored

  @D70 @D9 @negative
  Scenario: Rule S6, an empty file
    Given a create vocabulary request for "A"
    And the request file is empty
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue with detail "Vocabulary contains no statements" and location "document"
    And no vocabulary "A" is stored

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18182_AC3 @D9 @D10 @negative
  Scenario: All semantic errors are returned in one response
    Given a create vocabulary request for "A"
    And the request file is
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      ex:hasPart a owl:ObjectProperty .
      ex:Thing1 ex:hasPart "literal value" .
      ex:mixed a owl:ObjectProperty , owl:DatatypeProperty .
      ex:Concept1 skos:prefLabel "One"@en , "Two"@en .
      ex:Cat a owl:Class ; owl:disjointWith ex:Dog .
      ex:Felix a ex:Cat , ex:Dog .
      """
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 4 issues
    And every issue is in "body" named "vocabularyFile" with type "urn:problem-type:belgif:input-validation:invalidInput"
    And the problem has an issue with detail "Literal value for object property" and location "statement '${NS:A}Thing1 ${NS:A}hasPart \"literal value\"'"
    And the problem has an issue with detail "Property declared as both object and datatype property" and location "resource '${NS:A}mixed'"
    And the problem has an issue with detail "Duplicate preferred label for the same language" and location "resource '${NS:A}Concept1', language 'en'"
    And the problem has an issue with detail "Resource is an instance of disjoint classes" and location "resource '${NS:A}Felix'"
    And no vocabulary "A" is stored

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18173_AC2 @D8 @D9 @negative
  Scenario: A new version with semantic errors creates nothing and deprecates nothing
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request file is
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <${NS:A}> .
      ex:mixed a owl:ObjectProperty , owl:DatatypeProperty .
      ex:Cat a owl:Class ; owl:disjointWith ex:Dog .
      ex:Felix a ex:Cat , ex:Dog .
      """
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 2 issues
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has status "ACTIVE"

  @D57
  Scenario: Relative IRIs resolve against the content URL of the new version
    Given a vocabulary "Base" exists
    And vocabulary "Base" has a new version
    And a create vocabulary request for "A"
    And the request file is
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix prev: <../../${Base}/versions/1#> .
      @prefix : <#> .
      :Scheme a skos:ConceptScheme ; skos:prefLabel "Relative"@en .
      <#Concept> a skos:Concept ; skos:inScheme :Scheme ; skos:related prev:Concept1 .
      """
    When I submit the request
    Then the response status is 201
    And the response has exactly 1 validation warnings
    And the response field "validationWarnings[0].code" is "deprecatedVocabularyReference"
    When I send a GET request to "/content/${A}/versions/1"
    Then the response body equals the uploaded file

  @D125 @NFR-04 @negative
  Scenario: The validation report holds at most 1000 issues and says when it is truncated
    Given a create vocabulary request for "A"
    And the request file is the generated Turtle with 1001 duplicate label errors
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1000 issues
    And the problem has an issue with location "resource '${NS:A}c00000', language 'en'"
    And the problem has an issue with location "resource '${NS:A}c00999', language 'en'"
    And the problem has no issue with location "resource '${NS:A}c01000', language 'en'"
    And the response field "truncated" is true
    And no vocabulary "A" is stored

  @D125
  Scenario: A validation report with exactly 1000 issues is complete
    Given a create vocabulary request for "A"
    And the request file is the generated Turtle with 1000 duplicate label errors
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1000 issues
    And the response field "truncated" is absent or false
