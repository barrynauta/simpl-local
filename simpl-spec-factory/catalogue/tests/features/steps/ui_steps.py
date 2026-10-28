"""Browser steps for features/ui (tagged @ui @wip). The browser implementation comes in a later
pipeline step; these definitions exist so the dry run resolves every step. Selectors are the
data-testid values of digest section 7."""
from behave import given, then, when


def _pending(*_args, **_kwargs):
    raise NotImplementedError("browser step pending: implemented in the UI test-writer step")


given('I am logged in to the UI as {user:Q}')(_pending)
given('the browser request to {path:Q} fails')(_pending)
when('I open the {tab:Q} tab')(_pending)
when('I type {text:Q} into {testid:Q}')(_pending)
when('I clear {testid:Q}')(_pending)
when('I click {testid:Q}')(_pending)
when('I check {testid:Q}')(_pending)
when('I uncheck {testid:Q}')(_pending)
when('I select {value:Q} in {testid:Q}')(_pending)
when('I open the result row of {alias:Q} in {testid:Q}')(_pending)
when('I open the detail page of {alias:Q} in the catalogue {cid:Q}')(_pending)
then('{testid:Q} is visible')(_pending)
then('{testid:Q} is not visible')(_pending)
then('{testid:Q} is enabled')(_pending)
then('{testid:Q} is disabled')(_pending)
then('{testid:Q} is checked')(_pending)
then('{testid:Q} is not checked')(_pending)
then('{testid:Q} shows the text {text:Q}')(_pending)
then('{testid:Q} contains the text {text:Q}')(_pending)
then('{testid:Q} has the options {options:QList} in this order')(_pending)
then('the result tabs are {ids:QList} in this order')(_pending)
then('{testid:Q} shows the result row of {alias:Q}')(_pending)
then('{testid:Q} does not show the result row of {alias:Q}')(_pending)
then('the result row of {alias:Q} in {testid:Q} shows {child:Q}')(_pending)
then('the result row of {alias:Q} in {testid:Q} does not show {child:Q}')(_pending)
then('{testid:Q} shows {n:d} result rows')(_pending)
then('the detail page shows the property {path:Q} with the value {value:Q}')(_pending)
then('the detail page shows no property rows')(_pending)
then('the detail page shows a property row for every property of {alias:Q}')(_pending)
then('the browser requested {path:Q}')(_pending)
then('the last advanced search request body is the JSON')(_pending)
then('the browser sent no request to {path:Q}')(_pending)
