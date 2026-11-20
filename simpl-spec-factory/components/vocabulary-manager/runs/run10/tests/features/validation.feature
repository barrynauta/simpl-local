@validation
Feature: Syntax and semantic validation of uploaded vocabulary content
  Two steps on every create and new version: syntax, then semantic (D9). A validation error blocks
  with a 400 validation report; a validation warning never blocks and comes back in
  validationWarnings of the 201 response (D10).

  Background:
    Given I use the token of "ga-admin"

  # ---------------------------------------------------------------- syntax

  @D48 @AC_SIMPL-18181_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18170_AC3 @D9 @D10 @D8 @D60 @D65 @negative
  Scenario: Invalid Turtle on create gives one syntax issue with its line, and nothing is stored
    When I create the vocabulary "A" with the file "syntax_error"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And every issue is in "body" named "vocabularyFile"
    And every issue has the type "urn:problem-type:belgif:input-validation:invalidInput"
    And the problem has an issue with a detail starting with "Encountered invalid Turtle syntax" and the location matching "^line 3(, column \d+)?$"
    And the vocabulary list filtered by name "${A}" has 0 items

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18173_AC2 @D9 @D8 @negative
  Scenario: Invalid Turtle on a new version is rejected; no version is created and none is deprecated
    Given a vocabulary "A" exists
    When I create a new version of "A" with the file "syntax_error"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue with a detail starting with "Encountered invalid Turtle syntax" and the location matching "^line 3(, column \d+)?$"
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has the status "ACTIVE"

  @AC_SIMPL-18181_AC2 @D9 @negative
  Scenario: Semantic validation does not run after a syntax error
    # The file also holds a duplicate preferred label after the broken line; only the syntax issue is reported.
    When I create the vocabulary "A" with the file "syntax_error"
    Then the problem has exactly 1 issue
    And no issue has a detail starting with "Duplicate preferred label"

  @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC1 @AC_SIMPL-18194_AC2 @D10 @D14
  Scenario: Valid content passes both steps and the 201 response carries an empty warnings array
    When I create the vocabulary "A" with the file "valid"
    Then the status is 201
    And the response has no validation warnings
    When I create a new version of "A" with the file "valid"
    Then the status is 201
    And the response has no validation warnings

  # ---------------------------------------------------------------- semantic rules S1 to S6

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @D9 @D10 @D65 @D8 @negative
  Scenario: S1 a literal as object of an object property is an error
    When I create the vocabulary "A" with the file "s1_literal_for_object_property"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And every issue is in "body" named "vocabularyFile"
    And every issue has the type "urn:problem-type:belgif:input-validation:invalidInput"
    And the problem has an issue with the detail "Literal value for object property" and a location containing "statement '${ns:A}Thing ${ns:A}linksTo | invalid-literal"
    And the vocabulary list filtered by name "${A}" has 0 items

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: S2 a resource as object of a datatype property is an error
    When I create the vocabulary "A" with the file "s2_resource_for_datatype_property"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue with the detail "Resource value for datatype property" and the location "statement '${ns:A}Thing ${ns:A}hasCode ${ns:A}Other'"
    And the vocabulary list filtered by name "${A}" has 0 items

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: S3 a property declared both object and datatype property is an error
    When I create the vocabulary "A" with the file "s3_both_property_kinds"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue with the detail "Property declared as both object and datatype property" and the location "resource '${ns:A}mixed'"

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: S4 two preferred labels with the same language tag are an error
    When I create the vocabulary "A" with the file "s4_duplicate_label"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue with the detail "Duplicate preferred label for the same language" and the location "resource '${ns:A}Concept', language 'en'"

  @AC_SIMPL-18183_AC2 @D53 @D65 @negative
  Scenario: S4 two untagged preferred labels count as one language
    When I create the vocabulary "A" with the file "s4_duplicate_untagged"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue with the detail "Duplicate preferred label for the same language" and the location "resource '${ns:A}Concept', language 'none'"

  @AC_SIMPL-18183_AC1 @D53
  Scenario: S4 one untagged and one tagged preferred label are not an error
    When I create the vocabulary "A" with the file "s4_untagged_and_tagged"
    Then the status is 201

  @AC_SIMPL-18183_AC2 @D65 @negative
  Scenario: S4 gives one issue per subject and language
    When I create the vocabulary "A" with the file "s4_two_subjects"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 2 issues
    And the problem has an issue with the detail "Duplicate preferred label for the same language" and the location "resource '${ns:A}First', language 'en'"
    And the problem has an issue with the detail "Duplicate preferred label for the same language" and the location "resource '${ns:A}Second', language 'fr'"

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: S5 a resource typed with two disjoint classes is an error
    When I create the vocabulary "A" with the file "s5_disjoint_classes"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue with the detail "Resource is an instance of disjoint classes" and the location "resource '${ns:A}Felix'"

  @AC_SIMPL-18183_AC2 @D57 @D65 @negative
  Scenario: Relative IRIs resolve against the content URL of the new version
    When I create the vocabulary "A" with the file "s5_relative_iris"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with the detail "Resource is an instance of disjoint classes" and the location "resource '${base}/content/${A}/versions/1#Felix'"

  @AC_SIMPL-18183_AC2 @D57 @negative
  Scenario: Relative IRIs in a new version resolve against that version's content URL
    Given a vocabulary "A" exists
    When I create a new version of "A" with the file "s5_relative_iris"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with the detail "Resource is an instance of disjoint classes" and the location "resource '${base}/content/${A}/versions/2#Felix'"

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: S6 a file with prefixes and comments but no triples is an error
    When I create the vocabulary "A" with the file "s6_comments_only"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue with the detail "Vocabulary contains no statements" and the location "document"
    And the vocabulary list filtered by name "${A}" has 0 items

  @AC_SIMPL-18183_AC2 @D70 @negative
  Scenario: S6 a 0-byte file passes the field check and fails validation
    When I create the vocabulary "A" with the file "empty"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue with the detail "Vocabulary contains no statements" and the location "document"
    And the vocabulary list filtered by name "${A}" has 0 items

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @D9 @negative
  Scenario: All semantic errors are returned in one response
    When I create the vocabulary "A" with the file "multiple_errors"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 3 issues
    And every issue is in "body" named "vocabularyFile"
    And the problem has an issue with the detail "Literal value for object property"
    And the problem has an issue with the detail "Duplicate preferred label for the same language" and the location "resource '${ns:A}Concept', language 'en'"
    And the problem has an issue with the detail "Resource is an instance of disjoint classes" and the location "resource '${ns:A}Felix'"
    And the vocabulary list filtered by name "${A}" has 0 items

  @AC_SIMPL-18183_AC2 @D9 @D8 @negative
  Scenario: A semantic error on a new version creates nothing and deprecates nothing
    Given a vocabulary "A" exists
    When I create a new version of "A" with the file "multiple_errors"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 3 issues
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has the status "ACTIVE"

  # ---------------------------------------------------------------- internal references (S7, deprecated warnings)

  @AC_SIMPL-18184_AC1 @D11 @D12
  Scenario: A reference to an ACTIVE internal version gives no warning
    Given a vocabulary "R" exists
    And a vocabulary "A" exists
    And the file variable "ref" is "${base}/content/${R}/versions/1#"
    When I create a new version of "A" with the file "reference"
    Then the status is 201
    And the response has no validation warnings

  @AC_SIMPL-18184_AC1 @D12 @D5
  Scenario: A reference without a version resolves to the latest version and gives no warning when it is ACTIVE
    Given a vocabulary "R" exists with 2 versions
    And a vocabulary "A" exists
    And the file variable "ref" is "${base}/content/${R}#"
    When I create a new version of "A" with the file "reference"
    Then the status is 201
    And the response has no validation warnings

  @AC_SIMPL-18184_AC2 @D11 @D47 @D10 @D14
  Scenario: A reference to a deprecated version is a warning that recommends the latest ACTIVE version
    Given a vocabulary "R" exists with 2 versions
    And a vocabulary "A" exists
    And the file variable "ref" is "${base}/content/${R}/versions/1#"
    When I create a new version of "A" with the file "reference"
    Then the status is 201
    And the response has exactly 1 validation warning
    And there is a validation warning with the code "deprecatedVocabularyReference", the description "Referenced internal vocabulary '${R}' version 1 is deprecated. Use version 2, the latest ACTIVE version." and the location "prefix 'r' -> '${base}/content/${R}/versions/1#'"
    And version 1 of vocabulary "A" has the status "DEPRECATED"
    And version 1 of vocabulary "R" has the status "DEPRECATED"
    And version 2 of vocabulary "R" has the status "ACTIVE"

  @AC_SIMPL-18184_AC2 @D11 @D47
  Scenario: A deprecated reference with a '/' separator is recognised as internal
    Given a vocabulary "R" exists with 2 versions
    And a vocabulary "A" exists
    And the file variable "ref" is "${base}/content/${R}/versions/1/"
    When I create a new version of "A" with the file "reference"
    Then the status is 201
    And there is a validation warning with the code "deprecatedVocabularyReference", the description "Referenced internal vocabulary '${R}' version 1 is deprecated. Use version 2, the latest ACTIVE version." and the location "prefix 'r' -> '${base}/content/${R}/versions/1/'"

  @AC_SIMPL-18184_AC2 @D11 @D3 @D47
  Scenario: A reference into a DEPRECATED vocabulary warns without recommending a version
    Given a vocabulary "R" exists
    And vocabulary "R" has the status "DEPRECATED"
    And a vocabulary "A" exists
    And the file variable "ref" is "${base}/content/${R}/versions/1#"
    When I create a new version of "A" with the file "reference"
    Then the status is 201
    And there is a validation warning with the code "deprecatedVocabularyReference", the description "Referenced internal vocabulary '${R}' version 1 is deprecated. The vocabulary '${R}' is deprecated." and the location "prefix 'r' -> '${base}/content/${R}/versions/1#'"

  @AC_SIMPL-18184_AC2 @D11 @D47
  Scenario: A deprecated reference without any ACTIVE version gives only the first sentence
    Given a vocabulary "R" exists
    And version 1 of vocabulary "R" has the status "DEPRECATED"
    And a vocabulary "A" exists
    And the file variable "ref" is "${base}/content/${R}/versions/1#"
    When I create a new version of "A" with the file "reference"
    Then the status is 201
    And there is a validation warning with the code "deprecatedVocabularyReference", the description "Referenced internal vocabulary '${R}' version 1 is deprecated." and the location "prefix 'r' -> '${base}/content/${R}/versions/1#'"

  @AC_SIMPL-18184_AC2 @D5 @D11 @D12 @D47
  Scenario: A reference without a version to a deprecated latest version recommends the latest ACTIVE version
    Given a vocabulary "R" exists with 2 versions
    And version 1 of vocabulary "R" has the status "ACTIVE"
    And version 2 of vocabulary "R" has the status "DEPRECATED"
    And a vocabulary "A" exists
    And the file variable "ref" is "${base}/content/${R}#"
    When I create a new version of "A" with the file "reference"
    Then the status is 201
    And there is a validation warning with the code "deprecatedVocabularyReference", the description "Referenced internal vocabulary '${R}' version 2 is deprecated. Use version 1, the latest ACTIVE version." and the location "prefix 'r' -> '${base}/content/${R}#'"

  @AC_SIMPL-18184_AC2 @D12 @D47
  Scenario: A reference with nothing after the version number is recognised as internal
    Given a vocabulary "R" exists with 2 versions
    And a vocabulary "A" exists
    And the file variable "ref" is "${base}/content/${R}/versions/1"
    When I create a new version of "A" with the file "reference"
    Then the status is 201
    And there is a validation warning with the code "deprecatedVocabularyReference", the description "Referenced internal vocabulary '${R}' version 1 is deprecated. Use version 2, the latest ACTIVE version." and the location "prefix 'r' -> '${base}/content/${R}/versions/1'"

  @D12 @D10
  Scenario: The W3C namespaces rdf, rdfs, owl, xsd and skos give no warning
    When I create the vocabulary "A" with the file "w3c_namespaces"
    Then the status is 201
    And the response has no validation warnings

  @D11 @D47
  Scenario: A deprecated reference on create is a warning too
    Given a vocabulary "R" exists with 2 versions
    And the file variable "ref" is "${base}/content/${R}/versions/1#"
    When I create the vocabulary "A" with the file "reference"
    Then the status is 201
    And there is a validation warning with the code "deprecatedVocabularyReference", the description "Referenced internal vocabulary '${R}' version 1 is deprecated. Use version 2, the latest ACTIVE version." and the location "prefix 'r' -> '${base}/content/${R}/versions/1#'"

  @D11 @D10
  Scenario: Each deprecated reference gives its own warning
    Given a vocabulary "R" exists with 2 versions
    And a vocabulary "Q" exists with 2 versions
    And a vocabulary "A" exists
    And the file variable "ref" is "${base}/content/${R}/versions/1#"
    And the file variable "ref2" is "${base}/content/${Q}/versions/1#"
    When I create a new version of "A" with the file "two_references"
    Then the status is 201
    And the response has exactly 2 validation warnings
    And there is a validation warning with the code "deprecatedVocabularyReference" and the location "prefix 'r' -> '${base}/content/${R}/versions/1#'"
    And there is a validation warning with the code "deprecatedVocabularyReference" and the location "prefix 'q' -> '${base}/content/${Q}/versions/1#'"

  @AC_SIMPL-18184_AC3 @D12 @D47 @D8 @negative
  Scenario: A reference to a version that does not exist is error S7 and nothing is created
    Given a vocabulary "R" exists
    And a vocabulary "A" exists
    And the file variable "ref" is "${base}/content/${R}/versions/9#"
    When I create a new version of "A" with the file "reference"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue with the detail "Referenced internal vocabulary '${R}' version 9 does not exist." and the location "prefix 'r' -> '${base}/content/${R}/versions/9#'"
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has the status "ACTIVE"

  @AC_SIMPL-18184_AC3 @D12 @D47 @negative
  Scenario: A reference to a vocabulary name that does not exist is error S7
    Given a vocabulary "A" exists
    And the file variable "ref" is "${base}/content/${Nowhere}#"
    When I create a new version of "A" with the file "reference"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with the detail "Referenced internal vocabulary '${Nowhere}' does not exist." and the location "prefix 'r' -> '${base}/content/${Nowhere}#'"
    And vocabulary "A" has 1 version

  @AC_SIMPL-18184_AC3 @D12 @D47 @negative
  Scenario: A versioned reference to an unknown vocabulary is error S7 with the version
    Given a vocabulary "A" exists
    And the file variable "ref" is "${base}/content/${Nowhere}/versions/1#"
    When I create a new version of "A" with the file "reference"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with the detail "Referenced internal vocabulary '${Nowhere}' version 1 does not exist." and the location "prefix 'r' -> '${base}/content/${Nowhere}/versions/1#'"

  @D69 @D47 @negative
  Scenario: A reference with a version that is not a positive integer is error S7 with the version as given
    Given a vocabulary "R" exists
    And a vocabulary "A" exists
    And the file variable "ref" is "${base}/content/${R}/versions/abc#"
    When I create a new version of "A" with the file "reference"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with the detail "Referenced internal vocabulary '${R}' version abc does not exist." and the location "prefix 'r' -> '${base}/content/${R}/versions/abc#'"

  @D12 @D47 @negative
  Scenario: S7 also applies on create
    Given the file variable "ref" is "${base}/content/${Nowhere}/versions/2#"
    When I create the vocabulary "A" with the file "reference"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with the detail "Referenced internal vocabulary '${Nowhere}' version 2 does not exist."
    And the vocabulary list filtered by name "${A}" has 0 items

  @D69 @D12
  Scenario: A content namespace with a name that breaks the name pattern is external
    Given a vocabulary "A" exists
    And the file variable "ref" is "${base}/content/not-a-name#"
    When I create a new version of "A" with the file "reference"
    Then the status is 201
    And there is a validation warning with the code "externalReferenceSkipped" and the location "prefix 'r' -> '${base}/content/not-a-name#'"

  @D12
  Scenario: A reference to the vocabulary being versioned is ignored, even for an unknown version
    Given a vocabulary "A" exists
    And the file variable "ref" is "${base}/content/${A}/versions/99#"
    When I create a new version of "A" with the file "reference"
    Then the status is 201
    And the response has no validation warnings

  @D12
  Scenario: A reference to an older, deprecated version of the vocabulary being versioned is ignored
    Given a vocabulary "A" exists with 2 versions
    And the file variable "ref" is "${base}/content/${A}/versions/1#"
    When I create a new version of "A" with the file "reference"
    Then the status is 201
    And the response has no validation warnings

  # ---------------------------------------------------------------- external references

  @D10 @D12 @AC_SIMPL-18183_AC1
  Scenario: An unregistered external namespace gives the warning externalReferenceSkipped and the vocabulary is created
    Given the file variable "ref" is "${extns:unregistered}#"
    When I create the vocabulary "A" with the file "reference"
    Then the status is 201
    And the response has exactly 1 validation warning
    And there is a validation warning with the code "externalReferenceSkipped" and the location "prefix 'r' -> '${extns:unregistered}#'"
    And every validation warning code is one of "externalReferenceSkipped, deprecatedVocabularyReference, semanticReasoningSkipped"
    And the vocabulary list filtered by name "${A}" has 1 item

  @D10 @D12
  Scenario: The unregistered external namespace check runs on new versions too
    Given a vocabulary "A" exists
    And the file variable "ref" is "${extns:unregistered}/"
    When I create a new version of "A" with the file "reference"
    Then the status is 201
    And there is a validation warning with the code "externalReferenceSkipped" and the location "prefix 'r' -> '${extns:unregistered}/'"

  @D12 @D30
  Scenario Outline: A namespace that matches a registered external vocabulary gives no warning (<case>)
    Given an external vocabulary "E" is registered with namespace "${extns:dc}" and aliases "${extns:dcalias}/"
    And the file variable "ref" is "<reference>"
    When I create the vocabulary "A" with the file "reference"
    Then the status is 201
    And the response has no validation warnings

    Examples:
      | case                         | reference           |
      | namespace with '#'           | ${extns:dc}#        |
      | namespace with '/'           | ${extns:dc}/        |
      | namespace as registered      | ${extns:dc}         |
      | alias without trailing '/'   | ${extns:dcalias}    |
      | alias with trailing '#'      | ${extns:dcalias}#   |

  @D42 @D12
  Scenario: After an external vocabulary is removed, a new version that references it gets the warning again
    Given an external vocabulary "E" is registered with namespace "${extns:dc}"
    And the file variable "ref" is "${extns:dc}#"
    And a vocabulary "A" exists with the file "reference"
    When I send "DELETE" to "/external-vocabularies/${E}"
    Then the status is 204
    And vocabulary "A" has the status "ACTIVE"
    When I send "GET" to "/content/${A}/versions/1" with the header "Accept" set to "text/turtle"
    Then the body is the file of version 1 of "A"
    When I create a new version of "A" with the file "reference"
    Then the status is 201
    And there is a validation warning with the code "externalReferenceSkipped" and the location "prefix 'r' -> '${extns:dc}#'"

  @D42 @D12
  Scenario: Registering the external vocabulary later clears the warning on the next version
    Given the file variable "ref" is "${extns:late}#"
    And a vocabulary "A" exists with the file "reference"
    And an external vocabulary "E" is registered with namespace "${extns:late}"
    When I create a new version of "A" with the file "reference"
    Then the status is 201
    And the response has no validation warnings

  @D10
  Scenario: Validation warnings are not stored and not returned by other operations
    Given the file variable "ref" is "${extns:unregistered}#"
    When I create the vocabulary "A" with the file "reference"
    Then the status is 201
    When I send "GET" to "/vocabularies/${A}/versions/1"
    Then the status is 200
    And the response field "validationWarnings" is absent or empty
    When I send "GET" to "/vocabularies/${A}"
    Then the response field "validationWarnings" is absent or empty
