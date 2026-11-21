Feature: Syntax and semantic validation of uploaded content
  Two steps on every create and new version: syntax, then semantic (D9). A syntax error stops
  validation with one issue. Semantic rules S1 to S7 all run and every error is reported (D9, D10).
  Locations follow D65 and D140; the report holds at most 1000 issues (D125).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18194_AC1 @D9 @D10 @D8 @D60 @negative @OWT-A10
  Scenario: A syntax error is reported with its line and nothing is stored
    When I create the vocabulary "Alpha" with the fixture "syntax-error-line3.ttl"
    Then the validation report has one syntax error on line 3
    And no vocabulary of this scenario is stored

  @AC_SIMPL-18181_AC2 @D9 @D8 @negative
  Scenario: A syntax error in a new version creates nothing and deprecates nothing
    Given a vocabulary "Alpha" exists
    And I remember the state of the vocabulary "Alpha"
    When I create a new version of "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      ex:a skos:prefLabel "Fine"@en .
      ex:b skos:prefLabel "Broken"@en
      ex:c skos:prefLabel "No dot before me"@en .
      """
    Then the validation report has one syntax error on line 5
    And the vocabulary "Alpha" is unchanged

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18182_AC1 @D9 @negative
  Scenario: Semantic validation does not run after a syntax error
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      ex:dup skos:prefLabel "One"@en , "Two"@en .
      ex:a ex:b ex:c ex:d .
      """
    Then the validation report has one syntax error on line 4
    And no vocabulary of this scenario is stored

  @D9 @D79 @negative
  Scenario: A binary file is a syntax error
    When I create a vocabulary with the parts
      | part           | value            |
      | name           | {Alpha}          |
      | description    | Some text.       |
      | changelog      | Initial version. |
      | vocabularyFile | binary-file      |
    Then the response is a validation report
    And the problem has exactly one issue
    And no vocabulary of this scenario is stored

  @D9 @D70 @D65 @negative
  Scenario: An empty file is rejected with S6
    When I create a vocabulary with the parts
      | part           | value            |
      | name           | {Alpha}          |
      | description    | Some text.       |
      | changelog      | Initial version. |
      | vocabularyFile | empty-file       |
    Then the validation report has exactly the issues
      | detail                            | location |
      | Vocabulary contains no statements | document |
    And no vocabulary of this scenario is stored

  @D9 @D65 @negative
  Scenario: A file with only prefixes and comments is rejected with S6
    When I create the vocabulary "Alpha" with the content
      """
      # Nothing but declarations.
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      """
    Then the validation report has exactly the issues
      | detail                            | location |
      | Vocabulary contains no statements | document |

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @D9 @D65 @D140 @negative
  Scenario: S1 a literal as the object of an object property
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      ex:hasPart a owl:ObjectProperty .
      ex:thing ex:hasPart "not a resource"@en .
      """
    Then the validation report has exactly the issues
      | detail                            | location                                                                                                          |
      | Literal value for object property | statement 'https://example.org/{Alpha}/ns#thing https://example.org/{Alpha}/ns#hasPart "not a resource"@en' |
    And no vocabulary of this scenario is stored

  @D9 @D65 @D140 @negative
  Scenario: S1 location writes a typed literal in N-Triples form
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
      ex:hasPart a owl:ObjectProperty .
      ex:thing ex:hasPart "42"^^xsd:integer .
      """
    Then the validation report has exactly the issues
      | detail                            | location                                                                                                                                  |
      | Literal value for object property | statement 'https://example.org/{Alpha}/ns#thing https://example.org/{Alpha}/ns#hasPart "42"^^<http://www.w3.org/2001/XMLSchema#integer>' |

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: S2 a resource as the object of a datatype property
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      ex:size a owl:DatatypeProperty .
      ex:thing ex:size ex:other .
      """
    Then the validation report has exactly the issues
      | detail                              | location                                                                                                         |
      | Resource value for datatype property | statement 'https://example.org/{Alpha}/ns#thing https://example.org/{Alpha}/ns#size https://example.org/{Alpha}/ns#other' |

  @AC_SIMPL-18183_AC2 @D9 @D65 @D144 @negative
  Scenario: S3 a property declared both object and datatype property gives one issue
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      ex:prop a owl:DatatypeProperty .
      ex:other a owl:Class .
      ex:prop a owl:ObjectProperty .
      """
    Then the validation report has exactly the issues
      | detail                                                 | location                                       |
      | Property declared as both object and datatype property | resource 'https://example.org/{Alpha}/ns#prop' |

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: S4 two preferred labels in the same language
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      ex:concept skos:prefLabel "One"@en , "Two"@en , "Eins"@de .
      """
    Then the validation report has exactly the issues
      | detail                                          | location                                                         |
      | Duplicate preferred label for the same language | resource 'https://example.org/{Alpha}/ns#concept', language 'en' |

  @D9 @D65 @negative
  Scenario: S4 gives one issue per subject and language
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      ex:concept skos:prefLabel "One"@en , "Two"@en , "Three"@en , "Eins"@de , "Zwei"@de .
      """
    Then the validation report has exactly the issues
      | detail                                          | location                                                         |
      | Duplicate preferred label for the same language | resource 'https://example.org/{Alpha}/ns#concept', language 'en' |
      | Duplicate preferred label for the same language | resource 'https://example.org/{Alpha}/ns#concept', language 'de' |

  @D53 @D65 @negative
  Scenario: S4 two labels without a language tag count as one language
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      ex:concept skos:prefLabel "One" , "Two" .
      """
    Then the validation report has exactly the issues
      | detail                                          | location                                                           |
      | Duplicate preferred label for the same language | resource 'https://example.org/{Alpha}/ns#concept', language 'none' |

  @D53
  Scenario: S4 one label without a tag and one with a tag is valid
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      ex:concept skos:prefLabel "One" , "Two"@en .
      """
    Then the response is the created version 1 of "Alpha"

  @D142 @negative
  Scenario: S4 compares language tags without regard to case
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      ex:concept skos:prefLabel "One"@EN , "Two"@en .
      """
    Then the validation report has exactly the issues
      | detail                                          | location                                                         |
      | Duplicate preferred label for the same language | resource 'https://example.org/{Alpha}/ns#concept', language 'en' |

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: S5 a resource typed with two disjoint classes
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      ex:Cat a owl:Class .
      ex:Dog a owl:Class .
      ex:Cat owl:disjointWith ex:Dog .
      ex:felix a ex:Cat , ex:Dog .
      """
    Then the validation report has exactly the issues
      | detail                                      | location                                        |
      | Resource is an instance of disjoint classes | resource 'https://example.org/{Alpha}/ns#felix' |

  @D143 @negative
  Scenario: S5 gives one issue per resource, whatever the number of disjoint pairs
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      ex:Cat a owl:Class .
      ex:Dog a owl:Class .
      ex:Bird a owl:Class .
      ex:Cat owl:disjointWith ex:Dog , ex:Bird .
      ex:Dog owl:disjointWith ex:Bird .
      ex:chimera a ex:Cat , ex:Dog , ex:Bird .
      """
    Then the validation report has exactly the issues
      | detail                                      | location                                          |
      | Resource is an instance of disjoint classes | resource 'https://example.org/{Alpha}/ns#chimera' |

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18182_AC1 @D9 @D10 @negative
  Scenario: Every semantic error is reported in one response
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      ex:hasPart a owl:ObjectProperty .
      ex:size a owl:DatatypeProperty .
      ex:Cat a owl:Class .
      ex:Dog a owl:Class .
      ex:Cat owl:disjointWith ex:Dog .
      ex:thing ex:hasPart "text"@en ;
        ex:size ex:big .
      ex:felix a ex:Cat , ex:Dog .
      ex:concept skos:prefLabel "One"@en , "Two"@en .
      """
    Then the validation report has exactly the issues
      | detail                                          | location                                                                                                     |
      | Literal value for object property               | statement 'https://example.org/{Alpha}/ns#thing https://example.org/{Alpha}/ns#hasPart "text"@en'          |
      | Resource value for datatype property            | statement 'https://example.org/{Alpha}/ns#thing https://example.org/{Alpha}/ns#size https://example.org/{Alpha}/ns#big' |
      | Resource is an instance of disjoint classes     | resource 'https://example.org/{Alpha}/ns#felix'                                                              |
      | Duplicate preferred label for the same language | resource 'https://example.org/{Alpha}/ns#concept', language 'en'                                             |
    And no vocabulary of this scenario is stored

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @D9 @D8 @negative
  Scenario: Semantic errors in a new version create nothing and deprecate nothing
    Given a vocabulary "Alpha" exists
    And I remember the state of the vocabulary "Alpha"
    When I create a new version of "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      ex:one skos:prefLabel "A"@en , "B"@en .
      ex:two skos:prefLabel "C"@fr , "D"@fr .
      """
    Then the validation report has exactly the issues
      | detail                                          | location                                                     |
      | Duplicate preferred label for the same language | resource 'https://example.org/{Alpha}/ns#one', language 'en' |
      | Duplicate preferred label for the same language | resource 'https://example.org/{Alpha}/ns#two', language 'fr' |
    And the vocabulary "Alpha" is unchanged

  @D10 @negative
  Scenario: A rejected upload returns errors only, not its warnings
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix unreg: <https://unregistered-{tok:lower}.example.org/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      ex:concept skos:prefLabel "One"@en , "Two"@en ;
        unreg:note "uses an unregistered namespace"@en .
      """
    Then the validation report has exactly the issues
      | detail                                          | location                                                         |
      | Duplicate preferred label for the same language | resource 'https://example.org/{Alpha}/ns#concept', language 'en' |

  @AC_SIMPL-18183_AC1 @AC_SIMPL-18182_AC2 @AC_SIMPL-18181_AC1 @AC_SIMPL-18194_AC2 @D9 @D10
  Scenario: Content without errors passes both steps and is created
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
      ex:hasPart a owl:ObjectProperty .
      ex:size a owl:DatatypeProperty .
      ex:Cat a owl:Class .
      ex:Dog a owl:Class .
      ex:Cat owl:disjointWith ex:Dog .
      ex:thing ex:hasPart ex:part ;
        ex:size "12"^^xsd:integer .
      ex:felix a ex:Cat .
      ex:concept skos:prefLabel "One"@en , "Eins"@de .
      """
    Then the response is the created version 1 of "Alpha"
    And the response has no validation warnings

  @D57 @negative
  Scenario: Relative IRIs resolve against the content URL of the new version
    Given a vocabulary "Alpha" exists
    When I create a new version of "Alpha" with the content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      <#dup> skos:prefLabel "One"@en , "Two"@en .
      """
    Then the validation report has exactly the issues
      | detail                                          | location                                                       |
      | Duplicate preferred label for the same language | resource '{base}/content/{Alpha}/versions/2#dup', language 'en' |

  @D125 @Q2 @NFR-04 @negative
  Scenario: The validation report holds at most 1000 issues and says it was truncated
    When I create a vocabulary with the parts
      | part           | value            |
      | name           | {Alpha}          |
      | description    | Some text.       |
      | changelog      | Initial version. |
      | vocabularyFile | s4-errors:1001   |
    Then the validation report has 1000 issues
    And the response JSON has
      | field     | value |
      | truncated | true  |
    And no vocabulary of this scenario is stored

  @D125 @Q2 @negative
  Scenario: A report of exactly 1000 issues is not truncated
    When I create a vocabulary with the parts
      | part           | value            |
      | name           | {Alpha}          |
      | description    | Some text.       |
      | changelog      | Initial version. |
      | vocabularyFile | s4-errors:1000   |
    Then the validation report has 1000 issues
    And the response JSON has
      | field     | value      |
      | truncated | <not true> |
