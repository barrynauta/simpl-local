@api @cat @quality
Feature: catalogue-be gates publication on the quality assessment and stores the record
  SIMPL-27998 with the quality stub of 6.5. Default profile: overall threshold 70, Completeness
  threshold 90; classes Red 0 to 60, Yellow 60 to 80, Green 80 to 100 (D49 to D53).
  The record is stored and can be replaced (D55).

  @SIMPL-27998 @D50 @D51
  Scenario: A resource description that meets all thresholds is published with its record
    Given a "DataOffering" resource description "rd1" by "provider-a"
    When "provider-a" publishes "rd1"
    Then the status is 201
    And the field "qualityAssessment/overall/score" is "100"
    And the field "qualityAssessment/overall/classification" is "Green"
    And the field "qualityAssessment/overall/thresholdStatus" is "PASSED"
    And the field "qualityAssessment/dimensions" has 3 items
    And the field "qualityAssessment/dimensions/[name=Completeness]/score" is "100"
    And the field "qualityAssessment/dimensions/[name=Completeness]/classification" is "Green"
    And the field "qualityAssessment/dimensions/[name=Completeness]/thresholdStatus" is "PASSED"
    And the field "qualityAssessment/rules/0/status" is "PASSED"
    When "consumer-a" sends GET "/self-descriptions/{rd1.enc}/qualityAssessmentRecord" to "cat-a"
    Then the status is 200
    And the field "@graph/[@type=QualityAssessmentRecord]/computedOn" is "{rd1.id}"

  @SIMPL-27998 @D51
  Scenario Outline: Scores at or above every threshold pass (<scores>)
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And the quality stub scores "rd1" with "<scores>"
    When "provider-a" publishes "rd1"
    Then the status is 201
    And the field "qualityAssessment/overall/score" is "<overall>"
    And the field "qualityAssessment/overall/classification" is "<class>"

    Examples:
      | scores                                       | overall | class  |
      | overall=70, Completeness=90                  | 70      | Yellow |
      | overall=79.5, Completeness=95                | 79.5    | Yellow |
      | overall=80, Completeness=90, Accuracy=10     | 80      | Green  |

  @SIMPL-27998 @D51
  Scenario Outline: Scores below a threshold are rejected and nothing is stored (<scores>)
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And the quality stub scores "rd1" with "<scores>"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:qualityBelowThreshold" with status 422
    And the problem detail is "<detail>"
    And "rd1" is not stored

    Examples:
      | scores                          | detail                                    |
      | overall=65                      | Quality Score 65 is below treshold 70.    |
      | overall=69.99                   | Quality Score 69.99 is below treshold 70. |
      | overall=80, Completeness=85     | Quality Score 85 is below treshold 90.    |
      | overall=95, Completeness=89.5   | Quality Score 89.5 is below treshold 90.  |
      | overall=65, Completeness=50     | Quality Score 65 is below treshold 70.    |

  @SIMPL-27998 @D51 @D53
  Scenario: A dimension without a threshold does not block and is classified
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And the quality stub scores "rd1" with "overall=90, Accuracy=10, Validity=65"
    When "provider-a" publishes "rd1"
    Then the status is 201
    And the field "qualityAssessment/dimensions/[name=Accuracy]/score" is "10"
    And the field "qualityAssessment/dimensions/[name=Accuracy]/classification" is "Red"
    And the field "qualityAssessment/dimensions/[name=Validity]/classification" is "Yellow"
    And the field "qualityAssessment/dimensions/[name=Completeness]/classification" is "Green"

  @SIMPL-27998 @D53
  Scenario: The top of the highest range belongs to that range
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And the quality stub scores "rd1" with "overall=100"
    When "provider-a" publishes "rd1"
    Then the status is 201
    And the field "qualityAssessment/overall/classification" is "Green"

  @SIMPL-27998 @D52
  Scenario Outline: A failing quality assessment blocks publication with 502 (<mode>)
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And the quality stub answers "rd1" in mode "<mode>"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:qualityAssessmentFailed" with status 502
    And "rd1" is not stored

    Examples:
      | mode    |
      | invalid |
      | error   |

  @SIMPL-27998 @D52 @slow
  Scenario: A quality assessment that times out blocks publication with 502
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And the quality stub answers "rd1" in mode "timeout"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:qualityAssessmentFailed" with status 502
    And "rd1" is not stored

  @SIMPL-27998 @D52
  Scenario Outline: A missing quality configuration is a 500 (<shape>)
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" conforms to the shape "<shape>"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:qualityConfigurationError" with status 500
    And "rd1" is not stored

    Examples:
      | shape            |
      | NoProfileShape   |
      | NoThresholdShape |

  @SIMPL-27998 @D14
  Scenario: The quality gate runs after the schema checks
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And the quality stub scores "rd1" with "overall=10"
    And "rd1" is broken as "missing-name"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:shaclViolation" with status 422

  @SIMPL-27998 @D14
  Scenario: The quality gate runs after the duplicate check
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    And a "DataOffering" resource description "rd2" by "provider-a"
    And "rd2" has the field "credentialSubject/@id" set to "{rd1.id}"
    And the quality stub answers "rd1" in mode "error"
    When "provider-a" publishes "rd2"
    Then the response is a problem "catalogue:alreadyExists" with status 409

  @SIMPL-27998 @D51
  Scenario: A new version passes through the quality gate too
    Given "provider-a" has published a "DataOffering" resource description "v1"
    And "v2" is version 2 of "v1"
    And the quality stub scores "v2" with "overall=50"
    When "provider-a" publishes "v2"
    Then the response is a problem "catalogue:qualityBelowThreshold" with status 422
    And "v1" is stored with the status "active"

  # ------------------------------------------------------------------ QAR store (D55)

  @D55
  Scenario: The stored record has the structure of the quality stub
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "consumer-a" sends GET "/self-descriptions/{rd1.enc}/qualityAssessmentRecord" to "cat-a"
    Then the status is 200
    And the field "@graph/[@type=SelfDescriptionScore]/score" is "100"
    And the field "@graph/[@type=SelfDescriptionScore]/maxScore" is "100"
    And the field "@graph/[@type=QualityAssessmentRecord]/profileVersion" is "1.0.0"

  @D55 @D49
  Scenario: Replacing the record recomputes the stored summary without gating
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "provider-a" posts a quality assessment record for "rd1" with the score 0.25 of 1
    Then the status is 200
    And the field "status" is a non-empty string
    When "consumer-a" sends GET "/self-descriptions?ids={rd1.enc}" to "cat-a"
    Then the result "rd1" has the field "meta/qualityAssessment/overall/score" set to "25"
    And the result "rd1" has the field "meta/qualityAssessment/overall/classification" set to "Red"
    And "rd1" is stored with the status "active"
    When "consumer-a" sends GET "/self-descriptions/{rd1.enc}/qualityAssessmentRecord" to "cat-a"
    Then the field "@graph/[@type=SelfDescriptionScore]/score" is "0.25"

  @D55
  Scenario Outline: An invalid record is rejected with 400 (<variant>)
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "provider-a" posts a quality assessment record for "rd1" with "<variant>"
    Then the response is a problem with status 400
    When "consumer-a" sends GET "/self-descriptions/{rd1.enc}/qualityAssessmentRecord" to "cat-a"
    Then the field "@graph/[@type=SelfDescriptionScore]/score" is "100"

    Examples:
      | variant                                  |
      | an empty graph                           |
      | a node without @type                     |
      | a node without @id                       |
      | no QualityAssessmentRecord node          |
      | two QualityAssessmentRecord nodes        |
      | computedOn another resource description  |
      | no SelfDescriptionScore node             |
      | a non-numeric score                      |
      | a maxScore of zero                       |

  @D55
  Scenario: A record for an unknown resource description is 404
    When "provider-a" posts a quality assessment record for an unknown resource description to "cat-a"
    Then the response is a problem with status 404

  @D55
  Scenario: Reading the record of an unknown resource description is 404
    When "consumer-a" sends GET "/self-descriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}/qualityAssessmentRecord" to "cat-a"
    Then the response is a problem with status 404

  @rbac @D67
  Scenario: Writing a record needs a valid token but no role
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "nobody-a" posts a quality assessment record for "rd1" with the score 90 of 100
    Then the status is 200
    When "anonymous" posts a quality assessment record for "rd1" with the score 90 of 100
    Then the response is a problem with status 401

  @rbac @D66
  Scenario: Reading a record needs a searcher role
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "nobody-a" sends GET "/self-descriptions/{rd1.enc}/qualityAssessmentRecord" to "cat-a"
    Then the response is a problem with status 403
