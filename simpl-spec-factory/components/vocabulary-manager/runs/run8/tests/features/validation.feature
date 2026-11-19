Feature: Validation of vocabulary content on create and new version
  Two steps on every POST /vocabularies and POST .../versions: syntax (Turtle),
  then semantic rules S1 to S7. A syntax error stops validation with one issue.
  All semantic errors are returned together. Errors block (400, nothing
  stored); warnings never block and come back in validationWarnings.

  Background:
    Given I am authenticated as "ga-admin"

  # ---- syntax ---------------------------------------------------------------

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18170_AC3 @D9 @D10 @D14 @D60 @D65 @D8 @negative
  Scenario: Invalid Turtle is rejected with one syntax issue that gives the line
    When I create the vocabulary "Broken" with the fixture "syntax_error.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And every issue has type "urn:problem-type:belgif:input-validation:invalidInput"
    And every issue is in "body" named "vocabularyFile"
    And the problem has an issue with detail starting with "Encountered invalid Turtle syntax" and location matching "line 3(,.*)?"
    And no vocabulary whose name contains "${uid}" is stored

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18182_AC1 @D9 @negative
  Scenario: Semantic validation does not run after a syntax error
    When I create the vocabulary "Broken" with the fixture "syntax_and_semantic_errors.ttl"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And the problem has an issue with detail starting with "Encountered invalid Turtle syntax" and location matching "line 5(,.*)?"
    And no vocabulary whose name contains "${uid}" is stored

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18173_AC2 @AC_SIMPL-18180_AC3 @D8 @D9 @negative
  Scenario: A new version with a syntax error creates nothing and deprecates nothing
    Given a vocabulary "Health" exists
    When I send a POST request to "/vocabularies/${Health}/versions" with these form parts
      | part           | value                    |
      | description    | Next                     |
      | changelog      | Broken change            |
      | vocabularyFile | fixture:syntax_error.ttl |
    Then the response is a problem with status 400
    And the problem has 1 issue
    And every issue is in "body" named "vocabularyFile"
    And vocabulary "Health" has 1 version
    And version 1 of "Health" has version status "ACTIVE"

  @D70 @D9 @Q2 @negative
  Scenario: An empty file passes the field check and fails validation with S6
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value            |
      | name           | ${Empty}         |
      | description    | Some text        |
      | changelog      | Initial version. |
      | vocabularyFile | empty            |
    Then the response is a problem with status 400
    And the problem has an issue with detail "Vocabulary contains no statements" and location "document"
    And every issue is in "body" named "vocabularyFile"
    And no vocabulary whose name contains "${uid}" is stored

  # ---- semantic rules ---------------------------------------------------------

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18182_AC3 @AC_SIMPL-18180_AC6 @D9 @D10 @D65 @D8 @negative
  Scenario Outline: Each semantic rule blocks creation with its detail and location
    When I create the vocabulary "Sem" with the fixture "<fixture>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And every issue has type "urn:problem-type:belgif:input-validation:invalidInput"
    And every issue is in "body" named "vocabularyFile"
    And the problem has an issue with detail "<detail>" and location "<location>"
    And no vocabulary whose name contains "${uid}" is stored

    Examples:
      | rule | fixture                                  | detail                                                | location                                                                                         |
      | S2   | s2_resource_for_datatype_property.ttl    | Resource value for datatype property                  | statement 'https://example.org/vocab/${uid}#Thing https://example.org/vocab/${uid}#code https://example.org/vocab/${uid}#Other' |
      | S3   | s3_object_and_datatype_property.ttl      | Property declared as both object and datatype property | resource 'https://example.org/vocab/${uid}#both'                                                 |
      | S4   | s4_duplicate_preflabel.ttl               | Duplicate preferred label for the same language       | resource 'https://example.org/vocab/${uid}#Thing', language 'en'                                 |
      | S5   | s5_disjoint_classes.ttl                  | Resource is an instance of disjoint classes           | resource 'https://example.org/vocab/${uid}#rex'                                                  |
      | S6   | s6_no_statements.ttl                     | Vocabulary contains no statements                     | document                                                                                         |

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @D9 @D65 @negative
  Scenario: S1, a literal as the object of an object property, gives the statement as location
    When I create the vocabulary "Sem" with the fixture "s1_literal_for_object_property.ttl"
    Then the response is a problem with status 400
    And every issue is in "body" named "vocabularyFile"
    And the problem has an issue with detail "Literal value for object property", location starting with "statement 'https://example.org/vocab/${uid}#Thing https://example.org/vocab/${uid}#relatedTo " and containing "plain literal"
    And no vocabulary whose name contains "${uid}" is stored

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18180_AC7 @D9 @D65 @negative
  Scenario: All semantic errors are returned in one response
    When I create the vocabulary "Sem" with the fixture "three_semantic_errors.ttl"
    Then the response is a problem with status 400
    And the problem has 3 issues
    And the problem has an issue with detail "Literal value for object property"
    And the problem has an issue with detail "Property declared as both object and datatype property" and location "resource 'https://example.org/vocab/${uid}#both'"
    And the problem has an issue with detail "Duplicate preferred label for the same language" and location "resource 'https://example.org/vocab/${uid}#Label', language 'en'"
    And no vocabulary whose name contains "${uid}" is stored

  @D53 @D65 @negative
  Scenario: Two untagged preferred labels on one subject are an S4 error with language none
    When I create the vocabulary "Sem" with the fixture "s4_duplicate_untagged_preflabel.ttl"
    Then the response is a problem with status 400
    And the problem has an issue with detail "Duplicate preferred label for the same language" and location "resource 'https://example.org/vocab/${uid}#Thing', language 'none'"

  @D53
  Scenario: One untagged and one tagged preferred label are not an S4 error
    When I create the vocabulary "Sem" with the fixture "untagged_and_tagged_preflabel.ttl"
    Then the response status is 201

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18173_AC2 @D9 @D8 @negative
  Scenario: A new version with semantic errors creates nothing and deprecates nothing
    Given a vocabulary "Health" exists
    When I send a POST request to "/vocabularies/${Health}/versions" with these form parts
      | part           | value                             |
      | description    | Next                              |
      | changelog      | Semantic errors                   |
      | vocabularyFile | fixture:three_semantic_errors.ttl |
    Then the response is a problem with status 400
    And the problem has 3 issues
    And vocabulary "Health" has 1 version
    And version 1 of "Health" has version status "ACTIVE"

  @D57 @D65 @negative
  Scenario: Relative IRIs resolve against the content URL of the new version
    When I create the vocabulary "Rel" with content
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      <#both> a owl:ObjectProperty .
      <#both> a owl:DatatypeProperty .
      """
    Then the response is a problem with status 400
    And the problem has an issue with detail "Property declared as both object and datatype property" and location "resource '${contentBase}/content/${Rel}/versions/1#both'"

  # ---- warnings -------------------------------------------------------------

  @AC_SIMPL-18183_AC1 @AC_SIMPL-18194_AC2 @AC_SIMPL-18182_AC2 @D10 @D12
  Scenario: Content that uses only W3C namespaces and its own namespace gives no warnings
    When I create the vocabulary "Clean" with the fixture "w3c_namespaces_only.ttl"
    Then the response status is 201
    And the response has no validation warnings

  @AC_SIMPL-18183_AC2 @D10 @D12 @D65
  Scenario: An unregistered external namespace gives the warning externalReferenceSkipped and does not block
    When I create the vocabulary "Ext" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}#> .
      @prefix unreg: <http://unregistered.example.org/${uid}/terms#> .
      ex:Thing a skos:Concept ;
        skos:related unreg:Other .
      """
    Then the response status is 201
    And the response has 1 validation warning
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'unreg' -> 'http://unregistered.example.org/${uid}/terms#'"
    And every validation warning code is one of "externalReferenceSkipped,deprecatedVocabularyReference,semanticReasoningSkipped"
    And 1 vocabulary whose name contains "${Ext}" is stored

  @D12 @D30 @D42
  Scenario: A namespace that matches a registered external vocabulary gives no warning
    Given an external vocabulary "Terms" is registered with namespace "http://ext.example.org/${uid}/terms" and alias "https://alias.example.org/${uid}/terms"
    When I create the vocabulary "Uses" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}#> .
      @prefix t: <http://ext.example.org/${uid}/terms/> .
      @prefix a: <https://alias.example.org/${uid}/terms#> .
      ex:Thing a skos:Concept ;
        skos:related t:title , a:creator .
      """
    Then the response status is 201
    And the response has no validation warnings

  @D69 @D12
  Scenario: A content namespace with a name that breaks the name pattern is external
    When I create the vocabulary "Odd" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}#> .
      @prefix low: <${contentBase}/content/lower${uid}#> .
      ex:Thing a skos:Concept ;
        skos:related low:Other .
      """
    Then the response status is 201
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'low' -> '${contentBase}/content/lower${uid}#'"
