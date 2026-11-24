Feature: Syntax and semantic validation of uploaded vocabulary content
  Every POST /vocabularies and POST .../versions runs syntax validation (Turtle), then semantic
  validation (rules S1 to S7, all errors in one response) (D9, D10, D65). The validation report is the
  400 VocabularyValidationProblem (D14). Nothing is stored on failure (D8).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC1 @AC_SIMPL-18194_AC2 @AC_SIMPL-18187_AC1 @D9 @D10 @D14
  Scenario: Valid content passes both steps and the creation confirms it without error details
    Given the vocabulary file is:
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/$RUN/ok#> .
      ex:hasPart a owl:ObjectProperty .
      ex:size a owl:DatatypeProperty .
      ex:Cat a owl:Class . ex:Dog a owl:Class . ex:Cat owl:disjointWith ex:Dog .
      ex:felix a ex:Cat ; ex:hasPart ex:tail ; ex:size "3"^^xsd:integer ;
        skos:prefLabel "Felix"@en , "Félix"@fr , "Felix" .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response field "issues" is absent
    And the response has no validation warnings
    And vocabulary "$Alpha" has 1 version

  @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC1 @AC_SIMPL-18194_AC2 @D9
  Scenario: Valid content passes validation on a new version too
    Given a vocabulary "$Alpha" exists
    When I create a new version of "$Alpha"
    Then the response status is 201
    And the response has no validation warnings

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18194_AC1 @D9 @D65 @D8 @negative
  Scenario: A syntax error on creation is reported with its line and nothing is stored
    Given the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/$RUN/syn#> .
      ex:Scheme skos:prefLabel .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem type is "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And every issue is in "body" named "vocabularyFile" with type "urn:problem-type:belgif:input-validation:invalidInput"
    And issue 1 has a detail starting with "Encountered invalid Turtle syntax"
    And issue 1 has a location matching "line 3(, column \d+)?"
    And 0 vocabularies have a name containing "$RUN"

  @AC_SIMPL-18181_AC2 @D9 @D8 @negative
  Scenario: A syntax error on a new version creates no version and deprecates nothing
    Given a vocabulary "$Alpha" exists
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/$RUN/syn#> .

      ex:Scheme a skos:ConceptScheme
      ex:Other a skos:Concept .
      """
    When I create a new version of "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 has a detail starting with "Encountered invalid Turtle syntax"
    And issue 1 has a location matching "line 5(, column \d+)?"
    And vocabulary "$Alpha" has 1 version
    And version 1 of vocabulary "$Alpha" has version status "ACTIVE"
    And vocabulary "$Alpha" has latest version 1

  @AC_SIMPL-18181_AC2 @D9 @negative
  Scenario: Semantic validation does not run after a syntax error
    Given the vocabulary file is:
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <https://example.org/$RUN/syn#> .
      ex:hasPart a owl:ObjectProperty .
      ex:car ex:hasPart "wheel" .
      ex:car ex:hasPart ex:door ex:window .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 has a detail starting with "Encountered invalid Turtle syntax"
    And issue 1 has a location matching "line 5(, column \d+)?"

  @D9 @negative
  Scenario: Only the first syntax error is reported
    Given the vocabulary file is:
      """
      @prefix ex: <https://example.org/$RUN/syn#> .
      ex:a ex:b .
      ex:c ex:d .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 has a location matching "line 2(, column \d+)?"

  @D9 @D15 @negative
  Scenario: A prefix that is not declared is a syntax error
    Given the vocabulary file is:
      """
      @prefix ex: <https://example.org/$RUN/syn#> .
      ex:a undeclared:b ex:c .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 has a detail starting with "Encountered invalid Turtle syntax"
    And issue 1 has a location matching "line 2(, column \d+)?"

  @D9 @D15 @negative
  Scenario: RDF/XML is not accepted
    Given the vocabulary file is:
      """
      <?xml version="1.0"?>
      <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description rdf:about="https://example.org/a"/>
      </rdf:RDF>
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 has a detail starting with "Encountered invalid Turtle syntax"
    And 0 vocabularies have a name containing "$RUN"

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @D9 @D65 @D140 @D181 @negative
  Scenario: S1 a literal as the object of an object property
    Given the vocabulary file is:
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <https://example.org/$RUN/s1#> .
      ex:hasPart a owl:ObjectProperty .
      ex:car ex:hasPart "wheel" .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And every issue is in "body" named "vocabularyFile" with type "urn:problem-type:belgif:input-validation:invalidInput"
    And the problem has an issue with detail "Literal value for object property" and the location:
      """
      statement '<https://example.org/$RUN/s1#car> <https://example.org/$RUN/s1#hasPart> "wheel"'
      """
    And 0 vocabularies have a name containing "$RUN"

  @D140 @D181 @D65 @negative
  Scenario: S1 locations write the language tag and the datatype of the literal
    Given the vocabulary file is:
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
      @prefix ex: <https://example.org/$RUN/s1#> .
      ex:hasPart a owl:ObjectProperty .
      ex:car ex:hasPart "wheel"@en .
      ex:bike ex:hasPart "2"^^xsd:integer .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 2 issues
    And the problem has an issue with detail "Literal value for object property" and the location:
      """
      statement '<https://example.org/$RUN/s1#car> <https://example.org/$RUN/s1#hasPart> "wheel"@en'
      """
    And the problem has an issue with detail "Literal value for object property" and the location:
      """
      statement '<https://example.org/$RUN/s1#bike> <https://example.org/$RUN/s1#hasPart> "2"^^<http://www.w3.org/2001/XMLSchema#integer>'
      """

  @AC_SIMPL-18183_AC2 @D9 @D65 @D181 @negative
  Scenario: S2 a resource as the object of a datatype property
    Given the vocabulary file is:
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <https://example.org/$RUN/s2#> .
      ex:size a owl:DatatypeProperty .
      ex:car ex:size ex:big .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And the problem has an issue with detail "Resource value for datatype property" and the location:
      """
      statement '<https://example.org/$RUN/s2#car> <https://example.org/$RUN/s2#size> <https://example.org/$RUN/s2#big>'
      """

  @D9 @D199 @negative
  Scenario: S2 with a labelled blank node writes the label from the file
    Given the vocabulary file is:
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <https://example.org/$RUN/s2#> .
      ex:size a owl:DatatypeProperty .
      ex:car ex:size _:measure .
      _:measure ex:unit "cm" .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And the problem has an issue with detail "Resource value for datatype property" and the location:
      """
      statement '<https://example.org/$RUN/s2#car> <https://example.org/$RUN/s2#size> _:measure'
      """

  @D9 @D199 @negative
  Scenario: S2 with an anonymous blank node writes []
    Given the vocabulary file is:
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <https://example.org/$RUN/s2#> .
      ex:size a owl:DatatypeProperty .
      ex:car ex:size [ ex:unit "cm" ] .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And the problem has an issue with detail "Resource value for datatype property" and the location:
      """
      statement '<https://example.org/$RUN/s2#car> <https://example.org/$RUN/s2#size> []'
      """

  @AC_SIMPL-18183_AC2 @D9 @D65 @D144 @negative
  Scenario: S3 a property declared both object and datatype property gives one issue per property
    Given the vocabulary file is:
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <https://example.org/$RUN/s3#> .
      ex:mixed a owl:DatatypeProperty .
      ex:other a owl:Class .
      ex:mixed a owl:ObjectProperty .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 has detail "Property declared as both object and datatype property"
    And issue 1 has a location matching "resource '<?https://example\.org/$RUN/s3#mixed>?'"

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: S4 two preferred labels in the same language on one subject
    Given the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/$RUN/s4#> .
      ex:term skos:prefLabel "Car"@en , "Automobile"@en , "Auto"@de .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 has detail "Duplicate preferred label for the same language"
    And issue 1 has a location matching "resource '<?https://example\.org/$RUN/s4#term>?', language 'en'"

  @D142 @D65 @negative
  Scenario: S4 compares language tags ignoring case and writes them in lower case
    Given the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/$RUN/s4#> .
      ex:term skos:prefLabel "Car"@EN , "Automobile"@en .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 has detail "Duplicate preferred label for the same language"
    And issue 1 has a location matching "resource '<?https://example\.org/$RUN/s4#term>?', language 'en'"

  @D53 @D65 @negative
  Scenario: S4 two labels without a language tag count as one language
    Given the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/$RUN/s4#> .
      ex:term skos:prefLabel "Car" , "Automobile" .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 has detail "Duplicate preferred label for the same language"
    And issue 1 has a location matching "resource '<?https://example\.org/$RUN/s4#term>?', language 'none'"

  @D53
  Scenario: S4 one label without a tag and one with a tag are valid
    Given the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/$RUN/s4#> .
      ex:term skos:prefLabel "Car" , "Car"@en .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201

  @D65 @negative
  Scenario: S4 gives one issue per subject and language
    Given the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/$RUN/s4#> .
      ex:term skos:prefLabel "Car"@en , "Automobile"@en , "Auto"@de , "Wagen"@de .
      ex:other skos:prefLabel "One"@en , "Two"@en , "Three"@en .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 3 issues with detail "Duplicate preferred label for the same language"
    And the problem has 3 issues

  @AC_SIMPL-18183_AC2 @D9 @D65 @D143 @negative
  Scenario: S5 a resource typed with disjoint classes gives one issue per resource
    Given the vocabulary file is:
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <https://example.org/$RUN/s5#> .
      ex:Cat a owl:Class . ex:Dog a owl:Class . ex:Bird a owl:Class .
      ex:Cat owl:disjointWith ex:Dog , ex:Bird .
      ex:Dog owl:disjointWith ex:Bird .
      ex:chimera a ex:Cat , ex:Dog , ex:Bird .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 has detail "Resource is an instance of disjoint classes"
    And issue 1 has a location matching "resource '<?https://example\.org/$RUN/s5#chimera>?'"

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario Outline: S6 content without statements: <case>
    Given the vocabulary file is:
      """
      <content>
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 has detail "Vocabulary contains no statements"
    And issue 1 has location "document"
    And issue 1 is in "body" named "vocabularyFile"
    And 0 vocabularies have a name containing "$RUN"

    Examples:
      | case               | content                                                     |
      | only a prefix      | @prefix ex: <https://example.org/empty#> .                  |
      | only a comment     | # nothing here                                              |
      | only white space   |                                                             |

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18180_AC7 @D9 @D10 @negative
  Scenario: All semantic errors are returned in one response
    Given the vocabulary file is:
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/$RUN/all#> .
      ex:hasPart a owl:ObjectProperty .
      ex:size a owl:DatatypeProperty .
      ex:both a owl:ObjectProperty , owl:DatatypeProperty .
      ex:Cat a owl:Class . ex:Dog a owl:Class . ex:Cat owl:disjointWith ex:Dog .
      ex:car ex:hasPart "wheel" ; ex:size ex:big .
      ex:term skos:prefLabel "A"@en , "B"@en .
      ex:rex a ex:Cat , ex:Dog .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 5 issues
    And every issue is in "body" named "vocabularyFile" with type "urn:problem-type:belgif:input-validation:invalidInput"
    And the problem has an issue with detail "Literal value for object property"
    And the problem has an issue with detail "Resource value for datatype property"
    And the problem has an issue with detail "Property declared as both object and datatype property"
    And the problem has an issue with detail "Duplicate preferred label for the same language"
    And the problem has an issue with detail "Resource is an instance of disjoint classes"
    And 0 vocabularies have a name containing "$RUN"

  @AC_SIMPL-18183_AC2 @D9 @D8 @negative
  Scenario: Semantic errors on a new version create no version and deprecate nothing
    Given a vocabulary "$Alpha" exists
    And the vocabulary file is:
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <https://example.org/$RUN/v#> .
      ex:hasPart a owl:ObjectProperty .
      ex:car ex:hasPart "wheel" .
      """
    When I create a new version of "$Alpha"
    Then the response is a problem with status 400
    And the problem has an issue with detail "Literal value for object property"
    And vocabulary "$Alpha" has 1 version
    And version 1 of vocabulary "$Alpha" has version status "ACTIVE"

  @D125 @Q2 @NFR-04 @negative
  Scenario: A validation report holds at most 1000 issues and says it was truncated
    Given the vocabulary file has 1001 subjects with a duplicate English label
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1000 issues
    And the response field "truncated" equals true
    And 0 vocabularies have a name containing "$RUN"

  @D125 @Q2 @NFR-04 @negative
  Scenario: A validation report of exactly 1000 issues is not truncated
    Given the vocabulary file has 1000 subjects with a duplicate English label
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1000 issues
    And the response field "truncated" is absent or false

  @D10 @D12 @negative
  Scenario: A failed creation with an unregistered namespace reports only the error
    Given the vocabulary file is:
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix dct: <$EXT/terms/> .
      @prefix ex: <https://example.org/$RUN/w#> .
      ex:hasPart a owl:ObjectProperty .
      ex:car ex:hasPart "wheel" ; dct:title "Car" .
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
