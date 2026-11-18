Feature: Validation of vocabulary content on creation and new version
  Two steps run on every POST /vocabularies and POST .../versions: syntax, then semantic (D9).
  Errors block with a 400 validation report; warnings never block and come back in
  validationWarnings of the 201 response (D10, D14).

  Background:
    Given I am authenticated as "ga-admin"

  # ---------------------------------------------------------------- syntax
  @AC_SIMPL-18181_AC2 @AC_SIMPL-18194_AC1 @D9 @D10 @D14 @D8 @negative @BLG-err-problem
  Scenario: A vocabulary with a Turtle syntax error is rejected with one located issue
    When I create the vocabulary "Broken" with the file "syntax-error-line4.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And every issue is a vocabulary file issue
    And the problem has an issue whose detail starts with "Encountered invalid Turtle syntax"
    And the issue location is a parser position on line 4
    And the vocabulary "Broken" does not exist

  @AC_SIMPL-18181_AC2 @D9 @negative
  Scenario: Semantic validation does not run after a syntax error
    When I create the vocabulary "Broken" with the file "syntax-and-semantic-errors.ttl"
    Then the response status is 400
    And the problem has 1 issue
    And the problem has an issue whose detail starts with "Encountered invalid Turtle syntax"

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18194_AC1 @D9 @D8 @D4 @negative
  Scenario: A new version with a syntax error creates nothing and deprecates nothing
    Given a vocabulary "Health" exists
    When I create a new version of "Health" with the file "syntax-error-line4.ttl"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And every issue is a vocabulary file issue
    And the vocabulary "Health" has 1 version
    And the versions of "Health" have the statuses "1=ACTIVE"
    And the vocabulary "Health" has status "ACTIVE"

  @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC1 @AC_SIMPL-18194_AC2 @D9 @D10 @D14
  Scenario: Valid content passes both steps and the 201 is the success indicator
    When I create the vocabulary "Clean"
    Then the response status is 201
    And the response has no validation warnings
    And the response body has no field "issues"

  @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC1 @D9
  Scenario: A valid new version passes both steps
    Given a vocabulary "Health" exists
    When I create a new version of "Health"
    Then the response status is 201
    And the response has no validation warnings

  # ---------------------------------------------------------------- semantic
  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @D9 @D10 @D8 @negative
  Scenario Outline: Semantic rule <rule> blocks the creation
    When I create the vocabulary "Semantic" with the file "<file>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And every issue is a vocabulary file issue
    And the problem has an issue with detail "<detail>"
    And the vocabulary "Semantic" does not exist

    Examples:
      | rule | file                                  | detail                                                 |
      | S1   | s1-literal-for-object-property.ttl    | Literal value for object property                      |
      | S2   | s2-resource-for-datatype-property.ttl | Resource value for datatype property                   |
      | S3   | s3-object-and-datatype-property.ttl   | Property declared as both object and datatype property |
      | S4   | s4-duplicate-preflabel.ttl            | Duplicate preferred label for the same language        |
      | S5   | s5-disjoint-classes.ttl               | Resource is an instance of disjoint classes            |

  @AC_SIMPL-18183_AC2 @D9 @negative
  Scenario: A file without statements is rejected with location document
    When I create the vocabulary "Empty" with the file "s6-no-statements.ttl"
    Then the response status is 400
    And the problem has an issue with detail "Vocabulary contains no statements" and location "document"
    And the vocabulary "Empty" does not exist

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @D9 @D14 @negative
  Scenario: All semantic errors are returned in one response
    When I create the vocabulary "Many" with the file "three-semantic-errors.ttl"
    Then the response status is 400
    And the problem has 3 issues
    And every issue is a vocabulary file issue
    And the problem has an issue with detail "Literal value for object property"
    And the problem has an issue with detail "Duplicate preferred label for the same language"
    And the problem has an issue with detail "Resource is an instance of disjoint classes"

  @AC_SIMPL-18183_AC2 @D9 @D8 @D4 @negative
  Scenario: A new version with semantic errors creates nothing and deprecates nothing
    Given a vocabulary "Health" exists
    When I create a new version of "Health" with the file "s4-duplicate-preflabel.ttl"
    Then the response status is 400
    And the problem has an issue with detail "Duplicate preferred label for the same language"
    And the versions of "Health" have the statuses "1=ACTIVE"

  # ---------------------------------------------------------------- internal references
  @AC_SIMPL-18184_AC1 @D11 @D12 @D10
  Scenario: A reference to an ACTIVE internal version gives no warning
    Given a vocabulary "Base" exists
    And a vocabulary "Health" exists
    When I create a new version of "Health" with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix base: <${content:Base}/versions/1#> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Uses base"@en .
      """
    Then the response status is 201
    And the response has no validation warnings

  @AC_SIMPL-18184_AC1 @D12
  Scenario: A reference without a version resolves to the latest version
    Given a vocabulary "Base" exists
    When I create the vocabulary "User" with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix base: <${content:Base}#> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme .
      """
    Then the response status is 201
    And the response has no validation warnings

  @AC_SIMPL-18184_AC2 @D11 @D12 @D3 @D10 @E2
  Scenario: A reference to a version replaced by a newer one gives a deprecation warning, not an error
    Given a vocabulary "Base" exists with 2 versions
    And a vocabulary "Health" exists
    When I create a new version of "Health" with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix legacy: <${content:Base}/versions/1#> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme .
      """
    Then the response status is 201
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '${name:Base}' version 1 is deprecated. Use version 2, the latest ACTIVE version." and location "prefix 'legacy' -> '${content:Base}/versions/1#'"
    And the response has 1 validation warning with code "deprecatedVocabularyReference"
    And the versions of "Health" have the statuses "1=DEPRECATED, 2=ACTIVE"

  @AC_SIMPL-18184_AC2 @D11 @D12
  Scenario: Each deprecated reference gives its own warning
    Given a vocabulary "Base" exists with 3 versions
    When I create the vocabulary "User" with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix one: <${content:Base}/versions/1#> .
      @prefix two: <${content:Base}/versions/2/> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme .
      """
    Then the response status is 201
    And the response has 2 validation warnings with code "deprecatedVocabularyReference"
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '${name:Base}' version 2 is deprecated. Use version 3, the latest ACTIVE version." and location "prefix 'two' -> '${content:Base}/versions/2/'"

  @AC_SIMPL-18184_AC2 @D11 @D4
  Scenario: Without an ACTIVE version the deprecation warning has no recommendation
    Given a vocabulary "Base" exists
    And version 1 of vocabulary "Base" has status "DEPRECATED"
    When I create the vocabulary "User" with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix old: <${content:Base}/versions/1> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme .
      """
    Then the response status is 201
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '${name:Base}' version 1 is deprecated." and location "prefix 'old' -> '${content:Base}/versions/1'"

  @AC_SIMPL-18184_AC2 @D11 @D3
  Scenario: A version of a DEPRECATED vocabulary counts as deprecated
    Given a vocabulary "Base" exists
    And the vocabulary "Base" has status "DEPRECATED"
    When I create the vocabulary "User" with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix base: <${content:Base}#> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme .
      """
    Then the response status is 201
    And the response has a validation warning "deprecatedVocabularyReference" whose description starts with "Referenced internal vocabulary '${name:Base}' version 1 is deprecated."

  @AC_SIMPL-18184_AC3 @D12 @D9 @D8 @negative
  Scenario: A reference to a version that does not exist is a validation error
    Given a vocabulary "Base" exists
    And a vocabulary "Health" exists
    When I create a new version of "Health" with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix base: <${content:Base}/versions/9#> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme .
      """
    Then the response is a problem with status 400
    And every issue is a vocabulary file issue
    And the problem has an issue with detail "Referenced internal vocabulary '${name:Base}' version 9 does not exist."
    And the versions of "Health" have the statuses "1=ACTIVE"

  @AC_SIMPL-18184_AC3 @D12 @D9 @negative
  Scenario: A reference to a vocabulary that does not exist is a validation error
    When I create the vocabulary "User" with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ghost: <${content:Ghost}/versions/1#> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme .
      """
    Then the response status is 400
    And the problem has an issue with detail "Referenced internal vocabulary '${name:Ghost}' version 1 does not exist."
    And the vocabulary "User" does not exist

  @AC_SIMPL-18183_AC2 @D9 @D12 @negative
  Scenario: A missing reference and a semantic error are reported together
    When I create the vocabulary "User" with the content:
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ghost: <${content:Ghost}/versions/2#> .
      @prefix ex: <${own}> .
      ex:hasPart a owl:ObjectProperty .
      ex:Thing ex:hasPart "a literal" .
      """
    Then the response status is 400
    And the problem has 2 issues
    And the problem has an issue with detail "Literal value for object property"
    And the problem has an issue with detail "Referenced internal vocabulary '${name:Ghost}' version 2 does not exist."

  @D12
  Scenario: A reference to the vocabulary being versioned is ignored
    Given a vocabulary "Health" exists with 2 versions
    When I create a new version of "Health" with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix self: <${content:Health}/versions/1#> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme .
      """
    Then the response status is 201
    And the response has no validation warnings

  @D12
  Scenario: A reference to the vocabulary being created is ignored
    When I create the vocabulary "Fresh" with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix self: <${content:Fresh}/versions/1#> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme .
      """
    Then the response status is 201
    And the response has no validation warnings

  # ---------------------------------------------------------------- external references
  @D12 @D10 @AC_SIMPL-18183_AC1
  Scenario: An unregistered external namespace gives the warning externalReferenceSkipped
    When I create the vocabulary "User" with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix far: <https://unregistered.example/${runid}/terms#> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme ;
        far:note "uses an outside namespace" .
      """
    Then the response status is 201
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'far' -> 'https://unregistered.example/${runid}/terms#'"
    And the response has 1 validation warning with code "externalReferenceSkipped"
    And every validation warning code is one of "externalReferenceSkipped, deprecatedVocabularyReference"

  @D12 @D10
  Scenario: W3C namespaces and the vocabulary's own namespace give no warning
    When I create the vocabulary "User" with the content:
      """
      @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
      @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${own}> .
      ex:Thing a owl:Class ;
        rdfs:label "Thing"@en .
      """
    Then the response status is 201
    And the response has no validation warnings

  @D12 @D30 @D42
  Scenario Outline: A namespace that matches a registered external vocabulary gives no warning (<case>)
    Given an external vocabulary "Dc" is registered with:
      | field       | value                                    |
      | namespaceUri | https://dc.example/${runid}/terms/       |
      | aliasUris   | https://alias.example/${runid}/terms#     |
    When I create the vocabulary "User" with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix dc: <<namespace>> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme ;
        dc:title "Title" .
      """
    Then the response status is 201
    And the response has no validation warning with code "externalReferenceSkipped"

    Examples:
      | case                         | namespace                               |
      | same namespace               | https://dc.example/${runid}/terms/      |
      | namespace without slash      | https://dc.example/${runid}/terms       |
      | namespace with hash          | https://dc.example/${runid}/terms#      |
      | alias                        | https://alias.example/${runid}/terms#   |
      | alias without hash           | https://alias.example/${runid}/terms    |

  @D42 @D12
  Scenario: After the external vocabulary is removed its namespace gives the warning again
    Given an external vocabulary "Dc" is registered
    And the external vocabulary "Dc" is removed
    When I create the vocabulary "User" with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix dc: <${extns:Dc}> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme ;
        dc:title "Title" .
      """
    Then the response status is 201
    And the response has 1 validation warning with code "externalReferenceSkipped"

  @D42 @D12
  Scenario: A version stored before the external vocabulary was registered can be cleared by a new version
    Given a vocabulary "User" exists with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix dc: <${extns:Dc}> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme ;
        dc:title "Title" .
      """
    And an external vocabulary "Dc" is registered
    When I create a new version of "User" with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix dc: <${extns:Dc}> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme ;
        dc:title "New title" .
      """
    Then the response status is 201
    And the response has no validation warnings

  @D10 @D11 @D12
  Scenario: Warnings of both kinds come back together and the version is still created
    Given a vocabulary "Base" exists with 2 versions
    When I create the vocabulary "User" with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix legacy: <${content:Base}/versions/1#> .
      @prefix far: <https://unregistered.example/${runid}/x#> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme .
      """
    Then the response status is 201
    And the response has 1 validation warning with code "deprecatedVocabularyReference"
    And the response has 1 validation warning with code "externalReferenceSkipped"
    And every validation warning code is one of "externalReferenceSkipped, deprecatedVocabularyReference"

  @D10
  Scenario: Warnings are not stored or returned by other operations
    When I create the vocabulary "User" with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix far: <https://unregistered.example/${runid}/y#> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme .
      """
    Then the response status is 201
    When I send GET "/vocabularies/${name:User}/versions/1"
    Then the response status is 200
    And the field "validationWarnings" is absent
