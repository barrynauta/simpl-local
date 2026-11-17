Feature: Webhook subscriptions
  SIMPL-19992 with the contract of D41. Any authenticated caller may manage
  subscriptions (D7). Each scenario subscribes with its own receiver path and
  the suite deletes the subscriptions afterwards (D60, D61).

  @SIMPL-19992 @D41
  Scenario: Subscribe to both events
    Given I am "ga-admin"
    When I subscribe the webhook "hook" to "SchemaPublished,SchemaRevoked"
    Then the response status is 201
    And the response content type is "application/json"
    And the response describes the webhook "hook" with the events "SchemaPublished,SchemaRevoked"

  @SIMPL-19992 @D41
  Scenario: The list of subscriptions is a plain array that includes mine
    Given the webhook "hook" is subscribed to "SchemaRevoked"
    When I list the webhooks
    Then the response status is 200
    And the webhook list contains "hook" with the events "SchemaRevoked"

  @SIMPL-19992 @D41
  Scenario: Delete a subscription
    Given the webhook "hook" is subscribed to "SchemaPublished"
    When I delete the webhook "hook"
    Then the response status is 204
    When I list the webhooks
    Then the webhook list does not contain "hook"
    When I delete the webhook "hook"
    Then the response is a "notFound" problem with status 404

  @SIMPL-19992 @D41
  Scenario Outline: Deleting an unknown subscription gives 404
    When I send a DELETE request to "/webhooks/<id>"
    Then the response is a "notFound" problem with status 404

    Examples:
      | id                                   |
      | 0b6f2c8e-7b1d-4c55-9a0e-000000000999 |
      | wh_999                               |

  @SIMPL-19992 @D41
  Scenario Outline: A target URL that is not an absolute http or https URL is rejected
    When I create a webhook subscription with JSON body
      """
      {"targetUrl": "<url>", "events": ["SchemaPublished"]}
      """
    Then the response is a "badRequest" problem with status 400

    Examples:
      | url              |
      | url              |
      | not-a-url        |
      | http://          |
      | ftp://invalid.com |
      | /relative/path   |

  @SIMPL-19992 @D41
  Scenario: A missing target URL is rejected
    When I create a webhook subscription with JSON body
      """
      {"events": ["SchemaPublished"]}
      """
    Then the response is a "badRequest" problem with status 400

  @SIMPL-19992 @D41
  Scenario Outline: Missing or empty events are rejected
    When I create a webhook subscription with JSON body
      """
      {"targetUrl": "$RECEIVER"<events>}
      """
    Then the response is a "badRequest" problem with status 400

    Examples:
      | events        |
      |               |
      | , "events": [] |

  @SIMPL-19992 @D41 @openapi-only
  Scenario: An unknown event type is rejected with 406
    When I create a webhook subscription with JSON body
      """
      {"targetUrl": "$RECEIVER", "events": ["SchemaExploded"]}
      """
    Then the response is a "notAcceptable" problem with status 406
    And the problem detail is "'SchemaExploded' is not a valid event type. Valid event types are: SchemaPublished, SchemaRevoked"

  @SIMPL-19992 @D41 @openapi-only
  Scenario: The same target URL with the same events is a conflict
    Given the webhook "hook" is subscribed to "SchemaPublished,SchemaRevoked"
    When I subscribe the webhook "hook" to "SchemaRevoked,SchemaPublished"
    Then the response is a "conflict" problem with status 409

  @SIMPL-19992 @D41
  Scenario: The same target URL with other events is a separate subscription
    Given the webhook "hook" is subscribed to "SchemaPublished"
    And the webhook "hook2" uses the same target URL as "hook"
    When I subscribe the webhook "hook2" to "SchemaRevoked"
    Then the response status is 201
