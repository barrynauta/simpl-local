Feature: Upload of contract clause documents at the Provider
  CM stores the document, computes the SHA-256 of the exact bytes and returns
  metadata only (SIMPL-28846, D63, A.7).

  @SIMPL-28846 @D63
  Scenario Outline: SIMPL-28846 AC1 and AC2 - a <kind> document is stored and its metadata returned
    When I upload the fixture "<file>" with media type "<mediaType>" to the provider CM
    Then the response status is 201
    And the response fields are:
      | field         | value       |
      | documentName  | <file>      |
      | mediaType     | <mediaType> |
      | hashAlgorithm | SHA-256     |
    And the response field "hash" is the SHA-256 of the uploaded content
    And the response field "size" is the size of the uploaded content
    And the response field "documentId" matches "[0-9a-fA-F-]{36}"
    And I remember the response field "documentId" as "documentId"
    And the response field "sourceReference" is "urn:simpl:contract-clause:${documentId}"
    And the response does not contain the uploaded content

    Examples:
      | kind     | file        | mediaType       |
      | PDF      | clauses.pdf | application/pdf |
      | text     | clauses.txt | text/plain      |
      | markdown | clauses.md  | text/markdown   |

  @SIMPL-28846 @D63
  Scenario: SIMPL-28846 AC3 - a media type that is not allowed is refused with 415
    When I upload the fixture "image.png" with media type "image/png" to the provider CM
    Then the response is a problem with status 415 and type "invalidDocument"

  @SIMPL-28846 @D63
  Scenario: SIMPL-28846 AC3 - a document larger than the limit is refused with 413
    When I upload a file "big.txt" of the maximum size plus 1 bytes with media type "text/plain" to the provider CM
    Then the response is a problem with status 413

  @SIMPL-28846 @D63
  Scenario: SIMPL-28846 AC3 - an empty document is refused with 400
    When I upload the file "empty.txt" with media type "text/plain" and content "" to the provider CM
    Then the response is a problem with status 400

  @SIMPL-28846 @D63
  Scenario: An upload without a file part is refused with 400
    When I send a multipart upload without a file part to the provider CM
    Then the response is a problem with status 400

  @SIMPL-28846 @D63
  Scenario: SIMPL-28846 AC6 - uploading the same content again creates a new document
    When I upload the file "same.txt" with media type "text/plain" and content "Same clause text" to the provider CM
    Then the response status is 201
    And I remember the response field "documentId" as "firstDocumentId"
    And I remember the response field "hash" as "firstHash"
    When I upload the file "same.txt" with media type "text/plain" and content "Same clause text" to the provider CM
    Then the response status is 201
    And the response field "hash" is "${firstHash}"
    And the response field "documentId" does not contain "${firstDocumentId}"

  @SIMPL-28846 @D63 @D65 @rbac
  Scenario: An upload without the API key is refused with 401
    When I upload the file "x.txt" with media type "text/plain" to the provider CM without an API key
    Then the response is a problem with status 401 and type "unauthorized"
