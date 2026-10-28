@webhook @D60
Feature: Webhook delivery of SchemaPublished and SchemaRevoked
  SIMPL-19992 names the two event types; triggers, payload and delivery are
  fixed by D42 to D44. The receiver runs inside the suite; the service reaches
  it at WEBHOOK_RECEIVER_URL. Waits: 10 s for a delivery, 3 s for an absence.

  @SIMPL-19992 @D42 @D43 @D44
  Scenario: Creating a published RESOURCE schema delivers SchemaPublished
    Given the webhook "hook" is subscribed to "SchemaPublished,SchemaRevoked"
    And I am "ga-admin"
    When I create the RESOURCE schema "$A" for resource type "$RT"
    Then the response status is 201
    And the webhook "hook" receives a "SchemaPublished" event for schema "$A"
    And the event was posted as JSON with the header "X-Simpl-Event" set to "SchemaPublished"
    And the event field "eventId" is a UUID
    And the event field "eventType" is "SchemaPublished"
    And the event field "occurredAt" is an ISO timestamp
    And the event field "schemaName" is "$A"
    And the event field "schemaType" is "RESOURCE"
    And the event field "resourceType" is "$RT"
    And the event field "version" is "1"
    And the event field "status" is "PUBLISHED"
    And the event field "schemaUri" is an absolute URI containing "/schemas/$A/1"

  @SIMPL-19992 @D42
  Scenario: A schema revoked on create delivers nothing
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And the webhook "hook" is subscribed to "SchemaPublished,SchemaRevoked"
    When I create the RESOURCE schema "$B" for resource type "$RT"
    Then the JSON field "status" is "REVOKED"
    And the webhook "hook" receives no event for schema "$B"

  @SIMPL-19992 @D42 @D43
  Scenario: A new published version delivers SchemaPublished and no SchemaRevoked for the superseded one
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And the webhook "hook" is subscribed to "SchemaPublished,SchemaRevoked"
    When I add a new version to schema "$A"
    Then the response status is 201
    And the webhook "hook" receives a "SchemaPublished" event for schema "$A"
    And the event field "version" is "2"
    And the event field "schemaUri" is an absolute URI containing "/schemas/$A/2"
    And the webhook "hook" receives no "SchemaRevoked" event for schema "$A"

  @SIMPL-19992 @D42
  Scenario: A new version that stays revoked delivers nothing
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And the RESOURCE schema "$B" exists for resource type "$RT"
    And the webhook "hook" is subscribed to "SchemaPublished,SchemaRevoked"
    When I add a new version to schema "$B"
    Then the JSON field "status" is "REVOKED"
    And the webhook "hook" receives no event for schema "$B"

  @SIMPL-19992 @D42 @D43
  Scenario: Revoking delivers SchemaRevoked
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And the webhook "hook" is subscribed to "SchemaPublished,SchemaRevoked"
    When I revoke schema "$A"
    Then the response status is 200
    And the webhook "hook" receives a "SchemaRevoked" event for schema "$A"
    And the event was posted as JSON with the header "X-Simpl-Event" set to "SchemaRevoked"
    And the event field "status" is "REVOKED"
    And the event field "version" is "1"

  @SIMPL-19992 @SIMPL-20529 @D42
  Scenario: Publishing a revoked schema delivers SchemaPublished
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has been revoked
    And the webhook "hook" is subscribed to "SchemaPublished"
    When I publish schema "$A"
    Then the response status is 200
    And the webhook "hook" receives a "SchemaPublished" event for schema "$A"
    And the event field "status" is "PUBLISHED"

  @SIMPL-19992 @D44
  Scenario: Only subscribed event types are delivered
    Given the webhook "hook" is subscribed to "SchemaRevoked"
    And I am "ga-admin"
    When I create the RESOURCE schema "$A" for resource type "$RT"
    Then the response status is 201
    And the webhook "hook" receives no "SchemaPublished" event for schema "$A"
    When I revoke schema "$A"
    Then the response status is 200
    And the webhook "hook" receives a "SchemaRevoked" event for schema "$A"

  @SIMPL-19992 @D44
  Scenario: Every matching subscription gets the event once
    Given the webhook "hook" is subscribed to "SchemaPublished"
    And the webhook "hook2" is subscribed to "SchemaPublished,SchemaRevoked"
    When I create the RESOURCE schema "$A" for resource type "$RT"
    Then the response status is 201
    And the webhook "hook" receives exactly 1 "SchemaPublished" event for schema "$A"
    And the webhook "hook2" receives exactly 1 "SchemaPublished" event for schema "$A"

  @SIMPL-19992 @D42 @D44
  Scenario: Failed requests deliver nothing
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And the webhook "hook" is subscribed to "SchemaPublished,SchemaRevoked"
    When I publish schema "$A"
    Then the response status is 409
    When I add a new version to schema "$A" from "syntax-error.ttl"
    Then the response status is 400
    And the webhook "hook" receives no event for schema "$A"

  @SIMPL-19992 @D41 @D44
  Scenario: A deleted subscription receives nothing
    Given the webhook "hook" is subscribed to "SchemaPublished"
    When I delete the webhook "hook"
    Then the response status is 204
    When I create the RESOURCE schema "$A" for resource type "$RT"
    Then the response status is 201
    And the webhook "hook" receives no event for schema "$A"

  @SIMPL-19992 @D44
  Scenario: A delivery answered with an error is retried with the same event
    Given the webhook "hook" is subscribed to "SchemaPublished"
    And the webhook "hook" fails the first 1 deliveries
    When I create the RESOURCE schema "$A" for resource type "$RT"
    Then the response status is 201
    And the webhook "hook" receives the same "SchemaPublished" event for schema "$A" 2 times

  @SIMPL-19992 @D44
  Scenario: A delivery that keeps failing is tried three times in total
    Given the webhook "hook" is subscribed to "SchemaPublished"
    And the webhook "hook" fails the first 5 deliveries
    When I create the RESOURCE schema "$A" for resource type "$RT"
    Then the response status is 201
    And the webhook "hook" receives exactly 3 delivery attempts for schema "$A"

  @SIMPL-19992 @SIMPL-33694 @D42 @D43 @search-serial
  Scenario: SEARCH schema changes deliver webhooks too
    Given no SEARCH schema is published
    And the webhook "hook" is subscribed to "SchemaPublished,SchemaRevoked"
    And I am "ga-admin"
    When I create the SEARCH schema "$S"
    Then the response status is 201
    And the webhook "hook" receives a "SchemaPublished" event for schema "$S"
    And the event field "schemaType" is "SEARCH"
    And the event field "resourceType" is null
    And the event field "schemaUri" is an absolute URI containing "/schemas/$S/1"
    When I revoke schema "$S"
    Then the response status is 200
    And the webhook "hook" receives a "SchemaRevoked" event for schema "$S"
    And the event field "schemaType" is "SEARCH"
