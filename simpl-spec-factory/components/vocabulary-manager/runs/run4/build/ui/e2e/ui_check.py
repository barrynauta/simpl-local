"""Own UI check against the running stack (Playwright for Python). Not part of 'mvn verify'.

Run: python3 ui_check.py   (needs: pip install playwright && playwright install chromium)
"""
import os
import random
import re
import sys
import tempfile

from playwright.sync_api import expect, sync_playwright

UI = os.environ.get("UI_URL", "http://localhost:4433")
RUN = str(random.randint(100000, 999999))
NAME = f"UiCheck{RUN}"
TTL = f"""@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix ui: <https://example.eu/ui/{RUN}#> .
ui:Scheme a skos:ConceptScheme ; skos:prefLabel "UI check"@en .
"""


def login(page, user):
    page.goto(UI + "/vocabularies")
    page.fill("#username", user)
    page.fill("#password", "Password1!")
    page.click("#kc-login")
    expect(page.get_by_role("heading", name="Vocabularies")).to_be_visible()


def main():
    folder = tempfile.mkdtemp()
    good = os.path.join(folder, "good.ttl")
    bad = os.path.join(folder, "bad.ttl")
    open(good, "w").write(TTL)
    open(bad, "w").write("@prefix ex: <http://e/> .\nex:a ex:b ;;; .\n")
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page()
        login(page, "ga-admin")
        page.get_by_role("link", name="New Vocabulary +").click()
        page.get_by_label("Name").fill(NAME)
        page.get_by_label("Description").fill("Created by the UI check")
        page.get_by_label("File upload").set_input_files(bad)
        expect(page.get_by_text(re.compile(r"Line \d+, column \d+"))).to_be_visible()
        expect(page.get_by_role("button", name="Submit")).to_be_disabled()
        page.get_by_label("File upload").set_input_files(good)
        expect(page.get_by_text("Syntax check passed.")).to_be_visible()
        page.get_by_role("button", name="Submit").click()
        expect(page.get_by_text(f"Version 1 of {NAME} was created.")).to_be_visible()
        expect(page.get_by_role("heading", name=NAME)).to_be_visible()
        page.get_by_role("link", name="Update").click()
        expect(page.get_by_text(f"{NAME}.ttl (current content)")).to_be_visible()
        expect(page.get_by_text("Submitting creates version 2 and deprecates every older ACTIVE version.")).to_be_visible()
        page.get_by_label("Description").fill("Second description")
        page.get_by_label("Change log").fill("Description only")
        page.get_by_role("button", name="Submit").click()
        expect(page.get_by_text(f"Version 2 of {NAME} was created.")).to_be_visible()
        expect(page.get_by_text("This version is deprecated.").first).to_be_visible()
        expect(page.get_by_text("Latest ACTIVE version: 2").first).to_be_visible()
        page.get_by_role("button", name="View version 1").click()
        expect(page.get_by_label("Version details")).to_contain_text("ga-admin@example.com")
        with page.expect_download() as download:
            page.get_by_label("Version details").get_by_role("button", name="Download").click()
        assert download.value.suggested_filename == f"{NAME}-v1.ttl", download.value.suggested_filename
        page.get_by_role("button", name="Change status to Deprecated").click()
        expect(page.get_by_text(f"The status of {NAME} was updated to Deprecated.")).to_be_visible()
        expect(page.get_by_text("This vocabulary is deprecated. Its use is discouraged.")).to_be_visible()
        # Validation report (server 400) and warnings panel (deprecated reference, D11 and D14).
        semantic = os.path.join(folder, "semantic.ttl")
        open(semantic, "w").write(
            "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n@prefix ex: <http://example.org/> .\n"
            "ex:p a owl:ObjectProperty .\nex:s ex:p \"literal\" .\n")
        page.goto(UI + "/vocabularies/new")
        page.get_by_label("Name").fill(NAME + "Bad")
        page.get_by_label("Description").fill("Semantic error")
        page.get_by_label("File upload").set_input_files(semantic)
        expect(page.get_by_text("Syntax check passed.")).to_be_visible()
        page.get_by_role("button", name="Submit").click()
        expect(page.get_by_label("Validation report")).to_contain_text("Literal value for object property")
        referencing = os.path.join(folder, "ref.ttl")
        open(referencing, "w").write(
            f"@prefix old: <https://authority.example/vocabulary-manager/content/{NAME}/versions/1#> .\n"
            f"@prefix me: <https://example.eu/ref/{RUN}#> .\nme:a me:uses old:Scheme .\n")
        page.goto(UI + "/vocabularies/new")
        page.get_by_label("Name").fill(NAME + "Ref")
        page.get_by_label("Description").fill("Uses a deprecated version")
        page.get_by_label("File upload").set_input_files(referencing)
        expect(page.get_by_text("Syntax check passed.")).to_be_visible()
        page.get_by_role("button", name="Submit").click()
        expect(page.get_by_label("Validation warnings")).to_contain_text(
            f"Referenced internal vocabulary '{NAME}' version 1 is deprecated.")
        page.goto(UI + "/vocabularies")
        page.get_by_label("Name").fill(NAME)
        row = page.locator(f"tr[data-name='{NAME}']")
        expect(row).to_contain_text("Deprecated")
        expect(row).to_contain_text("This vocabulary is deprecated. Its use is discouraged.")
        row.get_by_role("button", name="Actions").click()
        expect(row.get_by_role("menuitem", name="View vocabulary details")).to_be_visible()
        expect(row.get_by_role("menuitem", name="Change status to Active")).to_be_visible()
        page.route(f"**/v1/vocabularies/{NAME}", lambda route: route.fulfill(
            status=404, content_type="application/problem+json", body='{"status":404,"detail":"gone"}')
            if route.request.method == "PATCH" else route.continue_())
        row.get_by_role("menuitem", name="Change status to Active").click()
        expect(page.get_by_text("The vocabulary could not be found.")).to_be_visible()
        expect(row).to_contain_text("Deprecated")
        page.route("**/v1/vocabularies?**", lambda route: route.fulfill(
            status=500, content_type="application/problem+json",
            body='{"status":500,"title":"Internal Server Error","detail":"Stubbed failure"}'))
        page.reload()
        expect(page.get_by_text("The vocabulary list could not be loaded.")).to_be_visible()
        expect(page.get_by_text("Stubbed failure")).to_be_visible()
        browser.close()

        browser = p.chromium.launch()
        page = browser.new_page()
        login(page, "ga-viewer")
        expect(page.get_by_role("link", name="New Vocabulary +")).to_have_count(0)
        page.goto(UI + "/vocabularies/new")
        expect(page.get_by_text("You are not authorized to perform this action.")).to_be_visible()
        browser.close()
    print("UI check passed.")


if __name__ == "__main__":
    sys.exit(main())
