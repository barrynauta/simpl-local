Feature: Check of referenced internal vocabulary versions
  A namespace declared with @prefix that starts with the content URL of an internal
  vocabulary is a vocabulary reference (D12). A reference to an effectively deprecated
  version gives a warning and does not block (D11, D47, D3).

  Background:
    Given I am a Governance Authority user with role "GA_VOCABULARY_ADMIN"

  @AC_SIMPL-18184_AC1 @D11 @D12 @D10
  Scenario: References to ACTIVE versions pass without a deprecated-version warning
    Given a vocabulary "{Base}" exists
    And a vocabulary "{Other}" exists
    And a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix base: <{content:Base:1}#> .
      @prefix other: <{content:Other}/> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme"@en ;
        skos:related base:Something , other:Else .
      """
    Then the response status is 201
    And the response field "version" equals 2
    And the response field "validationWarnings" equals []

  @AC_SIMPL-18184_AC2 @D11 @D47 @D12 @D4 @D10
  Scenario: A reference to a deprecated version gives a warning that names the latest ACTIVE version
    Given a vocabulary "{Base}" exists with 2 versions
    And a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix legacy: <{content:Base:1}#> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme"@en ;
        skos:related legacy:Something .
      """
    Then the response status is 201
    And the response field "version" equals 2
    And the response field "status" is "ACTIVE"
    And the response field "validationWarnings[0].code" is "deprecatedVocabularyReference"
    And the response field "validationWarnings[0].description" is "Referenced internal vocabulary '{Base}' version 1 is deprecated. Use version 2, the latest ACTIVE version."
    And the response field "validationWarnings[0].location" is "prefix 'legacy' -> '{content:Base:1}#'"
    And the response field "validationWarnings[1]" is absent
    And version 1 of "{Base}" has status "DEPRECATED"
    And version 2 of "{Base}" has status "ACTIVE"
    And the vocabulary "{Base}" has 2 versions

  @AC_SIMPL-18184_AC2 @D11 @D47 @D10
  Scenario: Each deprecated reference gives its own warning
    Given a vocabulary "{Base}" exists with 3 versions
    And a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix one: <{content:Base:1}#> .
      @prefix two: <{content:Base:2}/> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme"@en ;
        skos:related one:Something , two:Else .
      """
    Then the response status is 201
    And the response field "validationWarnings[0].code" is "deprecatedVocabularyReference"
    And the response field "validationWarnings[1].code" is "deprecatedVocabularyReference"
    And the response field "validationWarnings[2]" is absent

  @AC_SIMPL-18184_AC2 @D11 @D47 @D3
  Scenario: A version of a DEPRECATED vocabulary is effectively deprecated and no version is recommended
    Given a vocabulary "{Base}" exists
    And the vocabulary "{Base}" has status "DEPRECATED"
    And a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix base: <{content:Base:1}#> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme"@en ;
        skos:related base:Something .
      """
    Then the response status is 201
    And the response field "validationWarnings[0].code" is "deprecatedVocabularyReference"
    And the response field "validationWarnings[0].description" is "Referenced internal vocabulary '{Base}' version 1 is deprecated. The vocabulary '{Base}' is deprecated."
    And version 1 of "{Base}" has status "ACTIVE"

  @AC_SIMPL-18184_AC2 @D11 @D47 @D4
  Scenario: Without another ACTIVE version the warning recommends nothing
    Given a vocabulary "{Base}" exists
    And version 1 of "{Base}" has status "DEPRECATED"
    And a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix base: <{content:Base:1}#> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme"@en ;
        skos:related base:Something .
      """
    Then the response status is 201
    And the response field "validationWarnings[0].description" is "Referenced internal vocabulary '{Base}' version 1 is deprecated."

  @AC_SIMPL-18184_AC2 @D11 @D47 @D5
  Scenario: A reference without a version to a vocabulary whose latest version is deprecated warns
    Given a vocabulary "{Base}" exists with 2 versions
    And version 2 of "{Base}" has status "DEPRECATED"
    And version 1 of "{Base}" has status "ACTIVE"
    When I create the vocabulary "{Alpha}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix base: <{content:Base}#> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme"@en ;
        skos:related base:Something .
      """
    Then the response status is 201
    And the response field "validationWarnings[0].code" is "deprecatedVocabularyReference"
    And the response field "validationWarnings[0].description" is "Referenced internal vocabulary '{Base}' version 2 is deprecated. Use version 1, the latest ACTIVE version."
    And the response field "validationWarnings[0].location" is "prefix 'base' -> '{content:Base}#'"

  @AC_SIMPL-18184_AC2 @D11 @D10
  Scenario: The deprecated-reference check also runs when a vocabulary is created
    Given a vocabulary "{Base}" exists with 2 versions
    When I create the vocabulary "{Alpha}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix legacy: <{content:Base:1}#> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme"@en ;
        skos:related legacy:Something .
      """
    Then the response status is 201
    And the response field "version" equals 1
    And the response field "validationWarnings[0].code" is "deprecatedVocabularyReference"
