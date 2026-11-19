Feature: References to internal vocabularies
  A namespace declared with @prefix that starts with <contentBase>/content/<Name>
  or <contentBase>/content/<Name>/versions/<n> is an internal reference. A
  reference to a missing vocabulary or version is error S7. A reference to an
  effectively deprecated version is a warning, never an error (D11).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18184_AC1 @D11 @D12
  Scenario: A new version that references ACTIVE internal versions gets no deprecation warning
    Given a vocabulary "Ref" exists
    And a vocabulary "Main" exists
    When I create a new version of "Main" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}/main#> .
      @prefix ref: <${contentBase}/content/${Ref}/versions/1#> .
      @prefix latest: <${contentBase}/content/${Ref}/> .
      ex:Thing a skos:Concept ;
        skos:related ref:Something , latest:Other .
      """
    Then the response status is 201
    And the response has no validation warnings

  @AC_SIMPL-18184_AC2 @AC_SIMPL-18184_AC4 @D3 @D11 @D47 @D65 @D14
  Scenario: A reference to a deprecated version warns and recommends the latest ACTIVE version
    Given a vocabulary "Ref" exists with 2 versions
    And a vocabulary "Main" exists
    When I create a new version of "Main" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}/main#> .
      @prefix ref: <${contentBase}/content/${Ref}/versions/1#> .
      ex:Thing a skos:Concept ;
        skos:related ref:Something .
      """
    Then the response status is 201
    And the response field "version" is "2"
    And the response has 1 validation warning
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '${Ref}' version 1 is deprecated. Use version 2, the latest ACTIVE version." and location "prefix 'ref' -> '${contentBase}/content/${Ref}/versions/1#'"
    And version 1 of "Ref" has version status "DEPRECATED"
    And version 2 of "Ref" has version status "ACTIVE"

  @AC_SIMPL-18184_AC2 @D11 @D47 @D12
  Scenario: Each deprecated reference gives its own warning, also on POST /vocabularies
    Given a vocabulary "Ref" exists with 3 versions
    When I create the vocabulary "Main" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}/main#> .
      @prefix one: <${contentBase}/content/${Ref}/versions/1#> .
      @prefix two: <${contentBase}/content/${Ref}/versions/2/> .
      ex:Thing a skos:Concept ;
        skos:related one:A , two:B .
      """
    Then the response status is 201
    And the response has 2 validation warnings
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '${Ref}' version 1 is deprecated. Use version 3, the latest ACTIVE version." and location "prefix 'one' -> '${contentBase}/content/${Ref}/versions/1#'"
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '${Ref}' version 2 is deprecated. Use version 3, the latest ACTIVE version." and location "prefix 'two' -> '${contentBase}/content/${Ref}/versions/2/'"

  @AC_SIMPL-18184_AC2 @D47 @D4
  Scenario: Without another ACTIVE version the warning recommends no version
    Given a vocabulary "Ref" exists
    And the version status of version 1 of "Ref" is set to "DEPRECATED"
    When I create the vocabulary "Main" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}/main#> .
      @prefix ref: <${contentBase}/content/${Ref}/versions/1#> .
      ex:Thing a skos:Concept ;
        skos:related ref:Something .
      """
    Then the response status is 201
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '${Ref}' version 1 is deprecated." and location "prefix 'ref' -> '${contentBase}/content/${Ref}/versions/1#'"

  @AC_SIMPL-18184_AC2 @D3 @D47 @D12
  Scenario: A reference without version to a DEPRECATED vocabulary resolves to the latest version and names the vocabulary
    Given a vocabulary "Ref" exists
    And the vocabulary status of "Ref" is set to "DEPRECATED"
    When I create the vocabulary "Main" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}/main#> .
      @prefix ref: <${contentBase}/content/${Ref}#> .
      ex:Thing a skos:Concept ;
        skos:related ref:Something .
      """
    Then the response status is 201
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '${Ref}' version 1 is deprecated. The vocabulary '${Ref}' is deprecated." and location "prefix 'ref' -> '${contentBase}/content/${Ref}#'"
    And vocabulary "Ref" has vocabulary status "DEPRECATED"
    And version 1 of "Ref" has version status "ACTIVE"

  @AC_SIMPL-18184_AC3 @D12 @D47 @D65 @D8 @negative
  Scenario: A reference to a version that does not exist is error S7 and creates nothing
    Given a vocabulary "Ref" exists
    And a vocabulary "Main" exists
    When I create a new version of "Main" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}/main#> .
      @prefix ref: <${contentBase}/content/${Ref}/versions/9#> .
      ex:Thing a skos:Concept ;
        skos:related ref:Something .
      """
    Then the response is a problem with status 400
    And every issue is in "body" named "vocabularyFile"
    And the problem has an issue with detail "Referenced internal vocabulary '${Ref}' version 9 does not exist." and location "prefix 'ref' -> '${contentBase}/content/${Ref}/versions/9#'"
    And vocabulary "Main" has 1 version
    And version 1 of "Main" has version status "ACTIVE"

  @AC_SIMPL-18184_AC3 @D12 @D47 @negative
  Scenario: A reference without version to a vocabulary that does not exist is error S7
    When I create the vocabulary "Main" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}/main#> .
      @prefix ghost: <${contentBase}/content/${Ghost}#> .
      ex:Thing a skos:Concept ;
        skos:related ghost:Something .
      """
    Then the response is a problem with status 400
    And the problem has an issue with detail "Referenced internal vocabulary '${Ghost}' does not exist." and location "prefix 'ghost' -> '${contentBase}/content/${Ghost}#'"
    And no vocabulary whose name contains "${uid}" is stored

  @D69 @D47 @negative
  Scenario: A version segment that is not a positive integer is error S7 with the value as given
    Given a vocabulary "Ref" exists
    When I create the vocabulary "Main" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}/main#> .
      @prefix ref: <${contentBase}/content/${Ref}/versions/abc#> .
      ex:Thing a skos:Concept ;
        skos:related ref:Something .
      """
    Then the response is a problem with status 400
    And the problem has an issue with detail "Referenced internal vocabulary '${Ref}' version abc does not exist." and location "prefix 'ref' -> '${contentBase}/content/${Ref}/versions/abc#'"
    And 1 vocabulary whose name contains "${uid}" is stored

  @D12
  Scenario: A reference to the vocabulary being versioned is ignored
    Given a vocabulary "Main" exists
    When I create a new version of "Main" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}/main#> .
      @prefix self: <${contentBase}/content/${Main}/versions/7#> .
      ex:Thing a skos:Concept ;
        skos:related self:Something .
      """
    Then the response status is 201
    And the response has no validation warnings

  @D12
  Scenario: A reference to the vocabulary being created is ignored
    When I create the vocabulary "Main" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}/main#> .
      @prefix self: <${contentBase}/content/${Main}#> .
      ex:Thing a skos:Concept ;
        skos:related self:Something .
      """
    Then the response status is 201
    And the response has no validation warnings
