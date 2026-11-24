# UI notes (D177, D200)

Free texts that no decision fixes (tests do not check their wording):

* Field errors: "Name is required.", "Name must have 3 to 64 characters.", "Name must start with an upper-case
  letter and contain only letters and digits.", "Description is required.", "Description must have at most 1000
  characters.", "Change log is required.", "Vocabulary file is required.", "Namespace URI is required.",
  "Namespace URI must be an absolute URI.", "At most 20 alias URIs are allowed.", "Every alias URI must be an
  absolute URI."
* Browser syntax error: "Syntax error at line <n>: <parser message>" (the column is not given by N3.js).
* Page titles: "New vocabulary", "New version of <Name>", "Register external vocabulary", "External vocabularies",
  section "Version history"; external list empty state "No external vocabularies found."; external list load
  error "The external vocabulary list could not be loaded."; dialog title for removal "Remove external vocabulary".
* Header: "Vocabulary Manager", user name, "Log out".

Known parser differences (D200): the browser check uses N3.js 2.13, the server Jena RIOT 6.3. N3.js accepts a few
inputs that Jena refuses (for example some invalid IRIs and invalid UTF-8); the server then reports the error in
the validation report. No valid Turtle file was found that N3.js refuses.

Not implemented in the UI: the Figma design (not available, see the specs README); version status change (API
only, D4).
