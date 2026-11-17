<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Non-Functional Requirements](https://code.europa.eu/simpl/simpl-open/foundations/non-functional-requirements) / [NFR08 - Maintainability](https://code.europa.eu/simpl/simpl-open/foundations/non-functional-requirements/-/tree/main/nfr08-maintainability?ref_type=heads) / Enforce coding and development guidelines
</div>

# Enforce coding and development Guidelines

## Canonical source
[https://simpl-programme.ec.europa.eu/book-page/enforce-coding-and-development-guidelines](https://simpl-programme.ec.europa.eu/book-page/enforce-coding-and-development-guidelines)

## Description

Simpl-Open's development lifecyle shall follow well-defined coding and development guidelines to ensure consistency, readability, and maintainability of the codebase.

## SMART Breakdown

* Specific: All code contributions in Simpl-Open shall follow clearly documented guidelines covering formatting rules, naming conventions, modular design principles, error handling, and documentation expectations. These rules apply across all relevant languages and frameworks used in the platform.
* Measurable: Compliance shall be assessed using automated tools such as linters, code formatters, and static analysis tools integrated in the CI/CD pipeline. Metrics include:
    * Number of linting violations per commit
    * Percentage of repositories with enforced formatting rules
    * Code review checklists referencing guideline adherence
* Achievable: Guidelines shall be made available in a central, version-controlled location (e.g. Confluence or Git), and automatically enforced via pre-commit hooks, CI/CD jobs, or merge gate checks. Teams receive support through onboarding and shared best practices.
* Realistic: Development guidelines are a standard quality assurance measure in open and modular environments like Simpl-Open. Their enforcement reduces technical debt and ensures better collaboration across distributed teams.
* Timely: Guidelines shall be finalised before major development milestones and reviewed quarterly. Enforcement mechanisms must be included in CI/CD from early phases and refined iteratively.
