Feature: Vocabulary references in uploaded content
  A vocabulary reference is a namespace declared with @prefix or PREFIX (D12). Internal when it
  starts with <contentBase>/content/<Name>; external otherwise. A reference to an effectively
  deprecated internal version gives a warning, not an error (D11, D47); a reference to a missing
  internal vocabulary or version is error S7 (D12, D47, D69, D146). Unregistered external
  namespaces give externalReferenceSkipped (D12, D42, D145).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18184_AC1 @D12
  Scenario: References to ACTIVE internal versions give no warning
    Given a vocabulary "Ref" exists with 2 versions
    And a vocabulary "Alpha" exists
    When I create a new version of "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ref: <{base}/content/{Ref}#> .
      @prefix ref2: <{base}/content/{Ref}/versions/2/> .
      ex:concept skos:prefLabel "Uses Ref"@en ;
        skos:related ref:term , ref2:other .
      """
    Then the response is the created version 2 of "Alpha"
    And the response has no validation warnings

  @AC_SIMPL-18184_AC2 @D11 @D47 @D65 @D3
  Scenario: A new version that references a deprecated version is created with a warning
    Given a vocabulary "Ref" exists with 2 versions
    And a vocabulary "Alpha" exists
    When I create a new version of "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ref: <{base}/content/{Ref}/versions/1#> .
      ex:concept skos:related ref:term .
      """
    Then the response is the created version 2 of "Alpha"
    And the response has exactly the validation warnings
      | code                          | description                                                                                         | location                                          |
      | deprecatedVocabularyReference | Referenced internal vocabulary '{Ref}' version 1 is deprecated. Use version 2, the latest ACTIVE version. | prefix 'ref' -> '{base}/content/{Ref}/versions/1#' |
    And version 1 of "Alpha" has the version status "DEPRECATED"

  @AC_SIMPL-18184_AC2 @D11 @D47 @D65
  Scenario: A new vocabulary that references a deprecated version is created with a warning
    Given a vocabulary "Ref" exists with 3 versions
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix old: <{base}/content/{Ref}/versions/2/> .
      ex:concept skos:related old:term .
      """
    Then the response is the created version 1 of "Alpha"
    And the response has exactly the validation warnings
      | code                          | description                                                                                         | location                                          |
      | deprecatedVocabularyReference | Referenced internal vocabulary '{Ref}' version 2 is deprecated. Use version 3, the latest ACTIVE version. | prefix 'old' -> '{base}/content/{Ref}/versions/2/' |

  @D11 @D47 @D3
  Scenario: Without another ACTIVE version no version is recommended
    Given a vocabulary "Ref" exists
    And version 1 of "Ref" has the version status "DEPRECATED"
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ref: <{base}/content/{Ref}/versions/1#> .
      ex:concept skos:related ref:term .
      """
    Then the response is the created version 1 of "Alpha"
    And the response has exactly the validation warnings
      | code                          | description                                                     | location                                          |
      | deprecatedVocabularyReference | Referenced internal vocabulary '{Ref}' version 1 is deprecated. | prefix 'ref' -> '{base}/content/{Ref}/versions/1#' |

  @D11 @D47 @D3 @D5 @D12
  Scenario: A reference to the latest version of a DEPRECATED vocabulary warns that the vocabulary is deprecated
    Given a vocabulary "Ref" exists with 2 versions
    And the vocabulary status of "Ref" is "DEPRECATED"
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ref: <{base}/content/{Ref}#> .
      ex:concept skos:related ref:term .
      """
    Then the response is the created version 1 of "Alpha"
    And the response has exactly the validation warnings
      | code                          | description                                                                                         | location                               |
      | deprecatedVocabularyReference | Referenced internal vocabulary '{Ref}' version 2 is deprecated. The vocabulary '{Ref}' is deprecated. | prefix 'ref' -> '{base}/content/{Ref}#' |

  @D11 @D47 @D3
  Scenario: A reference to an old version of a DEPRECATED vocabulary recommends no version
    Given a vocabulary "Ref" exists with 2 versions
    And the vocabulary status of "Ref" is "DEPRECATED"
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ref: <{base}/content/{Ref}/versions/1#> .
      ex:concept skos:related ref:term .
      """
    Then the response is the created version 1 of "Alpha"
    And the response has exactly the validation warnings
      | code                          | description                                                                                         | location                                          |
      | deprecatedVocabularyReference | Referenced internal vocabulary '{Ref}' version 1 is deprecated. The vocabulary '{Ref}' is deprecated. | prefix 'ref' -> '{base}/content/{Ref}/versions/1#' |

  @AC_SIMPL-18184_AC3 @D12 @D47 @D65 @D8 @negative
  Scenario: A reference to a version that does not exist is error S7
    Given a vocabulary "Ref" exists
    And a vocabulary "Alpha" exists
    And I remember the state of the vocabulary "Alpha"
    When I create a new version of "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ref: <{base}/content/{Ref}/versions/7#> .
      ex:concept skos:related ref:term .
      """
    Then the validation report has exactly the issues
      | detail                                                    | location                                          |
      | Referenced internal vocabulary '{Ref}' version 7 does not exist. | prefix 'ref' -> '{base}/content/{Ref}/versions/7#' |
    And the vocabulary "Alpha" is unchanged

  @AC_SIMPL-18184_AC3 @D12 @D47 @negative
  Scenario: A reference to a vocabulary that does not exist is error S7
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ghost: <{base}/content/{Ghost}#> .
      ex:concept skos:related ghost:term .
      """
    Then the validation report has exactly the issues
      | detail                                                  | location                                   |
      | Referenced internal vocabulary '{Ghost}' does not exist. | prefix 'ghost' -> '{base}/content/{Ghost}#' |
    And no vocabulary of this scenario is stored

  @AC_SIMPL-18184_AC3 @D12 @D47 @negative
  Scenario: A versioned reference to a vocabulary that does not exist is error S7
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ghost: <{base}/content/{Ghost}/versions/1#> .
      ex:concept skos:related ghost:term .
      """
    Then the validation report has exactly the issues
      | detail                                                            | location                                              |
      | Referenced internal vocabulary '{Ghost}' version 1 does not exist. | prefix 'ghost' -> '{base}/content/{Ghost}/versions/1#' |

  @D69 @D47 @negative
  Scenario: A version that is not a positive integer is error S7 with the version as given
    Given a vocabulary "Ref" exists
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ref: <{base}/content/{Ref}/versions/abc#> .
      ex:concept skos:related ref:term .
      """
    Then the validation report has exactly the issues
      | detail                                                      | location                                            |
      | Referenced internal vocabulary '{Ref}' version abc does not exist. | prefix 'ref' -> '{base}/content/{Ref}/versions/abc#' |

  @D146 @D12
  Scenario: Another path under an existing name is a reference to its latest version
    Given a vocabulary "Ref" exists
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ref: <{base}/content/{Ref}/terms/> .
      ex:concept skos:related ref:term .
      """
    Then the response is the created version 1 of "Alpha"
    And the response has no validation warnings

  @D146 @D47 @negative
  Scenario: Another path under an unknown name is error S7 without a version
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ghost: <{base}/content/{Ghost}/terms/> .
      ex:concept skos:related ghost:term .
      """
    Then the validation report has exactly the issues
      | detail                                                  | location                                         |
      | Referenced internal vocabulary '{Ghost}' does not exist. | prefix 'ghost' -> '{base}/content/{Ghost}/terms/' |

  @D69 @D12
  Scenario Outline: A content URL without a valid name is an external reference: <case>
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix odd: <<namespace>> .
      ex:concept skos:related odd:term .
      """
    Then the response is the created version 1 of "Alpha"
    And the response has exactly the validation warnings
      | code                     | location                       |
      | externalReferenceSkipped | prefix 'odd' -> '<namespace>' |

    Examples:
      | case                  | namespace                         |
      | no name               | {base}/content/                   |
      | name in lower case    | {base}/content/lower{tok}/        |

  @D12
  Scenario: A reference to the vocabulary being versioned is ignored
    Given a vocabulary "Alpha" exists
    When I create a new version of "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix self: <{base}/content/{Alpha}/versions/1#> .
      ex:concept skos:related self:term .
      """
    Then the response is the created version 2 of "Alpha"
    And the response has no validation warnings

  @D12
  Scenario: A reference to the vocabulary being created is ignored
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix self: <{base}/content/{Alpha}#> .
      ex:concept skos:related self:term .
      """
    Then the response is the created version 1 of "Alpha"
    And the response has no validation warnings

  @D12 @D10 @D65 @AC_SIMPL-18183_AC1
  Scenario: An unregistered external namespace gives externalReferenceSkipped and does not block
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ext: <https://unregistered-{tok:lower}.example.org/ns#> .
      ex:concept skos:related ext:term .
      """
    Then the response is the created version 1 of "Alpha"
    And the response has exactly the validation warnings
      | code                     | location                                                         |
      | externalReferenceSkipped | prefix 'ext' -> 'https://unregistered-{tok:lower}.example.org/ns#' |
    And every validation warning has a code from the closed set

  @D12 @D10
  Scenario: The W3C namespaces and the vocabulary's own namespace give no warning
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
      @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      ex:Thing rdf:type owl:Class ;
        rdfs:label "Thing"@en ;
        skos:note "12"^^xsd:string .
      """
    Then the response is the created version 1 of "Alpha"
    And the response has no validation warnings

  @D12 @D30 @D42
  Scenario Outline: A namespace that matches a registered external vocabulary gives no warning: <case>
    Given an external vocabulary "Ext" is registered with the namespace "https://ext-{tok:lower}.example.org/ns" and the aliases "https://alias-{tok:lower}.example.org/terms/"
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix e: <<namespace>> .
      ex:concept skos:related e:term .
      """
    Then the response is the created version 1 of "Alpha"
    And the response has no validation warnings

    Examples:
      | case                         | namespace                                     |
      | namespace with '#'           | https://ext-{tok:lower}.example.org/ns#       |
      | namespace with '/'           | https://ext-{tok:lower}.example.org/ns/       |
      | alias as registered          | https://alias-{tok:lower}.example.org/terms/  |
      | alias with '#' instead of '/' | https://alias-{tok:lower}.example.org/terms# |

  @D145 @D12
  Scenario: A namespace declared under two prefixes gives one warning at its first declaration
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix first: <https://twice-{tok:lower}.example.org/ns#> .
      @prefix second: <https://twice-{tok:lower}.example.org/ns#> .
      ex:concept skos:related first:a , second:b .
      """
    Then the response is the created version 1 of "Alpha"
    And the response has exactly the validation warnings
      | code                     | location                                                      |
      | externalReferenceSkipped | prefix 'first' -> 'https://twice-{tok:lower}.example.org/ns#' |

  @D12
  Scenario: A SPARQL-style PREFIX declaration is a reference too
    When I create the vocabulary "Alpha" with the content
      """
      PREFIX ex: <https://example.org/{name}/ns#>
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      PREFIX ext: <https://sparql-{tok:lower}.example.org/ns#>
      ex:concept skos:related ext:term .
      """
    Then the response is the created version 1 of "Alpha"
    And the response has exactly the validation warnings
      | code                     | location                                                    |
      | externalReferenceSkipped | prefix 'ext' -> 'https://sparql-{tok:lower}.example.org/ns#' |

  @D42 @D12
  Scenario: Registering the external vocabulary later clears the warning in a new version
    When I create the vocabulary "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ext: <https://later-{tok:lower}.example.org/ns#> .
      ex:concept skos:related ext:term .
      """
    Then the response is the created version 1 of "Alpha"
    And the response has exactly the validation warnings
      | code                     | location                                                   |
      | externalReferenceSkipped | prefix 'ext' -> 'https://later-{tok:lower}.example.org/ns#' |
    Given an external vocabulary "Ext" is registered with the namespace "https://later-{tok:lower}.example.org/ns"
    When I create a new version of "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ext: <https://later-{tok:lower}.example.org/ns#> .
      ex:concept skos:related ext:term , ext:other .
      """
    Then the response is the created version 2 of "Alpha"
    And the response has no validation warnings

  @D42 @D41 @D12
  Scenario: Removing a referenced external vocabulary keeps stored versions and warns on later uploads
    Given an external vocabulary "Ext" is registered with the namespace "https://removed-{tok:lower}.example.org/ns"
    And a vocabulary "Alpha" exists with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ext: <https://removed-{tok:lower}.example.org/ns#> .
      ex:concept skos:related ext:term .
      """
    When I send DELETE "/external-vocabularies/{Ext}"
    Then the response status is 204
    And version 1 of "Alpha" has the version status "ACTIVE"
    When I send GET "/content/{Alpha}/versions/1"
    Then the response body is the uploaded content of version 1 of "Alpha"
    When I create a new version of "Alpha" with the content
      """
      @prefix ex: <https://example.org/{name}/ns#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ext: <https://removed-{tok:lower}.example.org/ns#> .
      ex:concept skos:related ext:term , ext:more .
      """
    Then the response is the created version 2 of "Alpha"
    And the response has exactly the validation warnings
      | code                     | location                                                     |
      | externalReferenceSkipped | prefix 'ext' -> 'https://removed-{tok:lower}.example.org/ns#' |
