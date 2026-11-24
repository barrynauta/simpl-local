Feature: Search vocabularies and external vocabularies
  POST /vocabularies/searches and POST /external-vocabulary/searches are read-only, not paged,
  limited to 100 references (D38, D68, D137, D153, D182, D184, D205). Both roles may search (D16).

  Background:
    Given I am authenticated as "ga-admin"

  @D38 @D153 @D184 @D25 @D26
  Scenario: Internal search returns the latest and the explicit versions in request order
    Given vocabulary "$Beta" has 2 versions
    And vocabulary "$Alpha" has 3 versions
    When I send a POST request to "/vocabularies/searches" with JSON:
      """
      {"items": [{"name": "$Alpha"}, {"name": "$Beta", "version": 1}]}
      """
    Then the response status is 200
    And the response media type is "application/json"
    And the response items are named "$Alpha, $Beta"
    And the response field "items[0].latest" equals true
    And the response field "items[0].version" equals 3
    And the response field "items[0].status" equals "ACTIVE"
    And the response field "items[0].href" equals "/vocabularies/$Alpha/versions/3"
    And the response field "items[1].latest" equals false
    And the response field "items[1].version" equals 1
    And the response field "items[1].status" equals "DEPRECATED"
    And the response field "items[1].href" equals "/vocabularies/$Beta/versions/1"
    And the response field "items[1].contentHref" equals "$CB/content/$Beta/versions/1"
    And the response field "page" equals 1
    And the response field "pageSize" equals 100
    And the response field "total" equals 2
    And the response field "totalPages" equals 1
    And the link "self" is absent
    And the link "first" is absent
    And the link "next" is absent
    And the link "prev" is absent
    And the link "last" is absent

  @D38 @D3 @D25
  Scenario: A latest result carries the vocabulary status
    Given a vocabulary "$Alpha" exists
    And vocabulary "$Alpha" has vocabulary status "DEPRECATED"
    When I send a POST request to "/vocabularies/searches" with JSON:
      """
      {"items": [{"name": "$Alpha"}, {"name": "$Alpha", "version": 1}]}
      """
    Then the response status is 200
    And the response field "items[0].latest" equals true
    And the response field "items[0].status" equals "DEPRECATED"
    And the response field "items[1].latest" equals false
    And the response field "items[1].status" equals "ACTIVE"

  @D38
  Scenario: Missing names and versions are left out
    Given a vocabulary "$Alpha" exists
    When I send a POST request to "/vocabularies/searches" with JSON:
      """
      {"items": [{"name": "$Missing"}, {"name": "$Alpha", "version": 4}, {"name": "$Alpha"}]}
      """
    Then the response status is 200
    And the response items are named "$Alpha"
    And the response field "total" equals 1

  @D38 @D153
  Scenario: A search that finds nothing returns an empty result
    When I send a POST request to "/vocabularies/searches" with JSON:
      """
      {"items": [{"name": "$Missing"}]}
      """
    Then the response status is 200
    And the response has 0 items
    And the response field "total" equals 0
    And the response field "totalPages" equals 0

  @D38
  Scenario: A repeated reference gives one result at its first position; name and name with version are different
    Given vocabulary "$Alpha" has 2 versions
    And a vocabulary "$Beta" exists
    When I send a POST request to "/vocabularies/searches" with JSON:
      """
      {"items": [{"name": "$Alpha"}, {"name": "$Beta"}, {"name": "$Alpha"}, {"name": "$Alpha", "version": 2}, {"name": "$Alpha", "version": 2}]}
      """
    Then the response status is 200
    And the response items are named "$Alpha, $Beta, $Alpha"
    And the response field "items[0].latest" equals true
    And the response field "items[2].latest" equals false
    And the response field "items[2].version" equals 2

  @D54 @D38
  Scenario: Search by name is exact
    Given a vocabulary "$Alpha" exists
    When I send a POST request to "/vocabularies/searches" with JSON:
      """
      {"items": [{"name": "$Alpha!upper"}]}
      """
    Then the response status is 200
    And the response has 0 items

  @D38 @Q2 @NFR-04
  Scenario: 100 references are accepted
    When I send a POST request to "/vocabularies/searches" with JSON:
      """
      {"items": [{"name": "Aaa"},{"name": "Aab"},{"name": "Aac"},{"name": "Aad"},{"name": "Aae"},{"name": "Aaf"},{"name": "Aag"},{"name": "Aah"},{"name": "Aai"},{"name": "Aaj"},{"name": "Aba"},{"name": "Abb"},{"name": "Abc"},{"name": "Abd"},{"name": "Abe"},{"name": "Abf"},{"name": "Abg"},{"name": "Abh"},{"name": "Abi"},{"name": "Abj"},{"name": "Aca"},{"name": "Acb"},{"name": "Acc"},{"name": "Acd"},{"name": "Ace"},{"name": "Acf"},{"name": "Acg"},{"name": "Ach"},{"name": "Aci"},{"name": "Acj"},{"name": "Ada"},{"name": "Adb"},{"name": "Adc"},{"name": "Add"},{"name": "Ade"},{"name": "Adf"},{"name": "Adg"},{"name": "Adh"},{"name": "Adi"},{"name": "Adj"},{"name": "Aea"},{"name": "Aeb"},{"name": "Aec"},{"name": "Aed"},{"name": "Aee"},{"name": "Aef"},{"name": "Aeg"},{"name": "Aeh"},{"name": "Aei"},{"name": "Aej"},{"name": "Afa"},{"name": "Afb"},{"name": "Afc"},{"name": "Afd"},{"name": "Afe"},{"name": "Aff"},{"name": "Afg"},{"name": "Afh"},{"name": "Afi"},{"name": "Afj"},{"name": "Aga"},{"name": "Agb"},{"name": "Agc"},{"name": "Agd"},{"name": "Age"},{"name": "Agf"},{"name": "Agg"},{"name": "Agh"},{"name": "Agi"},{"name": "Agj"},{"name": "Aha"},{"name": "Ahb"},{"name": "Ahc"},{"name": "Ahd"},{"name": "Ahe"},{"name": "Ahf"},{"name": "Ahg"},{"name": "Ahh"},{"name": "Ahi"},{"name": "Ahj"},{"name": "Aia"},{"name": "Aib"},{"name": "Aic"},{"name": "Aid"},{"name": "Aie"},{"name": "Aif"},{"name": "Aig"},{"name": "Aih"},{"name": "Aii"},{"name": "Aij"},{"name": "Aja"},{"name": "Ajb"},{"name": "Ajc"},{"name": "Ajd"},{"name": "Aje"},{"name": "Ajf"},{"name": "Ajg"},{"name": "Ajh"},{"name": "Aji"},{"name": "$Alpha"}]}
      """
    Then the response status is 200

  @D38 @D68 @negative @Q2 @NFR-04
  Scenario: 101 references are rejected
    When I send a POST request to "/vocabularies/searches" with JSON:
      """
      {"items": [{"name": "Aaa"},{"name": "Aab"},{"name": "Aac"},{"name": "Aad"},{"name": "Aae"},{"name": "Aaf"},{"name": "Aag"},{"name": "Aah"},{"name": "Aai"},{"name": "Aaj"},{"name": "Aba"},{"name": "Abb"},{"name": "Abc"},{"name": "Abd"},{"name": "Abe"},{"name": "Abf"},{"name": "Abg"},{"name": "Abh"},{"name": "Abi"},{"name": "Abj"},{"name": "Aca"},{"name": "Acb"},{"name": "Acc"},{"name": "Acd"},{"name": "Ace"},{"name": "Acf"},{"name": "Acg"},{"name": "Ach"},{"name": "Aci"},{"name": "Acj"},{"name": "Ada"},{"name": "Adb"},{"name": "Adc"},{"name": "Add"},{"name": "Ade"},{"name": "Adf"},{"name": "Adg"},{"name": "Adh"},{"name": "Adi"},{"name": "Adj"},{"name": "Aea"},{"name": "Aeb"},{"name": "Aec"},{"name": "Aed"},{"name": "Aee"},{"name": "Aef"},{"name": "Aeg"},{"name": "Aeh"},{"name": "Aei"},{"name": "Aej"},{"name": "Afa"},{"name": "Afb"},{"name": "Afc"},{"name": "Afd"},{"name": "Afe"},{"name": "Aff"},{"name": "Afg"},{"name": "Afh"},{"name": "Afi"},{"name": "Afj"},{"name": "Aga"},{"name": "Agb"},{"name": "Agc"},{"name": "Agd"},{"name": "Age"},{"name": "Agf"},{"name": "Agg"},{"name": "Agh"},{"name": "Agi"},{"name": "Agj"},{"name": "Aha"},{"name": "Ahb"},{"name": "Ahc"},{"name": "Ahd"},{"name": "Ahe"},{"name": "Ahf"},{"name": "Ahg"},{"name": "Ahh"},{"name": "Ahi"},{"name": "Ahj"},{"name": "Aia"},{"name": "Aib"},{"name": "Aic"},{"name": "Aid"},{"name": "Aie"},{"name": "Aif"},{"name": "Aig"},{"name": "Aih"},{"name": "Aii"},{"name": "Aij"},{"name": "Aja"},{"name": "Ajb"},{"name": "Ajc"},{"name": "Ajd"},{"name": "Aje"},{"name": "Ajf"},{"name": "Ajg"},{"name": "Ajh"},{"name": "Aji"},{"name": "Ajj"},{"name": "$Alpha"}]}
      """
    Then the response is a problem with status 400
    And the problem type is "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And issue 1 is in "body" named "items"
    And the response holds no items

  @D205 @D38 @negative
  Scenario: An empty items array is rejected
    When I send a POST request to "/vocabularies/searches" with JSON:
      """
      {"items": []}
      """
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "body" named "items"

  @D68 @D182 @D91 @D112 @D52 @negative
  Scenario Outline: An invalid search item names the item: <body>
    When I send a POST request to "/vocabularies/searches" with JSON:
      """
      <body>
      """
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "<name>"

    Examples:
      | body                                                      | name             |
      | {"items": [{"name": "lower"}]}                            | items[0].name    |
      | {"items": [{"name": "Alpha"}, {"name": "Ab"}]}            | items[1].name    |
      | {"items": [{"name": "Alpha", "version": 0}]}              | items[0].version |
      | {"items": [{"name": "Alpha", "version": -2}]}             | items[0].version |
      | {"items": [{"name": "Alpha", "version": 1.5}]}            | items[0].version |
      | {"items": [{"name": "Alpha", "version": 1e1}]}            | items[0].version |
      | {"items": [{"name": "Alpha", "version": "1"}]}            | items[0].version |
      | {"items": [{"name": "Alpha", "version": 2147483648}]}     | items[0].version |
      | {"items": [{"version": 1}]}                               | items[0].name    |

  @D52 @D68 @negative
  Scenario Outline: A search body of the wrong shape is rejected: <body>
    When I send a POST request to "/vocabularies/searches" with JSON:
      """
      <body>
      """
    Then the response is a problem with status 400
    And the problem type is "urn:problem-type:belgif:badRequest"

    Examples:
      | body                                              |
      | {}                                                |
      | {"items": [{"name": "Alpha"}], "limit": 5}        |
      | {"items": [{"name": "Alpha", "latest": true}]}    |
      | {"items": "Alpha"}                                |
      | [{"name": "Alpha"}]                               |
      | not json                                          |

  @D38 @D52 @negative
  Scenario: A query parameter on a search is rejected
    When I send a POST request to "/vocabularies/searches?page=1" with JSON:
      """
      {"items": [{"name": "Alpha"}]}
      """
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "query" named "page"

  @D15 @API-38 @negative
  Scenario: A search that is not application/json gets 415
    When I send a POST request to "/vocabularies/searches" with the body "name=Alpha" as "application/x-www-form-urlencoded"
    Then the response is a problem with status 415
    And the problem type is "urn:problem-type:simpl:unsupportedMediaType"

  @D63 @API-37 @negative
  Scenario: A search with an Accept header it cannot serve gets 406
    Given the request header "Accept" is "text/turtle"
    When I send a POST request to "/vocabularies/searches" with JSON:
      """
      {"items": [{"name": "Alpha"}]}
      """
    Then the response is a problem with status 406

  @D150 @negative @Q2 @NFR-04
  Scenario: A search body over 1 MB gets 413
    When I send a POST request to "/vocabularies/searches" with a JSON body of 1048577 bytes
    Then the response is a problem with status 413
    And the problem type is "urn:problem-type:simpl:payloadTooLarge"
    And the problem detail is "The request body is larger than 1 MB."

  @D38 @D30 @D41 @D73
  Scenario Outline: External search by namespace or alias, normalised: <body>
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/ns" and aliases "$EXT/alias/"
    When I send a POST request to "/external-vocabulary/searches" with JSON:
      """
      <body>
      """
    Then the response status is 200
    And the response items are named "$Ext"
    And the response field "items[0].namespaceUri" equals "$EXT/ns"
    And the response field "items[0].aliasUris" equals ["$EXT/alias/"]
    And the response field "items[0].href" equals "/external-vocabularies/$Ext"
    And the response field "total" equals 1
    And the response field "totalPages" equals 1
    And the response field "pageSize" equals 100
    And the link "self" is absent

    Examples:
      | body                                  |
      | {"namespaceUri": "$EXT/ns"}           |
      | {"namespaceUri": "$EXT/ns#"}          |
      | {"namespaceUri": "$EXT/ns/"}          |
      | {"aliasUri": "$EXT/alias"}            |
      | {"aliasUri": "$EXT/alias#"}           |
      | {"namespaceUris": ["$EXT/ns"]}        |
      | {"aliasUris": ["$EXT/alias/"]}        |

  @D38
  Scenario: External batch search keeps the order of the first match and lists each vocabulary once
    Given an external vocabulary "$ExtA" is registered with namespace "$EXT/a" and aliases "$EXT/a2"
    And an external vocabulary "$ExtB" is registered with namespace "$EXT/b"
    When I send a POST request to "/external-vocabulary/searches" with JSON:
      """
      {"namespaceUris": ["$EXT/b", "$EXT/nothing", "$EXT/a"], "aliasUris": ["$EXT/a2", "$EXT/b"]}
      """
    Then the response status is 200
    And the response items are named "$ExtB, $ExtA"
    And the response field "total" equals 2

  @D38
  Scenario: Namespace URIs come before alias URIs in the result order
    Given an external vocabulary "$ExtA" is registered with namespace "$EXT/a" and aliases "$EXT/a2"
    And an external vocabulary "$ExtB" is registered with namespace "$EXT/b"
    When I send a POST request to "/external-vocabulary/searches" with JSON:
      """
      {"aliasUris": ["$EXT/a2"], "namespaceUris": ["$EXT/b"]}
      """
    Then the response items are named "$ExtB, $ExtA"

  @D38 @D153
  Scenario: An external search without a match returns an empty result
    When I send a POST request to "/external-vocabulary/searches" with JSON:
      """
      {"namespaceUri": "$EXT/none"}
      """
    Then the response status is 200
    And the response has 0 items
    And the response field "total" equals 0
    And the response field "totalPages" equals 0

  @D38 @D68 @negative
  Scenario Outline: An external search body that fits no or several oneOf branches is rejected: <body>
    When I send a POST request to "/external-vocabulary/searches" with JSON:
      """
      <body>
      """
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "body" named "body"

    Examples:
      | body                                                                  |
      | {}                                                                    |
      | {"namespaceUri": "$EXT/a", "aliasUri": "$EXT/b"}                      |
      | {"namespaceUri": "$EXT/a", "namespaceUris": ["$EXT/b"]}               |
      | {"aliasUri": "$EXT/a", "aliasUris": ["$EXT/b"]}                       |

  @D137 @D38 @negative
  Scenario Outline: An empty URI array is rejected and named after the array: <array>
    When I send a POST request to "/external-vocabulary/searches" with JSON:
      """
      {"<array>": []}
      """
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "body" named "<array>"

    Examples:
      | array         |
      | namespaceUris |
      | aliasUris     |

  @D73 @D182 @negative
  Scenario Outline: A URI that is not absolute is rejected: <body>
    When I send a POST request to "/external-vocabulary/searches" with JSON:
      """
      <body>
      """
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "<name>"

    Examples:
      | body                                               | name             |
      | {"namespaceUri": "purl.org/dc/terms"}              | namespaceUri     |
      | {"aliasUri": "/relative/path"}                     | aliasUri         |
      | {"namespaceUris": ["$EXT/ok", "not a uri"]}        | namespaceUris[1] |
      | {"aliasUris": ["relative#"]}                       | aliasUris[0]     |

  @D52 @negative
  Scenario: An unknown property in an external search is rejected
    When I send a POST request to "/external-vocabulary/searches" with JSON:
      """
      {"namespaceUri": "$EXT/a", "fuzzy": true}
      """
    Then the response is a problem with status 400

  @D38 @D52 @negative
  Scenario: A query parameter on the external search is rejected
    When I send a POST request to "/external-vocabulary/searches?pageSize=10" with JSON:
      """
      {"namespaceUri": "$EXT/a"}
      """
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "query" named "pageSize"

  @D38 @Q2 @NFR-04
  Scenario: An external search with 100 URIs in total is accepted
    When I search external vocabularies with 60 namespace URIs and 40 alias URIs
    Then the response status is 200

  @D38 @D68 @negative @Q2 @NFR-04
  Scenario Outline: An external search with more than 100 URIs is rejected: <ns> and <al>
    When I search external vocabularies with <ns> namespace URIs and <al> alias URIs
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "body" named "<name>"

    Examples:
      | ns  | al | name          |
      | 61  | 40 | aliasUris     |
      | 101 | 0  | namespaceUris |
      | 0   | 101 | aliasUris    |
