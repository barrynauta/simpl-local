Feature: Vocabulary references in uploaded content
  A vocabulary reference is a namespace declared with @prefix (D12). Internal references to
  effectively deprecated versions give a warning (D11, D47); internal references to versions that
  do not exist are error S7 (D12, D47, D69); unknown external namespaces give externalReferenceSkipped.

  @AC_SIMPL-18184_AC1 @D11 @D12
  Scenario: References to ACTIVE internal versions pass without a deprecated-version warning
    Given a vocabulary "Ref" exists
    And a vocabulary "Other" exists
    And a vocabulary "Health" exists
    When I create a new version of "Health" with a file that declares the prefix "ref" for "${contentBase}/content/${Ref}/versions/1#"
    Then the response status is 201
    And there are no validation warnings

  @AC_SIMPL-18184_AC1 @D12 @D5
  Scenario Outline: An internal reference in the form <form> is recognised
    Given a vocabulary "Ref" exists
    When I create the vocabulary "Health" with a file that declares the prefix "ref" for "<namespace>"
    Then the response status is 201
    And there are no validation warnings

    Examples:
      | form                    | namespace                                  |
      | latest with hash        | ${contentBase}/content/${Ref}#             |
      | latest with slash       | ${contentBase}/content/${Ref}/             |
      | latest with nothing     | ${contentBase}/content/${Ref}              |
      | version with slash      | ${contentBase}/content/${Ref}/versions/1/  |
      | version with nothing    | ${contentBase}/content/${Ref}/versions/1   |

  @AC_SIMPL-18184_AC2 @D11 @D47 @D10
  Scenario: A reference to a deprecated version is a warning that recommends the latest ACTIVE version
    Given a vocabulary "Ref" exists with 2 versions
    And a vocabulary "Health" exists
    When I create a new version of "Health" with a file that declares the prefix "legacy" for "${contentBase}/content/${Ref}/versions/1#"
    Then the response status is 201
    And the response field "version" is 2
    And the validation warnings are
      | code                          | description                                                                                         | location                                                         |
      | deprecatedVocabularyReference | Referenced internal vocabulary '${Ref}' version 1 is deprecated. Use version 2, the latest ACTIVE version. | prefix 'legacy' -> '${contentBase}/content/${Ref}/versions/1#' |
    And version 1 of "Ref" has the status "DEPRECATED"

  @AC_SIMPL-18184_AC2 @D11 @D47
  Scenario: A reference to a version of a DEPRECATED vocabulary says the vocabulary is deprecated and recommends nothing
    Given a vocabulary "Ref" exists
    And the vocabulary "Ref" was set to "DEPRECATED"
    And a vocabulary "Health" exists
    When I create a new version of "Health" with a file that declares the prefix "ref" for "${contentBase}/content/${Ref}/versions/1#"
    Then the response status is 201
    And the validation warnings are
      | code                          | description                                                                                    | location                                                      |
      | deprecatedVocabularyReference | Referenced internal vocabulary '${Ref}' version 1 is deprecated. The vocabulary '${Ref}' is deprecated. | prefix 'ref' -> '${contentBase}/content/${Ref}/versions/1#' |

  @AC_SIMPL-18184_AC2 @D11 @D47 @D5 @D12
  Scenario: A reference without a version resolves to the latest version, and no version is recommended when none other is ACTIVE
    Given a vocabulary "Ref" exists with 2 versions
    And version 2 of "Ref" was set to "DEPRECATED"
    When I create the vocabulary "Health" with a file that declares the prefix "ref" for "${contentBase}/content/${Ref}#"
    Then the response status is 201
    And the validation warnings are
      | code                          | description                                                      | location                                         |
      | deprecatedVocabularyReference | Referenced internal vocabulary '${Ref}' version 2 is deprecated. | prefix 'ref' -> '${contentBase}/content/${Ref}#' |

  @AC_SIMPL-18184_AC2 @D11 @D47 @D5
  Scenario: The recommended version is the latest ACTIVE version, also when it is older than the referenced one
    Given a vocabulary "Ref" exists with 2 versions
    And version 2 of "Ref" was set to "DEPRECATED"
    And version 1 of "Ref" was set to "ACTIVE"
    When I create the vocabulary "Health" with a file that declares the prefix "ref" for "${contentBase}/content/${Ref}/versions/2#"
    Then the response status is 201
    And the validation warnings are
      | code                          | description                                                                                          | location                                                     |
      | deprecatedVocabularyReference | Referenced internal vocabulary '${Ref}' version 2 is deprecated. Use version 1, the latest ACTIVE version. | prefix 'ref' -> '${contentBase}/content/${Ref}/versions/2#' |

  @AC_SIMPL-18184_AC2 @D11
  Scenario: Each deprecated reference gives its own warning
    Given a vocabulary "RefA" exists with 2 versions
    And a vocabulary "RefB" exists with 2 versions
    When I create the vocabulary "Health" with a file that declares these prefixes
      | prefix | namespace                                     |
      | a      | ${contentBase}/content/${RefA}/versions/1#    |
      | b      | ${contentBase}/content/${RefB}/versions/1#    |
    Then the response status is 201
    And the validation warnings are
      | code                          | location                                                     |
      | deprecatedVocabularyReference | prefix 'a' -> '${contentBase}/content/${RefA}/versions/1#'  |
      | deprecatedVocabularyReference | prefix 'b' -> '${contentBase}/content/${RefB}/versions/1#'  |

  @AC_SIMPL-18184_AC3 @D12 @D47 @D8 @negative
  Scenario: A reference to a version that does not exist is error S7 and no version is created
    Given a vocabulary "Ref" exists
    And a vocabulary "Health" exists
    When I create a new version of "Health" with a file that declares the prefix "ref" for "${contentBase}/content/${Ref}/versions/9#"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with the detail "Referenced internal vocabulary '${Ref}' version 9 does not exist." and the location "prefix 'ref' -> '${contentBase}/content/${Ref}/versions/9#'"
    And the vocabulary "Health" has 1 versions
    And version 1 of "Health" has the status "ACTIVE"

  @AC_SIMPL-18184_AC3 @D12 @D47 @D8 @negative
  Scenario: A reference to a vocabulary name that does not exist is error S7
    When I create the vocabulary "Health" with a file that declares the prefix "ghost" for "${contentBase}/content/${Ghost}#"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with the detail "Referenced internal vocabulary '${Ghost}' does not exist." and the location "prefix 'ghost' -> '${contentBase}/content/${Ghost}#'"
    And no vocabulary named "Health" is stored

  @D69 @D47 @negative
  Scenario: A version that is not a positive integer is error S7 with the version as given
    Given a vocabulary "Ref" exists
    When I create the vocabulary "Health" with a file that declares the prefix "ref" for "${contentBase}/content/${Ref}/versions/abc#"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue with the detail "Referenced internal vocabulary '${Ref}' version abc does not exist." and the location "prefix 'ref' -> '${contentBase}/content/${Ref}/versions/abc#'"

  @D69 @D12
  Scenario Outline: A content URL without a valid name is an external reference (<case>)
    When I create the vocabulary "Health" with a file that declares the prefix "odd" for "<namespace>"
    Then the response status is 201
    And the validation warnings are
      | code                     | location                    |
      | externalReferenceSkipped | prefix 'odd' -> '<namespace>' |

    Examples:
      | case                 | namespace                                 |
      | no name              | ${contentBase}/content/                   |
      | lower-case name      | ${contentBase}/content/lower${run}#       |

  @D12
  Scenario: A reference to the vocabulary being created is ignored
    When I create the vocabulary "Health" with a file that declares the prefix "self" for "${contentBase}/content/${Health}/versions/5#"
    Then the response status is 201
    And there are no validation warnings

  @D12
  Scenario: A reference to the vocabulary being versioned is ignored
    Given a vocabulary "Health" exists
    And version 1 of "Health" was set to "DEPRECATED"
    When I create a new version of "Health" with a file that declares the prefix "self" for "${contentBase}/content/${Health}/versions/1#"
    Then the response status is 201
    And there are no validation warnings

  @AC_SIMPL-18183_AC1 @D12 @D10
  Scenario: An unregistered external namespace gives the warning externalReferenceSkipped and the version is created
    When I create the vocabulary "Health" with a file that declares the prefix "dc" for "${extns:Dc}/"
    Then the response status is 201
    And the validation warnings are
      | code                     | location                              |
      | externalReferenceSkipped | prefix 'dc' -> '${extns:Dc}/'         |
    And every validation warning code is in the closed set

  @D12 @D30
  Scenario Outline: A namespace that matches a registered external vocabulary gives no warning (<case>)
    Given an external vocabulary "Dc" is registered with namespace "${extns:Dc}" and alias "${extns:DcAlias}"
    When I create the vocabulary "Health" with a file that declares the prefix "dc" for "<namespace>"
    Then the response status is 201
    And there are no validation warnings

    Examples:
      | case                         | namespace            |
      | namespace with trailing hash | ${extns:Dc}#         |
      | namespace with trailing slash| ${extns:Dc}/         |
      | alias                        | ${extns:DcAlias}/    |

  @D12 @D42
  Scenario: Registering the external vocabulary later clears the warning on the next version
    When I create the vocabulary "Health" with a file that declares the prefix "dc" for "${extns:Dc}/"
    Then the response status is 201
    And the validation warnings are
      | code                     | location                      |
      | externalReferenceSkipped | prefix 'dc' -> '${extns:Dc}/' |
    Given an external vocabulary "Dc" is registered with namespace "${extns:Dc}/"
    When I create a new version of "Health" with a file that declares the prefix "dc" for "${extns:Dc}/"
    Then the response status is 201
    And there are no validation warnings

  @D42 @D41
  Scenario: Removing a referenced external vocabulary is allowed and later uploads warn again
    Given an external vocabulary "Dc" is registered with namespace "${extns:Dc}/"
    And a vocabulary "Health" exists
    When I create a new version of "Health" with a file that declares the prefix "dc" for "${extns:Dc}/"
    Then the response status is 201
    And there are no validation warnings
    Given the external vocabulary "Dc" was removed
    Then version 2 of "Health" has the status "ACTIVE"
    When I send a "GET" request to "/content/${Health}/versions/2"
    Then the response body is the uploaded content of version 2 of "Health"
    When I create a new version of "Health" with a file that declares the prefix "dc" for "${extns:Dc}/"
    Then the response status is 201
    And the validation warnings are
      | code                     | location                      |
      | externalReferenceSkipped | prefix 'dc' -> '${extns:Dc}/' |


  @D10
  Scenario: Validation warnings are returned only by the creation response
    When I create the vocabulary "Health" with a file that declares the prefix "dc" for "${extns:Dc}/"
    Then the response status is 201
    And the validation warnings are
      | code                     | location                      |
      | externalReferenceSkipped | prefix 'dc' -> '${extns:Dc}/' |
    When I send a "GET" request to "/vocabularies/${Health}/versions/1"
    Then the response status is 200
    And the response field "validationWarnings" is absent or null
    When I send a "GET" request to "/vocabularies/${Health}/versions"
    Then the response field "items[0].validationWarnings" is absent or null
