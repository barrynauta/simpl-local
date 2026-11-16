@DataProvider @ContractBilling @regression @Data1
Feature: Contract Billing - Functional Tests
  As a QA engineer
  I want to verify that Contract Billing API endpoints
  behave correctly for authorized and unauthorized requests

  @TC_CB_02 @readonly @sanity
  Scenario: Authorized user gets agreement by random id
    Given a user with role "SD_PUBLISHER" logs in based on the "DATA_PROVIDER"
    When the user gets CB agreement by random id
    Then the response status code is 404

  @TC_CB_04 @readonly
  Scenario: Authorized user issues credential with random ids
    Given a user with role "SD_PUBLISHER" logs in based on the "DATA_PROVIDER"
    When the user issues a CB credential with random ids
    Then the response status code is 404

  @TC_CB_06 @readonly
  Scenario: Authorized user gets agreement file by random id
    Given a user with role "SD_PUBLISHER" logs in based on the "DATA_PROVIDER"
    When the user gets CB agreement file by random id
    Then the response status code is 404

  @TC_CB_07 @live-only @readonly @sanity
  Scenario: Unauthorized user checks health
    Given a user without authorization is based on "DATA_PROVIDER"
    When the user checks CB health endpoint
    Then the response status code is 401
