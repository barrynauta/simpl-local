Feature: Syntax and semantic validation of vocabulary content
  Every POST /vocabularies and POST .../versions runs syntax validation, then semantic
  validation (D9). Errors block with 400 VocabularyValidationProblem; warnings never block
  and come back in validationWarnings of the 201 response (D10).

  Background:
    Given I am a Governance Authority user with role "GA_VOCABULARY_ADMIN"

  @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC1 @AC_SIMPL-18194_AC2 @AC_SIMPL-18182_AC2 @D9 @D10 @D14
  Scenario: Content that passes both steps is created without error details
    When I create the vocabulary "{Alpha}" with the fixture "w3c-and-own-namespaces.ttl"
    Then the response status is 201
    And the response field "validationWarnings" equals []
    And the response field "issues" is absent
    And the response field "status" is "ACTIVE"

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18180_AC5 @D9 @D10 @D14 @D8 @D48 @negative
  Scenario: A syntax error on creation is reported with its location and nothing is stored
    When I create the vocabulary "{Alpha}" with the fixture "syntax-error.ttl"
    Then the response is a problem with status 400
    And the problem has exactly 1 issue
    And every issue is in "body" named "vocabularyFile"
    And every issue has the type "urn:problem-type:belgif:input-validation:invalidInput"
    And the problem has an issue with detail starting "Encountered invalid Turtle syntax"
    And the first issue has a location matching "^line 6, column \d+$"
    And no vocabulary named "{Alpha}" is stored

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18194_AC1 @D9 @D8 @D4 @negative
  Scenario: A syntax error on a new version creates no version and deprecates nothing
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with the fixture "syntax-error.ttl"
    Then the response is a problem with status 400
    And the problem has exactly 1 issue
    And the problem has an issue with detail starting "Encountered invalid Turtle syntax"
    And the first issue has a location matching "^line 6, column \d+$"
    And the vocabulary "{Alpha}" has 1 version
    And version 1 of "{Alpha}" has status "ACTIVE"

  @AC_SIMPL-18181_AC2 @D9 @negative
  Scenario: Semantic validation does not run after a syntax error
    When I create the vocabulary "{Alpha}" with the fixture "syntax-after-semantic.ttl"
    Then the response is a problem with status 400
    And the problem has exactly 1 issue
    And the problem has an issue with detail starting "Encountered invalid Turtle syntax"
    And the first issue has a location matching "^line 8, column \d+$"

  @AC_SIMPL-18181_AC1 @AC_SIMPL-18182_AC1 @D9 @negative
  Scenario: Content with valid syntax proceeds to semantic validation
    When I create the vocabulary "{Alpha}" with the fixture "s4-duplicate-preflabel.ttl"
    Then the response is a problem with status 400
    And the problem has an issue with detail "Duplicate preferred label for the same language"

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18182_AC3 @AC_SIMPL-18180_AC6 @D9 @D10 @D8 @negative
  Scenario Outline: A semantic error on creation blocks and is reported (<rule>)
    When I create the vocabulary "{Alpha}" with the fixture "<fixture>"
    Then the response is a problem with status 400
    And the problem has exactly 1 issue
    And every issue is in "body" named "vocabularyFile"
    And every issue has the type "urn:problem-type:belgif:input-validation:invalidInput"
    And the problem has an issue with detail "<detail>"
    And the issue with detail "<detail>" has a location containing "<location part>"
    And no vocabulary named "{Alpha}" is stored

    Examples:
      | rule | fixture                           | detail                                                 | location part |
      | S1   | s1-literal-object-property.ttl    | Literal value for object property                      | carOne        |
      | S2   | s2-resource-datatype-property.ttl | Resource value for datatype property                   | boxOne        |
      | S3   | s3-object-and-datatype.ttl        | Property declared as both object and datatype property | ambiguousProp |
      | S4   | s4-duplicate-preflabel.ttl        | Duplicate preferred label for the same language        | doubleLabel   |
      | S5   | s5-disjoint-classes.ttl           | Resource is an instance of disjoint classes            | felixTheMix   |

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @D9 @D8 @negative
  Scenario: A file without statements is a semantic error located at the document
    When I create the vocabulary "{Alpha}" with the fixture "s6-no-statements.ttl"
    Then the response is a problem with status 400
    And the problem has an issue with detail "Vocabulary contains no statements"
    And the issue with detail "Vocabulary contains no statements" has location "document"
    And no vocabulary named "{Alpha}" is stored

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18180_AC7 @D9 @D4 @D8 @negative
  Scenario Outline: A semantic error on a new version blocks and changes nothing (<rule>)
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with the fixture "<fixture>"
    Then the response is a problem with status 400
    And the problem has an issue with detail "<detail>"
    And the vocabulary "{Alpha}" has 1 version
    And version 1 of "{Alpha}" has status "ACTIVE"

    Examples:
      | rule | fixture                           | detail                                                 |
      | S1   | s1-literal-object-property.ttl    | Literal value for object property                      |
      | S3   | s3-object-and-datatype.ttl        | Property declared as both object and datatype property |
      | S5   | s5-disjoint-classes.ttl           | Resource is an instance of disjoint classes            |
      | S6   | s6-no-statements.ttl              | Vocabulary contains no statements                      |

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18180_AC7 @D9 @negative
  Scenario: All semantic errors are returned in one response
    When I create the vocabulary "{Alpha}" with the fixture "multiple-semantic-errors.ttl"
    Then the response is a problem with status 400
    And the problem has exactly 3 issues
    And the problem has an issue with detail "Literal value for object property"
    And the problem has an issue with detail "Property declared as both object and datatype property"
    And the problem has an issue with detail "Duplicate preferred label for the same language"
    And every issue is in "body" named "vocabularyFile"

  @D53 @D9 @negative
  Scenario: Two labels without a language tag on one subject are a duplicate
    When I create the vocabulary "{Alpha}" with the fixture "s4-two-untagged-labels.ttl"
    Then the response is a problem with status 400
    And the problem has an issue with detail "Duplicate preferred label for the same language"

  @D53 @D9
  Scenario Outline: Labels in different languages are not a duplicate (<case>)
    When I create the vocabulary "{Alpha}" with the fixture "<fixture>"
    Then the response status is 201

    Examples:
      | case                     | fixture                    |
      | one untagged, one tagged | s4-untagged-and-tagged.ttl |
      | English and French       | s4-different-languages.ttl |

  @AC_SIMPL-18183_AC2 @D10 @D12
  Scenario: An unregistered external namespace gives a warning and the vocabulary is created
    When I create the vocabulary "{Alpha}" with the fixture "external-unregistered.ttl"
    Then the response status is 201
    And the response field "validationWarnings[0].code" is "externalReferenceSkipped"
    And the response field "validationWarnings[0].location" is "prefix 'unreg' -> 'https://unregistered-{run}.example.org/terms/'"
    And the response field "validationWarnings[0].description" is present
    And the response field "validationWarnings[1]" is absent

  @D12 @D10
  Scenario: W3C namespaces and the vocabulary's own namespace give no warning
    When I create the vocabulary "{Alpha}" with the fixture "w3c-and-own-namespaces.ttl"
    Then the response status is 201
    And the response field "validationWarnings" equals []

  @D12 @D30 @D10
  Scenario Outline: A registered external namespace gives no warning (<case>)
    Given an external vocabulary "{Ext}" is registered with namespace "https://ext-{run}.example.org/terms" and alias "https://alias-{run}.example.org/terms"
    When I create the vocabulary "{Alpha}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ext: <<declared>> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme"@en ;
        ext:creator "Somebody" .
      """
    Then the response status is 201
    And the response field "validationWarnings" equals []

    Examples:
      | case                         | declared                                 |
      | namespace with trailing '#'  | https://ext-{run}.example.org/terms#     |
      | namespace with trailing '/'  | https://ext-{run}.example.org/terms/     |
      | alias with trailing '/'      | https://alias-{run}.example.org/terms/   |

  @D42 @D12 @D41
  Scenario: After the external vocabulary is removed, a new upload warns again
    Given an external vocabulary "{Ext}" is registered with namespace "https://ext-{run}.example.org/terms#"
    And a vocabulary "{Alpha}" exists with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ext: <https://ext-{run}.example.org/terms#> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme"@en ;
        ext:creator "Somebody" .
      """
    When I send "DELETE" to "/external-vocabularies/{Ext}"
    Then the response status is 204
    And the content of version 1 of "{Alpha}" is unchanged
    And version 1 of "{Alpha}" has status "ACTIVE"
    When I create a new version of "{Alpha}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ext: <https://ext-{run}.example.org/terms#> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme, second version"@en ;
        ext:creator "Somebody" .
      """
    Then the response status is 201
    And the response field "validationWarnings[0].code" is "externalReferenceSkipped"

  @D42 @D12
  Scenario: Registering the external vocabulary after an upload lets a new version clear the warning
    Given a vocabulary "{Alpha}" exists with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ext: <https://ext-{run}.example.org/later#> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme"@en ;
        ext:creator "Somebody" .
      """
    And an external vocabulary "{Ext}" is registered with namespace "https://ext-{run}.example.org/later#"
    When I create a new version of "{Alpha}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ext: <https://ext-{run}.example.org/later#> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme, second version"@en ;
        ext:creator "Somebody" .
      """
    Then the response status is 201
    And the response field "validationWarnings" equals []

  @AC_SIMPL-18184_AC3 @D12 @D47 @D8 @negative
  Scenario: A reference to a version that does not exist is a validation error
    Given a vocabulary "{Base}" exists
    And a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix base: <{content:Base:9}#> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme"@en ;
        skos:related base:Something .
      """
    Then the response is a problem with status 400
    And the problem has an issue with detail "Referenced internal vocabulary '{Base}' version 9 does not exist."
    And the vocabulary "{Alpha}" has 1 version
    And version 1 of "{Alpha}" has status "ACTIVE"

  @AC_SIMPL-18184_AC3 @D12 @D47 @D8 @negative
  Scenario: A reference to a vocabulary that does not exist is a validation error
    Given a vocabulary "{Base}" exists
    When I create the vocabulary "{Alpha}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ghost: <{content:Ghost}#> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme"@en ;
        skos:related ghost:Something .
      """
    Then the response is a problem with status 400
    And the problem has an issue with detail "Referenced internal vocabulary '{Ghost}' does not exist."
    And no vocabulary named "{Alpha}" is stored

  @D12 @D9 @negative
  Scenario: A missing reference and a semantic error are reported together
    Given a vocabulary "{Base}" exists
    When I create the vocabulary "{Alpha}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix base: <{content:Base:5}/> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:doubleLabel a skos:Concept ;
        skos:prefLabel "First"@en , "Second"@en ;
        skos:related base:Something .
      """
    Then the response is a problem with status 400
    And the problem has exactly 2 issues
    And the problem has an issue with detail "Referenced internal vocabulary '{Base}' version 5 does not exist."
    And the problem has an issue with detail "Duplicate preferred label for the same language"

  @D12
  Scenario: A reference to the vocabulary being versioned is ignored
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix self: <{content:Alpha}#> .
      @prefix selfnext: <{content:Alpha:2}#> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme"@en ;
        skos:related self:Something , selfnext:Other .
      """
    Then the response status is 201
    And the response field "version" equals 2
    And the response field "validationWarnings" equals []

  @D12 @D5
  Scenario: A reference without a version resolves to the latest version
    Given a vocabulary "{Base}" exists with 2 versions
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
    And the response field "validationWarnings" equals []
