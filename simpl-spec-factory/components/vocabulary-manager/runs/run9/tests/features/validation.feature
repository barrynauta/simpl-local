Feature: Validation of uploaded vocabulary content
  Two steps on every create and new version: syntax, then semantic (D9). Errors block with
  400 VocabularyValidationProblem (D10, D14); nothing is stored (D8). Locations follow D65.

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18170_AC3 @D9 @D10 @D8 @D65 @negative
  Scenario: A create with invalid Turtle is rejected with one syntax issue and nothing is stored
    When I create the vocabulary "Broken" with the file "syntax-error.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issues
    And every issue is in "body" named "vocabularyFile" with type "urn:problem-type:belgif:input-validation:invalidInput"
    And the problem has an issue whose detail starts with "Encountered invalid Turtle syntax" and whose location matches "^line 3(, column \d+)?$"
    And no vocabulary named "Broken" is stored

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18173_AC2 @AC_SIMPL-18194_AC1 @D9 @D8 @negative
  Scenario: A new version with invalid Turtle is rejected, no version is created and none is deprecated
    Given a vocabulary "Health" exists
    When I create a new version of "Health" with the file "syntax-error.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issues
    And the problem has an issue whose detail starts with "Encountered invalid Turtle syntax" and whose location matches "^line 3(, column \d+)?$"
    And the vocabulary "Health" has 1 versions
    And version 1 of "Health" has the status "ACTIVE"
    And the vocabulary "Health" has the status "ACTIVE"

  @AC_SIMPL-18181_AC2 @D9 @negative
  Scenario: Semantic validation does not run when the syntax check fails
    When I create the vocabulary "Mixed" with the file "syntax-after-semantic.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issues
    And the problem has an issue whose detail starts with "Encountered invalid Turtle syntax" and whose location matches "^line 5(, column \d+)?$"

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18170_AC3 @D9 @D10 @D65 @D8 @negative
  Scenario Outline: A semantic error is reported with its detail and location and nothing is stored (<rule>)
    When I create the vocabulary "Sem" with the file "<fixture>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issues
    And every issue is in "body" named "vocabularyFile" with type "urn:problem-type:belgif:input-validation:invalidInput"
    And the problem has an issue with the detail "<detail>" and a location matching "<location>"
    And no vocabulary named "Sem" is stored

    Examples:
      | rule | fixture                            | detail                                                 | location                                                                                                          |
      | S1   | s1-literal-object-property.ttl     | Literal value for object property                      | ^statement 'https://example\.org/vm/${run}/s1#subject https://example\.org/vm/${run}/s1#partOf \x22?invalid-literal\x22?'$ |
      | S2   | s2-resource-datatype-property.ttl  | Resource value for datatype property                   | ^statement 'https://example\.org/vm/${run}/s2#subject https://example\.org/vm/${run}/s2#code https://example\.org/vm/${run}/s2#Other'$ |
      | S3   | s3-both-property-kinds.ttl         | Property declared as both object and datatype property | ^resource 'https://example\.org/vm/${run}/s3#prop'$                                                               |
      | S4   | s4-duplicate-label.ttl             | Duplicate preferred label for the same language        | ^resource 'https://example\.org/vm/${run}/s4#c', language 'en'$                                                   |
      | S5   | s5-disjoint.ttl                    | Resource is an instance of disjoint classes            | ^resource 'https://example\.org/vm/${run}/s5#x'$                                                                  |
      | S6   | s6-comments-only.ttl               | Vocabulary contains no statements                      | ^document$                                                                                                        |

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18173_AC2 @D9 @D8 @negative
  Scenario: A semantic error on a new version leaves the vocabulary unchanged
    Given a vocabulary "Health" exists
    When I create a new version of "Health" with the file "s5-disjoint.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with the detail "Resource is an instance of disjoint classes"
    And the vocabulary "Health" has 1 versions
    And version 1 of "Health" has the status "ACTIVE"

  @AC_SIMPL-18194_AC1 @AC_SIMPL-18183_AC2 @D9 @negative
  Scenario: All semantic errors are returned in one response
    When I create the vocabulary "Multi" with the file "multi-semantic.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 3 issues
    And every issue has a detail and a location
    And the problem has an issue with the detail "Literal value for object property"
    And the problem has an issue with the detail "Duplicate preferred label for the same language"
    And the problem has an issue with the detail "Resource is an instance of disjoint classes"
    And no vocabulary named "Multi" is stored

  @D53 @D65 @negative
  Scenario: Two untagged preferred labels on one subject are an S4 error with language none
    When I create the vocabulary "Untagged" with the file "s4-untagged-duplicate.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with the detail "Duplicate preferred label for the same language" and the location "resource 'https://example.org/vm/${run}/s4u#c', language 'none'"

  @D53
  Scenario: One untagged and one tagged preferred label are allowed
    When I create the vocabulary "Mixed" with the file "s4-untagged-and-tagged.ttl"
    Then the response status is 201

  @D57 @D65 @negative
  Scenario: Relative IRIs resolve against the content URL of the new version
    When I create the vocabulary "Relative" with the file "s5-relative-iris.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with the detail "Resource is an instance of disjoint classes" and the location "resource '${contentBase}/content/${Relative}/versions/1#x'"

  @D70 @D9 @negative
  Scenario: An empty file passes the field check and fails validation with S6
    When I create the vocabulary "Empty" with an empty file
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "vocabularyFile"
    And the problem has an issue with the detail "Vocabulary contains no statements" and the location "document"
    And no vocabulary named "Empty" is stored

  @D70 @D9 @negative
  Scenario: A new version with an empty file fails validation with S6
    Given a vocabulary "Health" exists
    When I create a new version of "Health" with an empty file
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with the detail "Vocabulary contains no statements" and the location "document"
    And the vocabulary "Health" has 1 versions

  @D9 @D15 @negative
  Scenario: A binary file is not valid Turtle and gives a syntax error
    When I send a multipart POST to "/vocabularies" with
      | part           | value                                        |
      | name           | Binary${run}                                 |
      | description    | Description                                  |
      | changelog      | Initial version.                             |
      | vocabularyFile | @text-file:%PDF-1.4 binary-like, not turtle ( |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issues
    And the problem has an issue whose detail starts with "Encountered invalid Turtle syntax" and whose location matches "^line \d+(, column \d+)?$"
    And no vocabulary of this scenario is stored

  @AC_SIMPL-18194_AC2 @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC1 @D10 @D14
  Scenario: A valid new version is confirmed by the 201 response without error details
    Given a vocabulary "Health" exists
    When I create a new version of "Health"
    Then the response status is 201
    And the response has no field "issues" or it is an empty list
    And there are no validation warnings
    And every validation warning code is in the closed set
