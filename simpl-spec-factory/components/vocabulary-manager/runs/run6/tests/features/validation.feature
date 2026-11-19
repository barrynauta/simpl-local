Feature: Validation of vocabulary content on create and new version
  Two steps on every POST /vocabularies and POST .../versions: syntax, then semantic (D9).
  Validation errors block with 400 and a validation report; validation warnings never block
  and come back in validationWarnings of the 201 response (D10, D14).

  Background:
    Given I am authenticated as "ga-admin"

  # ---- syntax validation ---------------------------------------------------------------

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18170_AC3 @D9 @D8 @D10 @D14 @negative
  Scenario: Content that is not valid Turtle is rejected with one syntax issue
    When I create the vocabulary "Broken" from the file "syntax_error_line3.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And every issue is in "body" named "vocabularyFile" with type "urn:problem-type:belgif:input-validation:invalidInput"
    And the first issue has a detail starting with "Encountered invalid Turtle syntax"
    And the first issue has a location matching "line 3, column \d+"
    And no vocabulary named "Broken" is stored

  @AC_SIMPL-18181_AC2 @D9 @negative
  Scenario: Semantic validation does not run after a syntax error
    When I create the vocabulary "Broken" from the file "syntax_error_after_semantic_errors.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And the first issue has a detail starting with "Encountered invalid Turtle syntax"
    And the first issue has a location matching "line 7, column \d+"
    And no vocabulary named "Broken" is stored

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18173_AC2 @D8 @D9 @negative
  Scenario: A new version with a syntax error is rejected and deprecates nothing
    Given the vocabulary "Health" exists
    When I create a new version of "Health" from the file "syntax_error_line3.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And the first issue has a detail starting with "Encountered invalid Turtle syntax"
    And the vocabulary "Health" has 1 version
    And version 1 of "Health" has the status "ACTIVE"

  @AC_SIMPL-18181_AC1 @AC_SIMPL-18194_AC2 @D10 @D14
  Scenario: Valid content passes validation and the 201 response is the confirmation
    When I create the vocabulary "Health" from the file "w3c_namespaces_only.ttl"
    Then the response status is 201
    And the response has no validation warnings
    And the response field "status" is "ACTIVE"

  # ---- semantic validation -------------------------------------------------------------

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @D9 @D8 @D10 @negative
  Scenario Outline: The semantic rule <rule> blocks the creation
    When I create the vocabulary "Bad" from the file "<file>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And every issue is in "body" named "vocabularyFile" with type "urn:problem-type:belgif:input-validation:invalidInput"
    And the problem has an issue with detail "<detail>"
    And every issue has a non-empty location
    And no vocabulary named "Bad" is stored

    Examples:
      | rule | file                                  | detail                                                 |
      | S1   | s1_literal_for_object_property.ttl    | Literal value for object property                      |
      | S2   | s2_resource_for_datatype_property.ttl | Resource value for datatype property                   |
      | S3   | s3_object_and_datatype_property.ttl   | Property declared as both object and datatype property |
      | S4   | s4_duplicate_preflabel.ttl            | Duplicate preferred label for the same language        |
      | S5   | s5_disjoint_classes.ttl               | Resource is an instance of disjoint classes            |

  @AC_SIMPL-18183_AC2 @D9 @negative
  Scenario: A file without statements breaks S6 with the location document
    When I create the vocabulary "Empty" from the file "s6_no_statements.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with detail "Vocabulary contains no statements" and location "document"
    And no vocabulary named "Empty" is stored

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @D9 @negative
  Scenario: All semantic errors come back in one response
    When I create the vocabulary "Many" from the file "several_semantic_errors.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 3 issues
    And the problem has an issue with detail "Literal value for object property"
    And the problem has an issue with detail "Property declared as both object and datatype property"
    And the problem has an issue with detail "Duplicate preferred label for the same language"
    And every issue is in "body" named "vocabularyFile" with type "urn:problem-type:belgif:input-validation:invalidInput"
    And no vocabulary named "Many" is stored

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18173_AC2 @D8 @D9 @negative
  Scenario: A new version with a semantic error is rejected and deprecates nothing
    Given the vocabulary "Health" exists
    When I create a new version of "Health" from the file "s5_disjoint_classes.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with detail "Resource is an instance of disjoint classes"
    And the vocabulary "Health" has 1 version
    And version 1 of "Health" has the status "ACTIVE"

  @AC_SIMPL-18183_AC1 @D9
  Scenario: A new version with correct semantics is created
    Given the vocabulary "Health" exists
    When I create a new version of "Health" from the file "s4_labels_in_three_languages.ttl"
    Then the response status is 201
    And the response field "version" is 2

  @D53 @D9 @negative
  Scenario: Two preferred labels without a language tag break S4
    When I create the vocabulary "Untagged" from the file "s4_two_untagged_preflabels.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with detail "Duplicate preferred label for the same language"
    And no vocabulary named "Untagged" is stored

  @D53 @D9
  Scenario: One untagged and one tagged preferred label are allowed
    When I create the vocabulary "Mixed" from the file "s4_untagged_and_tagged_preflabel.ttl"
    Then the response status is 201

  @D57
  Scenario: Relative IRIs resolve against the content URL of the new version
    When I create the vocabulary "Relative" from the file "relative_iris.ttl"
    Then the response status is 201
    And the response has no validation warnings

  @D57 @D12 @D47 @negative
  Scenario: A relative prefix resolves against the content URL of the new version
    When I create the vocabulary "Relative" from the file "relative_reference_to_ghost.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with detail "Referenced internal vocabulary '{{name:Ghost}}' version 1 does not exist."
    And no vocabulary named "Relative" is stored

  # ---- references and warnings -----------------------------------------------------------

  @AC_SIMPL-18183_AC1 @D10 @D12
  Scenario: The W3C namespaces and the own namespace give no warning
    When I create the vocabulary "Plain" from the file "w3c_namespaces_only.ttl"
    Then the response status is 201
    And the response has no validation warnings

  @D10 @D12 @AC_SIMPL-18183_AC1
  Scenario: An unregistered external namespace gives a warning, not an error
    When I create the vocabulary "Linked" with the prefix "dct" for "{{ext:Terms}}/"
    Then the response status is 201
    And the response has 1 validation warning
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'dct' -> '{{ext:Terms}}/'"
    And every validation warning code is in the closed set

  @D10 @D12
  Scenario: A SPARQL-style PREFIX declaration is a vocabulary reference too
    When I create the vocabulary "Sparql" from the file "sparql_prefix_unregistered.ttl"
    Then the response status is 201
    And the response has 1 validation warning
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'dct' -> '{{ext:SparqlTerms}}/'"

  @D10 @D12 @D42
  Scenario: A registered external namespace gives no warning
    Given the external vocabulary "Terms" is registered with the namespace "{{ext:Terms}}"
    When I create the vocabulary "Linked" with the prefix "dct" for "{{ext:Terms}}/"
    Then the response status is 201
    And the response has no validation warnings

  @D12 @D30
  Scenario: A registered alias URI gives no warning
    Given the external vocabulary "Terms" is registered with the namespace "{{ext:Terms}}" and the alias "{{ext:TermsAlias}}"
    When I create the vocabulary "Linked" with the prefix "alt" for "{{ext:TermsAlias}}#"
    Then the response status is 201
    And the response has no validation warnings

  @AC_SIMPL-18184_AC1 @D11 @D12
  Scenario: A reference to an ACTIVE internal version gives no warning
    Given the vocabulary "Base" exists
    When I create the vocabulary "User" with the prefix "base" for "{{base}}/content/{{name:Base}}/versions/1#"
    Then the response status is 201
    And the response has no validation warnings

  @AC_SIMPL-18184_AC1 @D5 @D12
  Scenario: A reference without a version resolves to the latest version
    Given the vocabulary "Base" exists with 2 versions
    When I create the vocabulary "User" with the prefix "base" for "{{base}}/content/{{name:Base}}#"
    Then the response status is 201
    And the response has no validation warnings

  @AC_SIMPL-18184_AC2 @D11 @D47 @D3
  Scenario: A reference to a deprecated version recommends the latest ACTIVE version
    Given the vocabulary "Base" exists with 2 versions
    And the vocabulary "User" exists
    When I create a new version of "User" with the prefix "base" for "{{base}}/content/{{name:Base}}/versions/1#"
    Then the response status is 201
    And the response has 1 validation warning
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '{{name:Base}}' version 1 is deprecated. Use version 2, the latest ACTIVE version."
    And the response has a validation warning "deprecatedVocabularyReference" with location "prefix 'base' -> '{{base}}/content/{{name:Base}}/versions/1#'"
    And the vocabulary "User" has 2 versions

  @AC_SIMPL-18184_AC2 @D11 @D47 @D3
  Scenario: A reference to a version of a deprecated vocabulary recommends no version
    Given the vocabulary "Base" exists
    And the vocabulary "Base" is deprecated
    When I create the vocabulary "User" with the prefix "base" for "{{base}}/content/{{name:Base}}/versions/1#"
    Then the response status is 201
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '{{name:Base}}' version 1 is deprecated. The vocabulary '{{name:Base}}' is deprecated."

  @AC_SIMPL-18184_AC2 @D11 @D47
  Scenario: A reference to a deprecated version with no ACTIVE version left
    Given the vocabulary "Base" exists
    And version 1 of "Base" is set to "DEPRECATED"
    When I create the vocabulary "User" with the prefix "base" for "{{base}}/content/{{name:Base}}/versions/1#"
    Then the response status is 201
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '{{name:Base}}' version 1 is deprecated."

  @AC_SIMPL-18184_AC2 @D11 @D12 @D5
  Scenario: A reference without a version to a deprecated latest version gives a warning
    Given the vocabulary "Base" exists
    And version 1 of "Base" is set to "DEPRECATED"
    When I create the vocabulary "User" with the prefix "base" for "{{base}}/content/{{name:Base}}/"
    Then the response status is 201
    And the response has a validation warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '{{name:Base}}' version 1 is deprecated."

  @AC_SIMPL-18184_AC3 @D12 @D47 @D8 @negative
  Scenario: A reference to a version that does not exist is a validation error
    Given the vocabulary "Base" exists
    And the vocabulary "User" exists
    When I create a new version of "User" with the prefix "base" for "{{base}}/content/{{name:Base}}/versions/9#"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with detail "Referenced internal vocabulary '{{name:Base}}' version 9 does not exist."
    And the vocabulary "User" has 1 version
    And version 1 of "User" has the status "ACTIVE"

  @AC_SIMPL-18184_AC3 @D12 @D47 @negative
  Scenario: A reference without a version to an unknown vocabulary is a validation error
    Given the vocabulary "Anchor" exists
    When I create the vocabulary "User" with the prefix "ghost" for "{{base}}/content/{{name:Ghost}}#"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with detail "Referenced internal vocabulary '{{name:Ghost}}' does not exist."
    And no vocabulary named "User" is stored

  @AC_SIMPL-18184_AC3 @D12 @D47 @negative
  Scenario: A versioned reference to an unknown vocabulary is a validation error
    Given the vocabulary "Anchor" exists
    When I create the vocabulary "User" with the prefix "ghost" for "{{base}}/content/{{name:Ghost}}/versions/1#"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with detail "Referenced internal vocabulary '{{name:Ghost}}' version 1 does not exist."
    And no vocabulary named "User" is stored

  @D12
  Scenario: A reference to the vocabulary being created is ignored
    Given the vocabulary "Anchor" exists
    When I create the vocabulary "Self" with the prefix "me" for "{{base}}/content/{{name:Self}}/versions/1#"
    Then the response status is 201
    And the response has no validation warnings

  @D12 @D11
  Scenario: Several references each give their own result
    Given the vocabulary "Old" exists with 2 versions
    And the vocabulary "Fresh" exists
    When I create the vocabulary "User" with the prefixes:
      | prefix | namespace                                     |
      | old    | {{base}}/content/{{name:Old}}/versions/1#     |
      | fresh  | {{base}}/content/{{name:Fresh}}/versions/1#   |
      | dct    | {{ext:Unregistered}}/                         |
    Then the response status is 201
    And the response has 2 validation warnings
    And the response has a validation warning "deprecatedVocabularyReference" with location "prefix 'old' -> '{{base}}/content/{{name:Old}}/versions/1#'"
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'dct' -> '{{ext:Unregistered}}/'"
