@api @cat @bp05b @lifecycle
Feature: catalogue-be versions, revokes, deletes and lists resource descriptions
  BP05b steps 08 to 11, 13 and 14. RDs are immutable; a new version revokes the previous one
  (D13). Revoke and delete are owner only (D16, D17). The list has the filters of the composed
  root (D18).

  # ------------------------------------------------------------------ versions (D13, D19)

  @D13
  Scenario: A new version revokes the previous version
    Given "provider-a" has published a "DataOffering" resource description "v1"
    And "v2" is version 2 of "v1"
    When "provider-a" publishes "v2"
    Then the status is 201
    And the field "id" is the id of "v2"
    And the field "currentVersion" is "2"
    And the field "resourceKey" is the id of "v1"
    And "v1" is stored with the status "revoked"
    And "v2" is stored with the status "active"

  @D13
  Scenario: A third version keeps the resource key of the first
    Given "provider-a" has published a "DataOffering" resource description "v1"
    And "provider-a" has published "v2" as version 2 of "v1"
    And "v3" is version 3 of "v2"
    When "provider-a" publishes "v3" through "/self-descriptions/resource"
    Then the status is 201
    And the field "currentVersion" is "3"
    And the field "resourceKey" is the id of "v1"
    And "v2" is stored with the status "revoked"
    And "v1" is stored with the status "revoked"

  @D13
  Scenario: The revoked previous version has a new status time
    Given "provider-a" has published a "DataOffering" resource description "v1"
    And I remember the field "statusDatetime" as "v1_published"
    And more than one second has passed
    And "provider-a" has published "v2" as version 2 of "v1"
    When "provider-a" sends GET "/self-descriptions?ids={v1.enc}&statuses=revoked" to "cat-a"
    Then the status is 200
    And the result "v1" has the field "status" set to "revoked"
    And the result "v1" has the field "statusDatetime" different from "{v1_published}"

  @D13
  Scenario Outline: An invalid version number is a version conflict (<case>)
    Given "provider-a" has published a "DataOffering" resource description "v1"
    And "v2" is version 2 of "v1"
    And "v2" has the field "credentialSubject/simpl:version" set to "<number>"
    When "provider-a" publishes "v2"
    Then the response is a problem "catalogue:versionConflict" with status 409
    And "v1" is stored with the status "active"

    Examples:
      | case                       | number |
      | number skips a version     | 3      |
      | number repeats the version | 1      |

  @D13
  Scenario: A new version that points to an unknown previous version is a version conflict
    Given a "DataOffering" resource description "v2" by "provider-a"
    And "v2" has the field "credentialSubject/simpl:version" set to "2"
    And "v2" points to an unknown previous version
    When "provider-a" publishes "v2"
    Then the response is a problem "catalogue:versionConflict" with status 409

  @D13
  Scenario Outline: A version without the right chain fields is a version conflict (<broken>)
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "<broken>"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:versionConflict" with status 409

    Examples:
      | broken                          |
      | first-version-with-previous     |
      | second-version-without-previous |

  @D13
  Scenario: A new version must point to the currently active version
    Given "provider-a" has published a "DataOffering" resource description "v1"
    And "provider-a" has published "v2" as version 2 of "v1"
    And "v2b" is version 2 of "v1"
    When "provider-a" publishes "v2b"
    Then the response is a problem "catalogue:versionConflict" with status 409

  @rbac @D13
  Scenario: A new version must point to a resource of the same participant
    Given "provider-a" has published a "DataOffering" resource description "v1"
    And a "DataOffering" resource description "other" by "provider-a2"
    And "other" has the field "credentialSubject/simpl:version" set to "2"
    And "other" points to the previous version "v1"
    When "provider-a2" publishes "other"
    Then the response is a problem "catalogue:versionConflict" with status 409
    And "v1" is stored with the status "active"

  @D19
  Scenario: The version history lists all other versions, newest first
    Given "provider-a" has published a "DataOffering" resource description "v1"
    And "provider-a" has published "v2" as version 2 of "v1"
    And "provider-a" has published "v3" as version 3 of "v2"
    When "consumer-a" sends GET "/self-descriptions/{v2.enc}/versions" to "cat-a"
    Then the status is 200
    And the field "totalCount" is "2"
    And the result contains exactly "v1, v3" of this scenario
    And the result lists "v3, v1" in this order
    And the result "v3" has the field "version" set to "3"
    And the result "v3" has the field "status" set to "active"
    And the result "v1" has the field "version" set to "1"
    And the result "v1" has the field "status" set to "revoked"
    And the result "v1" has the field "name" set to "{token} v1 data"
    And the result "v1" has the field "offeringType" set to "data"

  @D19
  Scenario: The version history of a single version is empty
    Given "provider-a" has published a "DataOffering" resource description "v1"
    When "consumer-a" sends GET "/self-descriptions/{v1.enc}/versions" to "cat-a"
    Then the status is 200
    And the field "totalCount" is "0"
    And the field "items" is an empty list

  @D19 @D31
  Scenario: The version history is paged
    Given "provider-a" has published a "DataOffering" resource description "v1"
    And "provider-a" has published "v2" as version 2 of "v1"
    And "provider-a" has published "v3" as version 3 of "v2"
    When "consumer-a" sends GET "/self-descriptions/{v3.enc}/versions?page=2&pageSize=1" to "cat-a"
    Then the status is 200
    And the field "totalCount" is "2"
    And the result contains exactly "v1" of this scenario

  @D19
  Scenario: The version history of an unknown id is 404
    When "consumer-a" sends GET "/self-descriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}/versions" to "cat-a"
    Then the response is a problem with status 404

  # ------------------------------------------------------------------ revoke (D16)

  @D16
  Scenario: The owner revokes a resource description
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "provider-a" revokes "rd1"
    Then the status is 200
    And the field "id" is the id of "rd1"
    And the field "status" is "revoked"
    And the field "statusDatetime" is a timestamp
    And "rd1" is stored with the status "revoked"

  @D16
  Scenario: Revoking twice is idempotent
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    And "provider-a" has revoked "rd1"
    When "provider-a" revokes "rd1"
    Then the status is 200
    And the field "status" is "revoked"

  @D16
  Scenario: A revoked resource description stays readable
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    And "provider-a" has revoked "rd1"
    When "consumer-a" sends GET "/self-descriptions/{rd1.enc}" to "cat-a"
    Then the status is 200
    And the response is the stored resource description "rd1"

  @D16 @D19
  Scenario: A revoked resource description stays in the version history
    Given "provider-a" has published a "DataOffering" resource description "v1"
    And "provider-a" has published "v2" as version 2 of "v1"
    And "provider-a" has revoked "v2"
    When "consumer-a" sends GET "/self-descriptions/{v1.enc}/versions" to "cat-a"
    Then the status is 200
    And the result "v2" has the field "status" set to "revoked"

  @rbac @D16
  Scenario: Only the owner can revoke
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "provider-a2" revokes "rd1"
    Then the response is a problem with status 403
    And "rd1" is stored with the status "active"

  @rbac @D16
  Scenario: A Consumer cannot revoke
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "consumer-a" revokes "rd1"
    Then the response is a problem with status 403

  @D16
  Scenario: Revoking an unknown resource description is 404
    When "provider-a" sends POST "/self-descriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}/revoke" to "cat-a"
    Then the response is a problem "resourceNotFound" with status 404

  # ------------------------------------------------------------------ delete (D17)

  @D17
  Scenario: The owner deletes a resource description
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "provider-a" deletes "rd1"
    Then the status is 200
    And "rd1" is not stored

  @D17
  Scenario: The owner deletes a revoked resource description
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    And "provider-a" has revoked "rd1"
    When "provider-a" deletes "rd1"
    Then the status is 200
    And "rd1" is not stored

  @D17
  Scenario: Deleting the newest version does not reactivate the previous one
    Given "provider-a" has published a "DataOffering" resource description "v1"
    And "provider-a" has published "v2" as version 2 of "v1"
    When "provider-a" deletes "v2"
    Then the status is 200
    And "v1" is stored with the status "revoked"

  @D17 @D55
  Scenario: Deleting removes the quality assessment record too
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    And "provider-a" has deleted "rd1"
    When "consumer-a" sends GET "/self-descriptions/{rd1.enc}/qualityAssessmentRecord" to "cat-a"
    Then the response is a problem with status 404

  @rbac @D17
  Scenario: Only the owner can delete
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "provider-a2" deletes "rd1"
    Then the response is a problem with status 403
    And "rd1" is stored with the status "active"

  @D17
  Scenario: Deleting an unknown hash is 404
    When "provider-a" sends DELETE "/self-descriptions/0000000000000000000000000000000000000000000000000000000000000000" to "cat-a"
    Then the response is a problem with status 404

  # ------------------------------------------------------------------ list (D18)

  @D18
  Scenario: The list shows active resource descriptions by default
    Given "provider-a" has published a "DataOffering" resource description "active1"
    And "provider-a" has published a "DataOffering" resource description "revoked1"
    And "provider-a" has revoked "revoked1"
    When "consumer-a" sends GET "/self-descriptions?ids={active1.enc},{revoked1.enc}" to "cat-a"
    Then the status is 200
    And the result contains exactly "active1" of this scenario

  @D18
  Scenario: The list filters on several statuses at once
    Given "provider-a" has published a "DataOffering" resource description "active1"
    And "provider-a" has published a "DataOffering" resource description "revoked1"
    And "provider-a" has revoked "revoked1"
    When "consumer-a" sends GET "/self-descriptions?ids={active1.enc},{revoked1.enc}&statuses=active,revoked" to "cat-a"
    Then the status is 200
    And the result contains exactly "active1, revoked1" of this scenario
    And the result "revoked1" has the field "status" set to "revoked"

  @D18
  Scenario: The list shows the metadata by default and no content
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "consumer-a" sends GET "/self-descriptions?ids={rd1.enc}" to "cat-a"
    Then the status is 200
    And the field "totalCount" is "1"
    And the field "items/0/meta/id" is "{rd1.id}"
    And the field "items/0/meta/sdHash" is "{rd1.hash}"
    And the field "items/0/meta/status" is "active"
    And the field "items/0/meta/resourceKey" is "{rd1.id}"
    And the field "items/0/meta/currentVersion" is "1"
    And the field "items/0/meta/validatorDids" is an empty list
    And the field "items/0/meta/qualityAssessment/overall/score" is a number
    And the field "items/0/content" is null or absent

  @D18
  Scenario: The list returns the content as a JSON string on request
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "consumer-a" sends GET "/self-descriptions?ids={rd1.enc}&withContent=true" to "cat-a"
    Then the status is 200
    And the field "items/0/content" is a non-empty string
    And the field "items/0/content" contains the text "{rd1.id}"

  @D18
  Scenario: The list can leave out the metadata
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "consumer-a" sends GET "/self-descriptions?ids={rd1.enc}&withMeta=false&withContent=true" to "cat-a"
    Then the status is 200
    And the field "items/0/meta" is null or absent
    And the field "items/0/content" contains the text "{rd1.id}"

  @D18
  Scenario Outline: The list filters by <filter>
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    And "provider-a" has published a "ApplicationOffering" resource description "rd2"
    When "consumer-a" sends GET "/self-descriptions?ids={rd1.enc},{rd2.enc}&<filter>=<value>" to "cat-a"
    Then the status is 200
    And the result contains exactly "<expected>" of this scenario

    Examples:
      | filter     | value                         | expected |
      | hashes     | {rd2.hash}                    | rd2      |
      | hashes     | {rd1.hash},{rd2.hash}         | rd1, rd2 |
      | providedBy | provider-a                    | rd1, rd2 |
      | providedBy | provider-a2                   |          |

  @D18
  Scenario: The list filters by issuer
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    And I remember the field "issuer" as "issuer_a"
    When "consumer-a" sends GET "/self-descriptions?ids={rd1.enc}&issuers={issuer_a}" to "cat-a"
    Then the status is 200
    And the result contains exactly "rd1" of this scenario

  @D18 @D26
  Scenario: The validators filter matches nothing
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "consumer-a" sends GET "/self-descriptions?ids={rd1.enc}&validators=did:web:validator.example.org" to "cat-a"
    Then the status is 200
    And the result contains exactly "" of this scenario

  @D18
  Scenario: The list filters by upload time range
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "consumer-a" sends GET "/self-descriptions?ids={rd1.enc}&uploadTimerange=2026-01-01T00:00:00Z/2100-01-01T00:00:00Z" to "cat-a"
    Then the status is 200
    And the result contains exactly "rd1" of this scenario

  @D18
  Scenario: An upload time range in the past excludes new resource descriptions
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "consumer-a" sends GET "/self-descriptions?ids={rd1.enc}&uploadTimerange=2020-01-01T00:00:00Z/2021-01-01T00:00:00Z" to "cat-a"
    Then the status is 200
    And the result contains exactly "" of this scenario

  @D18
  Scenario Outline: A malformed time range is 400 (<range>)
    When "consumer-a" sends GET "/self-descriptions?<param>=<range>" to "cat-a"
    Then the response is a problem with status 400

    Examples:
      | param           | range                |
      | uploadTimerange | yesterday            |
      | uploadTimerange | 2026-01-01T00:00:00Z |
      | statusTimerange | 2026-13-01/2026-14-01 |

  @D18
  Scenario: The list is ordered by upload time, newest first
    Given "provider-a" has published a "DataOffering" resource description "first"
    And "provider-a" has published a "DataOffering" resource description "second"
    And "provider-a" has published a "DataOffering" resource description "third"
    When "consumer-a" sends GET "/self-descriptions?ids={first.enc},{second.enc},{third.enc}" to "cat-a"
    Then the status is 200
    And the result lists "third, second, first" in this order

  @D18
  Scenario: The list is paged with offset and limit
    Given "provider-a" has published a "DataOffering" resource description "first"
    And "provider-a" has published a "DataOffering" resource description "second"
    And "provider-a" has published a "DataOffering" resource description "third"
    When "consumer-a" sends GET "/self-descriptions?ids={first.enc},{second.enc},{third.enc}&offset=1&limit=1" to "cat-a"
    Then the status is 200
    And the field "totalCount" is "3"
    And the result contains exactly "second" of this scenario

  @rbac @D66
  Scenario Outline: Reading the catalogue needs a searcher role (<who>)
    When "<who>" sends GET "/self-descriptions?statuses=active&limit=1" to "cat-a"
    Then the status is <status>

    Examples:
      | who             | status |
      | anonymous       | 401    |
      | nobody-a        | 403    |
      | schema-admin-a  | 403    |
      | consumer-a      | 200    |
      | provider-a2     | 200    |
