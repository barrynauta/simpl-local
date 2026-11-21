Feature: Validation of vocabulary content on create and new version
  Syntax first, then semantic rules S1 to S7, all errors in one report (D9, D10, D65).
  Warnings never block (D10, D11, D12, D47). Nothing is stored on failure (D8).

  Background:
    Given I am authenticated as "ga-admin"

  # ------------------------------------------------------------ syntax

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18194_AC1 @D9 @D10 @D65 @D8 @negative
  Scenario: Invalid Turtle is rejected with one syntax issue and nothing is stored
    When I create vocabulary "{{Health}}" with file "syntax-error.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And every issue has type "urn:problem-type:belgif:input-validation:invalidInput", in "body" and name "vocabularyFile"
    And issue 0 has detail starting with "Encountered invalid Turtle syntax"
    And issue 0 has location matching "line 3(, column \d+)?"
    And no vocabulary named "{{Health}}" is stored

  @AC_SIMPL-18181_AC2 @D9 @negative
  Scenario: Semantic validation does not run when syntax validation fails
    When I create vocabulary "{{Health}}" with file "syntax-error-with-semantic-issues.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And issue 0 has detail starting with "Encountered invalid Turtle syntax"
    And issue 0 has location matching "line 4(, column \d+)?"

  @AC_SIMPL-18181_AC2 @AC_SIMPL-18194_AC1 @D8 @D4 @negative
  Scenario: A new version with invalid Turtle is rejected and no previous version is deprecated
    Given a vocabulary "{{Health}}" exists
    When I create a new version of "{{Health}}" with file "syntax-error.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And issue 0 has detail starting with "Encountered invalid Turtle syntax"
    And vocabulary "{{Health}}" has 1 version
    And version 1 of vocabulary "{{Health}}" has status "ACTIVE"

  @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC1 @AC_SIMPL-18194_AC2 @D9 @D10
  Scenario: A valid new version passes syntax and semantic validation
    Given a vocabulary "{{Health}}" exists
    When I create a new version of "{{Health}}"
    Then the status is 201
    And there are no validation warnings

  # ------------------------------------------------------------ semantic rules

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @D9 @D65 @D140 @negative
  Scenario: S1 a literal as object of an object property
    When I create vocabulary "{{Health}}" with content
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <https://example.org/vm/{{run}}#> .
      ex:hasPart a owl:ObjectProperty .
      ex:Thing ex:hasPart "a literal"@en .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And every issue has type "urn:problem-type:belgif:input-validation:invalidInput", in "body" and name "vocabularyFile"
    And the problem has an issue with detail "Literal value for object property" and the location
      """
      statement 'https://example.org/vm/{{run}}#Thing https://example.org/vm/{{run}}#hasPart "a literal"@en'
      """
    And no vocabulary named "{{Health}}" is stored

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: S2 a resource as object of a datatype property
    When I create vocabulary "{{Health}}" with content
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <https://example.org/vm/{{run}}#> .
      ex:size a owl:DatatypeProperty .
      ex:Thing ex:size ex:Other .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And the problem has an issue with detail "Resource value for datatype property" and location "statement 'https://example.org/vm/{{run}}#Thing https://example.org/vm/{{run}}#size https://example.org/vm/{{run}}#Other'"

  @AC_SIMPL-18183_AC2 @D9 @D65 @D144 @negative
  Scenario: S3 a property declared both object and datatype property gives one issue per property
    When I create vocabulary "{{Health}}" with content
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <https://example.org/vm/{{run}}#> .
      ex:both a owl:DatatypeProperty .
      ex:Thing a owl:Class .
      ex:both a owl:ObjectProperty .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And the problem has an issue with detail "Property declared as both object and datatype property" and location "resource 'https://example.org/vm/{{run}}#both'"

  @AC_SIMPL-18183_AC2 @D9 @D65 @D142 @negative
  Scenario: S4 two preferred labels with the same language tag, compared case-insensitively
    When I create vocabulary "{{Health}}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vm/{{run}}#> .
      ex:Concept a skos:Concept ;
          skos:prefLabel "First"@en , "Second"@EN .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And the problem has an issue with detail "Duplicate preferred label for the same language" and location "resource 'https://example.org/vm/{{run}}#Concept', language 'en'"

  @AC_SIMPL-18183_AC2 @D53 @D65 @negative
  Scenario: S4 two preferred labels without a language tag
    When I create vocabulary "{{Health}}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vm/{{run}}#> .
      ex:Concept a skos:Concept ;
          skos:prefLabel "First" , "Second" .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And the problem has an issue with detail "Duplicate preferred label for the same language" and location "resource 'https://example.org/vm/{{run}}#Concept', language 'none'"

  @AC_SIMPL-18183_AC1 @D53
  Scenario: One untagged and one tagged preferred label are allowed
    When I create vocabulary "{{Health}}" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vm/{{run}}#> .
      ex:Concept a skos:Concept ;
          skos:prefLabel "Plain" , "English"@en .
      """
    Then the status is 201

  @AC_SIMPL-18183_AC2 @D9 @D65 @D143 @negative
  Scenario: S5 an instance of disjoint classes gives one issue per resource
    When I create vocabulary "{{Health}}" with content
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <https://example.org/vm/{{run}}#> .
      ex:Cat a owl:Class ; owl:disjointWith ex:Dog , ex:Fish .
      ex:Dog a owl:Class ; owl:disjointWith ex:Fish .
      ex:Fish a owl:Class .
      ex:odd a ex:Cat , ex:Dog , ex:Fish .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And the problem has an issue with detail "Resource is an instance of disjoint classes" and location "resource 'https://example.org/vm/{{run}}#odd'"

  @AC_SIMPL-18183_AC2 @D9 @D65 @negative
  Scenario: S6 a file with only comments has no statements
    When I create vocabulary "{{Health}}" with file "comments-only.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And the problem has an issue with detail "Vocabulary contains no statements" and location "document"
    And no vocabulary named "{{Health}}" is stored

  @AC_SIMPL-18183_AC2 @AC_SIMPL-18194_AC1 @AC_SIMPL-18180_AC7 @D9 @negative
  Scenario: All semantic errors are returned in one report
    When I create vocabulary "{{Health}}" with content
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vm/{{run}}#> .
      ex:hasPart a owl:ObjectProperty .
      ex:Thing ex:hasPart "text" .
      ex:both a owl:ObjectProperty , owl:DatatypeProperty .
      ex:Concept skos:prefLabel "One"@fr , "Two"@fr .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 3 issues
    And every issue has type "urn:problem-type:belgif:input-validation:invalidInput", in "body" and name "vocabularyFile"
    And the problem has an issue with detail "Property declared as both object and datatype property" and location "resource 'https://example.org/vm/{{run}}#both'"
    And the problem has an issue with detail "Duplicate preferred label for the same language" and location "resource 'https://example.org/vm/{{run}}#Concept', language 'fr'"
    And the problem has 1 issues with detail "Literal value for object property"
    And no vocabulary named "{{Health}}" is stored

  @AC_SIMPL-18183_AC2 @D9 @D8 @negative
  Scenario: A new version with semantic errors is not created and nothing changes
    Given a vocabulary "{{Health}}" exists
    When I create a new version of "{{Health}}" with content
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <https://example.org/vm/{{run}}#> .
      ex:both a owl:ObjectProperty , owl:DatatypeProperty .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with detail "Property declared as both object and datatype property" and location "resource 'https://example.org/vm/{{run}}#both'"
    And vocabulary "{{Health}}" has 1 version
    And version 1 of vocabulary "{{Health}}" has status "ACTIVE"

  @D57 @D65 @negative
  Scenario: Relative IRIs resolve against the content URL of the new version
    When I create vocabulary "{{Health}}" with content
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      <#prop> a owl:ObjectProperty , owl:DatatypeProperty .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with detail "Property declared as both object and datatype property" and location "resource '{{contentBase}}/content/{{Health}}/versions/1#prop'"

  @D57 @negative
  Scenario: Relative IRIs in a new version resolve against that version's content URL
    Given a vocabulary "{{Health}}" exists
    When I create a new version of "{{Health}}" with content
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      <#prop> a owl:ObjectProperty , owl:DatatypeProperty .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with detail "Property declared as both object and datatype property" and location "resource '{{contentBase}}/content/{{Health}}/versions/2#prop'"

  @D125 @NFR-04 @Q2 @negative
  Scenario: A report with more than 1000 errors holds the first 1000 and is marked truncated
    When I create vocabulary "{{Health}}" with 1001 properties declared both object and datatype property
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1000 issues
    And the response field "truncated" equals true
    And the response field "issues.0.location" equals "resource 'https://example.org/vm/{{run}}#p00000'"
    And no vocabulary named "{{Health}}" is stored

  @D125 @NFR-04 @Q2 @negative
  Scenario: A report with exactly 1000 errors is not truncated
    When I create vocabulary "{{Health}}" with 1000 properties declared both object and datatype property
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1000 issues
    And the response field "truncated" is not true

  # ------------------------------------------------------------ external references and warnings

  @AC_SIMPL-18183_AC1 @D10 @D12
  Scenario: An unregistered external namespace gives a warning and the vocabulary is created
    When I create vocabulary "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}#> .
      @prefix unk: <https://example.org/unregistered/{{run}}/> .
      ex:Thing unk:label "x" .
      """
    Then the status is 201
    And the warning codes are "externalReferenceSkipped"
    And there is a warning "externalReferenceSkipped" with location "prefix 'unk' -> 'https://example.org/unregistered/{{run}}/'"
    And the response field "validationWarnings.0.description" is present

  @D12
  Scenario: The W3C core namespaces and the vocabulary's own namespace give no warning
    When I create vocabulary "{{Health}}" with content
      """
      @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
      @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix own: <https://example.org/own/{{run}}/> .
      own:Thing a owl:Class ; rdfs:label "Thing"^^xsd:string .
      """
    Then the status is 201
    And there are no validation warnings

  @D12 @D30 @D42
  Scenario: A registered external namespace or alias gives no warning, compared without a trailing slash or hash
    Given an external vocabulary "{{Ext}}" is registered with namespace "https://example.org/ext/{{run}}/terms" and aliases "https://alias.example.org/{{run}}/terms#"
    When I create vocabulary "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}#> .
      @prefix dct: <https://example.org/ext/{{run}}/terms/> .
      @prefix ali: <https://alias.example.org/{{run}}/terms/> .
      ex:Thing dct:title "x" ; ali:creator "y" .
      """
    Then the status is 201
    And there are no validation warnings

  @D42 @D12
  Scenario: After an external vocabulary is removed, a reference to its namespace gives a warning again
    Given an external vocabulary "{{Ext}}" is registered with namespace "https://example.org/ext/{{run}}/terms/"
    And a vocabulary "{{Health}}" exists with content
      """
      @prefix ex: <https://example.org/vm/{{run}}#> .
      @prefix dct: <https://example.org/ext/{{run}}/terms/> .
      ex:Thing dct:title "x" .
      """
    And external vocabulary "{{Ext}}" is deleted
    When I send "GET" to "/vocabularies/{{Health}}/versions/1"
    Then the response field "status" equals "ACTIVE"
    When I create a new version of "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}#> .
      @prefix dct: <https://example.org/ext/{{run}}/terms/> .
      ex:Thing dct:title "changed" .
      """
    Then the status is 201
    And there is a warning "externalReferenceSkipped" with location "prefix 'dct' -> 'https://example.org/ext/{{run}}/terms/'"

  @D42
  Scenario: Registering the external vocabulary later clears the warning on the next version
    Given a vocabulary "{{Health}}" exists with content
      """
      @prefix ex: <https://example.org/vm/{{run}}#> .
      @prefix dct: <https://example.org/ext/{{run}}/terms/> .
      ex:Thing dct:title "x" .
      """
    And an external vocabulary "{{Ext}}" is registered with namespace "https://example.org/ext/{{run}}/terms/"
    When I create a new version of "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}#> .
      @prefix dct: <https://example.org/ext/{{run}}/terms/> .
      ex:Thing dct:title "changed" .
      """
    Then the status is 201
    And there are no validation warnings

  @D145 @D12
  Scenario: A namespace declared under two prefixes is checked once
    When I create vocabulary "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}#> .
      @prefix unk: <https://example.org/unregistered/{{run}}/> .
      @prefix unk2: <https://example.org/unregistered/{{run}}/> .
      ex:Thing unk:label "x" .
      """
    Then the status is 201
    And the warning codes are "externalReferenceSkipped"
    And there is a warning "externalReferenceSkipped" with location "prefix 'unk' -> 'https://example.org/unregistered/{{run}}/'"

  @D69 @D12
  Scenario Outline: A content URL without a valid vocabulary name is an external reference (<case>)
    When I create vocabulary "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}#> .
      @prefix odd: <{{contentBase}}/content/<suffix>> .
      ex:Thing ex:label "x" .
      """
    Then the status is 201
    And there is a warning "externalReferenceSkipped" with location "prefix 'odd' -> '{{contentBase}}/content/<suffix>'"

    Examples:
      | case             | suffix               |
      | no name          |                      |
      | lower-case name  | lower{{run}}#        |

  # ------------------------------------------------------------ internal references

  @AC_SIMPL-18184_AC1 @D11 @D12
  Scenario: A reference to an ACTIVE internal version gives no warning
    Given a vocabulary "{{Base}}" exists
    And a vocabulary "{{Health}}" exists
    When I create a new version of "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix base: <{{contentBase}}/content/{{Base}}/versions/1#> .
      ex:Thing ex:basedOn base:Scheme .
      """
    Then the status is 201
    And there are no validation warnings

  @AC_SIMPL-18184_AC1 @D12 @D5
  Scenario: A reference without a version resolves to the latest version
    Given a vocabulary "{{Base}}" exists
    And vocabulary "{{Base}}" has 1 more versions
    When I create vocabulary "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix base: <{{contentBase}}/content/{{Base}}#> .
      ex:Thing ex:basedOn base:Scheme .
      """
    Then the status is 201
    And there are no validation warnings

  @AC_SIMPL-18184_AC2 @D11 @D47 @D65
  Scenario: A reference to a deprecated internal version warns and recommends the latest ACTIVE version
    Given a vocabulary "{{Base}}" exists
    And vocabulary "{{Base}}" has 1 more versions
    And a vocabulary "{{Health}}" exists
    When I create a new version of "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix base: <{{contentBase}}/content/{{Base}}/versions/1#> .
      ex:Thing ex:basedOn base:Scheme .
      """
    Then the status is 201
    And the warning codes are "deprecatedVocabularyReference"
    And there is a warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '{{Base}}' version 1 is deprecated. Use version 2, the latest ACTIVE version." and location "prefix 'base' -> '{{contentBase}}/content/{{Base}}/versions/1#'"
    And vocabulary "{{Health}}" has 2 versions

  @AC_SIMPL-18184_AC2 @D11 @D47
  Scenario: The deprecated-reference warning also applies when creating a vocabulary
    Given a vocabulary "{{Base}}" exists
    And vocabulary "{{Base}}" has 1 more versions
    When I create vocabulary "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix base: <{{contentBase}}/content/{{Base}}/versions/1/> .
      ex:Thing ex:basedOn base:Scheme .
      """
    Then the status is 201
    And there is a warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '{{Base}}' version 1 is deprecated. Use version 2, the latest ACTIVE version." and location "prefix 'base' -> '{{contentBase}}/content/{{Base}}/versions/1/'"

  @AC_SIMPL-18184_AC2 @D11 @D47
  Scenario: Without another ACTIVE version no version is recommended
    Given a vocabulary "{{Base}}" exists
    And version 1 of vocabulary "{{Base}}" has status "DEPRECATED"
    When I create vocabulary "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix base: <{{contentBase}}/content/{{Base}}/versions/1#> .
      ex:Thing ex:basedOn base:Scheme .
      """
    Then the status is 201
    And there is a warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '{{Base}}' version 1 is deprecated." and location "prefix 'base' -> '{{contentBase}}/content/{{Base}}/versions/1#'"

  @AC_SIMPL-18184_AC2 @D3 @D11 @D47
  Scenario: A version of a DEPRECATED vocabulary is effectively deprecated and no version is recommended
    Given a vocabulary "{{Base}}" exists
    And vocabulary "{{Base}}" has 1 more versions
    And vocabulary "{{Base}}" has status "DEPRECATED"
    When I create vocabulary "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix base: <{{contentBase}}/content/{{Base}}/versions/2#> .
      ex:Thing ex:basedOn base:Scheme .
      """
    Then the status is 201
    And there is a warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '{{Base}}' version 2 is deprecated. The vocabulary '{{Base}}' is deprecated." and location "prefix 'base' -> '{{contentBase}}/content/{{Base}}/versions/2#'"

  @AC_SIMPL-18184_AC2 @D3 @D11 @D12 @D47
  Scenario: A reference without a version to a DEPRECATED vocabulary warns about its latest version
    Given a vocabulary "{{Base}}" exists
    And vocabulary "{{Base}}" has status "DEPRECATED"
    When I create vocabulary "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix base: <{{contentBase}}/content/{{Base}}#> .
      ex:Thing ex:basedOn base:Scheme .
      """
    Then the status is 201
    And there is a warning "deprecatedVocabularyReference" with description "Referenced internal vocabulary '{{Base}}' version 1 is deprecated. The vocabulary '{{Base}}' is deprecated." and location "prefix 'base' -> '{{contentBase}}/content/{{Base}}#'"

  @AC_SIMPL-18184_AC3 @D12 @D47 @D65 @D8 @negative
  Scenario: A reference to an internal version that does not exist is a validation error
    Given a vocabulary "{{Base}}" exists
    And a vocabulary "{{Health}}" exists
    When I create a new version of "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix base: <{{contentBase}}/content/{{Base}}/versions/9#> .
      ex:Thing ex:basedOn base:Scheme .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And the problem has an issue with detail "Referenced internal vocabulary '{{Base}}' version 9 does not exist." and location "prefix 'base' -> '{{contentBase}}/content/{{Base}}/versions/9#'"
    And vocabulary "{{Health}}" has 1 version
    And version 1 of vocabulary "{{Health}}" has status "ACTIVE"

  @AC_SIMPL-18184_AC3 @D47 @D12 @negative
  Scenario: A reference to an internal vocabulary name that does not exist is a validation error
    When I create vocabulary "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix nope: <{{contentBase}}/content/{{Nope}}#> .
      ex:Thing ex:basedOn nope:Scheme .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with detail "Referenced internal vocabulary '{{Nope}}' does not exist." and location "prefix 'nope' -> '{{contentBase}}/content/{{Nope}}#'"
    And no vocabulary named "{{Health}}" is stored

  @D69 @negative
  Scenario: A non-numeric version in an internal reference is a validation error
    Given a vocabulary "{{Base}}" exists
    When I create vocabulary "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix base: <{{contentBase}}/content/{{Base}}/versions/abc#> .
      ex:Thing ex:basedOn base:Scheme .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with detail "Referenced internal vocabulary '{{Base}}' version abc does not exist." and location "prefix 'base' -> '{{contentBase}}/content/{{Base}}/versions/abc#'"

  @D146
  Scenario: Another path under an internal name refers to its latest version
    Given a vocabulary "{{Base}}" exists
    When I create vocabulary "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix base: <{{contentBase}}/content/{{Base}}/terms/> .
      ex:Thing ex:basedOn base:Scheme .
      """
    Then the status is 201
    And there are no validation warnings

  @D146 @negative
  Scenario: Another path under an unknown internal name is a validation error
    When I create vocabulary "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix nope: <{{contentBase}}/content/{{Nope}}/terms/> .
      ex:Thing ex:basedOn nope:Scheme .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with detail "Referenced internal vocabulary '{{Nope}}' does not exist." and location "prefix 'nope' -> '{{contentBase}}/content/{{Nope}}/terms/'"

  @D12
  Scenario: A reference to the vocabulary being versioned is ignored
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 1 more versions
    When I create a new version of "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix self: <{{contentBase}}/content/{{Health}}/versions/1#> .
      @prefix future: <{{contentBase}}/content/{{Health}}/versions/99#> .
      ex:Thing ex:basedOn self:Scheme .
      """
    Then the status is 201
    And there are no validation warnings

  @D12
  Scenario: A reference to the vocabulary being created is ignored
    When I create vocabulary "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix self: <{{contentBase}}/content/{{Health}}#> .
      ex:Thing ex:basedOn self:Scheme .
      """
    Then the status is 201
    And there are no validation warnings

  @D145 @D11
  Scenario: A deprecated internal namespace declared twice gives one warning
    Given a vocabulary "{{Base}}" exists
    And version 1 of vocabulary "{{Base}}" has status "DEPRECATED"
    When I create vocabulary "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix b1: <{{contentBase}}/content/{{Base}}/versions/1#> .
      @prefix b2: <{{contentBase}}/content/{{Base}}/versions/1#> .
      ex:Thing ex:basedOn b1:Scheme .
      """
    Then the status is 201
    And the warning codes are "deprecatedVocabularyReference"
    And there is a warning "deprecatedVocabularyReference" with location "prefix 'b1' -> '{{contentBase}}/content/{{Base}}/versions/1#'"

  @D10 @D11 @D12
  Scenario: Several warnings are returned together and the creation still succeeds
    Given a vocabulary "{{Base}}" exists
    And version 1 of vocabulary "{{Base}}" has status "DEPRECATED"
    When I create vocabulary "{{Health}}" with content
      """
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix base: <{{contentBase}}/content/{{Base}}/versions/1#> .
      @prefix unk: <https://example.org/unregistered/{{run}}/> .
      ex:Thing ex:basedOn base:Scheme ; unk:label "x" .
      """
    Then the status is 201
    And the warning codes are "deprecatedVocabularyReference, externalReferenceSkipped"

  @D9 @D12 @negative
  Scenario: A missing internal reference is reported together with other semantic errors
    When I create vocabulary "{{Health}}" with content
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix ex: <https://example.org/vm/{{run}}/health#> .
      @prefix nope: <{{contentBase}}/content/{{Nope}}#> .
      ex:both a owl:ObjectProperty , owl:DatatypeProperty .
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 2 issues
    And the problem has an issue with detail "Referenced internal vocabulary '{{Nope}}' does not exist." and location "prefix 'nope' -> '{{contentBase}}/content/{{Nope}}#'"
