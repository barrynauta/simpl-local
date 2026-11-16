@api @cat @query
Feature: catalogue-be openCypher query, latest schemas, verification page and error members
  Added in run 1.2 (4 Nov 2026), when the PSO tests became a source (digest section 1.1).
  D10 as revised: GET and POST /query, GET /schemas/latest and GET /verification are in scope.
  D84: POST /query runs one read-only openCypher statement. D85 (to validate): the claims of every
  active RD are in the graph, loaded on publish and removed on revoke, on delete and when a new
  version replaces them. D86: latest schemas. D87: the verification page. D88: POST /query/search.
  D89: every problem also carries "message"; verification failures also carry "code".

  # ------------------------------------------------------------------ query (D84, D85)

  @D84 @D85
  Scenario: A published resource description can be found with openCypher
    Given "provider-a" has published a "DataOffering" resource description "q1"
    When "consumer-a" sends POST "/query" to "cat-a"
      """
      {"statement": "MATCH (n:DataOffering {uri: $id}) RETURN n.uri AS uri, n.version AS version",
       "parameters": {"id": "{q1.id}"}}
      """
    Then the status is 200
    And the field "totalCount" is "1"
    And the field "items/0/uri" is the id of "q1"
    And the field "items/0/version" is "1"

  @D85
  Scenario: Nested claims are linked nodes named after the predicate
    Given "provider-a" has published a "DataOffering" resource description "q1"
    When "consumer-a" sends POST "/query" to "cat-a"
      """
      {"statement": "MATCH (n {uri: $id})-[:generalServiceProperties]->(g:GeneralServiceProperties) RETURN g.name AS name, g.claimsGraphUri AS graph",
       "parameters": {"id": "{q1.id}"}}
      """
    Then the status is 200
    And the field "totalCount" is "1"
    And the field "items/0/name" contains the text "{token}"
    And the field "items/0/graph" is a list containing "{q1.id}"

  @D85 @D16
  Scenario: A revoked resource description leaves the graph
    Given "provider-a" has published a "DataOffering" resource description "q1"
    And "provider-a" has revoked "q1"
    When "consumer-a" sends POST "/query" to "cat-a"
      """
      {"statement": "MATCH (n) WHERE $id IN n.claimsGraphUri RETURN n", "parameters": {"id": "{q1.id}"}}
      """
    Then the status is 200
    And the field "totalCount" is "0"

  @D85 @D17
  Scenario: A deleted resource description leaves the graph
    Given "provider-a" has published a "DataOffering" resource description "q1"
    And "provider-a" has deleted "q1"
    When "consumer-a" sends POST "/query" to "cat-a"
      """
      {"statement": "MATCH (n) WHERE $id IN n.claimsGraphUri RETURN n", "parameters": {"id": "{q1.id}"}}
      """
    Then the status is 200
    And the field "totalCount" is "0"

  @D85 @D13
  Scenario: A new version replaces the previous version in the graph
    Given "provider-a" has published a "DataOffering" resource description "q1"
    And "provider-a" has published "q2" as version 2 of "q1"
    When "consumer-a" sends POST "/query" to "cat-a"
      """
      {"statement": "MATCH (n:DataOffering) WHERE n.uri IN [$v1, $v2] RETURN n.uri AS uri, n.version AS version",
       "parameters": {"v1": "{q1.id}", "v2": "{q2.id}"}}
      """
    Then the status is 200
    And the field "totalCount" is "1"
    And the field "items/0/uri" is the id of "q2"
    And the field "items/0/version" is "2"

  @D84
  Scenario: withTotalCount false leaves out the count
    When "consumer-a" sends POST "/query?withTotalCount=false" to "cat-a"
      """
      {"statement": "RETURN 1 AS one"}
      """
    Then the status is 200
    And the field "totalCount" does not exist
    And the field "items/0/one" is "1"

  @D84
  Scenario Outline: An invalid query is a bad request (<case>)
    When "consumer-a" sends POST "<path>" to "cat-a"
      """
      <body>
      """
    Then the response is a problem with status 400

    Examples:
      | case                 | path                        | body                                                   |
      | not openCypher       | /query                      | {"statement": "string", "parameters": {}}              |
      | no statement         | /query                      | {"parameters": {}}                                     |
      | parameters not a map | /query                      | {"statement": "RETURN 1", "parameters": ""}            |
      | writes to the graph  | /query                      | {"statement": "CREATE (n:Resource {uri: 'x'}) RETURN n"} |
      | other language       | /query?queryLanguage=SPARQL | {"statement": "SELECT * WHERE { ?s ?p ?o }"}           |
      | unknown language     | /query?queryLanguage=XPATH  | {"statement": "RETURN 1"}                              |

  @D84 @rbac
  Scenario: The query needs a token
    When "anonymous" sends POST "/query" to "cat-a"
      """
      {"statement": "RETURN 1"}
      """
    Then the response is a problem "unauthorized" with status 401

  @D84 @rbac
  Scenario: The query needs a searcher role
    When "schema-viewer-a" sends POST "/query" to "cat-a"
      """
      {"statement": "RETURN 1"}
      """
    Then the response is a problem "forbidden" with status 403

  @D84
  Scenario: The query page is HTML
    When "consumer-a" sends GET "/query" to "cat-a"
    Then the status is 200
    And the response content type is "text/html"

  # ------------------------------------------------------------------ query search (D88)

  @D88 @pso-source
  Scenario: Query search with an empty body is a bad request
    When "consumer-a" sends POST "/query/search" to "cat-a"
      """
      {}
      """
    Then the response is a problem with status 400

  @D88
  Scenario: Query search with a statement answers like the query
    When "consumer-a" sends POST "/query/search" to "cat-a"
      """
      {"statement": "RETURN 1 AS one"}
      """
    Then the status is 200
    And the field "items/0/one" is "1"

  # ------------------------------------------------------------------ latest schemas (D86)

  @D86
  Scenario: The latest shapes are the composite of the stored shapes
    When "schema-viewer-a" sends GET "/schemas/latest?type=shape" to "cat-a"
    Then the status is 200
    And the response content type is "text/turtle"
    And the response body contains "DataOfferingShape"
    And the response body contains "ApplicationOfferingShape"

  @D86
  Scenario: A term selects the shapes with that target class
    When "schema-viewer-a" sends GET "/schemas/latest?type=shape&term=http%3A%2F%2Fw3id.org%2Fgaia-x%2Fsimpl%23DataOffering" to "cat-a"
    Then the status is 200
    And the response body contains "DataOfferingShape"
    And the response body does not contain "ApplicationOfferingShape"

  @D86 @pso-source
  Scenario: Ontologies are not stored, so the latest ontology is an empty RDF/XML graph
    When "schema-admin-a" sends GET "/schemas/latest?type=ontology" to "cat-a"
    Then the status is 200
    And the response content type is "application/rdf+xml"

  @D86
  Scenario Outline: The type of the latest schema is required and closed (<path>)
    When "schema-viewer-a" sends GET "<path>" to "cat-a"
    Then the response is a problem with status 400

    Examples:
      | path                       |
      | /schemas/latest            |
      | /schemas/latest?type=SHAPE |

  @D86 @rbac
  Scenario: The latest schemas need a schema role
    When "consumer-a" sends GET "/schemas/latest?type=shape" to "cat-a"
    Then the response is a problem "forbidden" with status 403

  # ------------------------------------------------------------------ verification page (D87)

  @D87 @D67 @pso-source
  Scenario: The verification page is HTML for any user of the realm
    When "nobody-a" sends GET "/verification" to "cat-a"
    Then the status is 200
    And the response content type is "text/html"

  @D87 @rbac
  Scenario: The verification page needs a token
    When "anonymous" sends GET "/verification" to "cat-a"
    Then the response is a problem "unauthorized" with status 401

  # ------------------------------------------------------------------ error members (D89)

  @D89 @D2 @pso-source
  Scenario: A problem also carries message and code
    When "schema-viewer-a" sends GET "/schemas/NoSuch{token}Shape" to "cat-a"
    Then the response is a problem "resourceNotFound" with status 404
    And the field "message" equals the field "detail"
    And the field "message" contains the text "was not found"
    And the field "code" does not exist

  @D89 @D17 @pso-source
  Scenario: Deleting an unknown hash names the hash in the message
    When "provider-a" sends DELETE "/self-descriptions/{token}nohash" to "cat-a"
    Then the response is a problem "resourceNotFound" with status 404
    And the field "message" contains the text "no self-description found for hash"

  @D89 @D14 @pso-source
  Scenario: A verification failure at publication has the code verification_error
    Given a "DataOffering" resource description "bad" by "provider-a"
    And "bad" is broken as "missing-name"
    When "provider-a" publishes "bad"
    Then the response is a problem "catalogue:shaclViolation" with status 422
    And the field "code" is "verification_error"
    And the field "message" equals the field "detail"
