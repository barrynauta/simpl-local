"""Behave environment of the Catalogue black-box suite.

All configuration comes from environment variables. Defaults are the host URLs of
digest section 5.1 (the factory build, compose project simpl-catalogue-factory).

Variables
---------
AUTH_MODE                  oidc (default): tokens from Keycloak, realms and clients of digest 5.2.
                           none: no Authorization header; scenarios tagged @rbac are skipped.
CAT_A_URL                  catalogue-be of data space A. Default http://localhost:18101
QMA_A_URL                  query mapper adapter A, with /v1. Default http://localhost:18102/v1
CBE_A_URL                  catalogue client backend A, without version. Default http://localhost:18103
CBE_A_V1_URL, CBE_A_V2_URL Override the cbe version roots. Default CBE_A_URL + /v1 and /v2
UI_A_URL                   UI of data space A. Default http://localhost:18104
CAT_B_URL                  catalogue-be of data space B. Default http://localhost:18201
QMA_B_URL                  adapter B (Trustor GA), with /v1. Default http://localhost:18202/v1
QMA_C_URL                  adapter C (trusts nobody), with /v1. Default http://localhost:18302/v1
SCHEMA_STUB_URL            Schema Manager stand-in. Default http://localhost:18401
QUALITY_STUB_URL           Quality Scoring stand-in with its control API. Default http://localhost:18402
KEYCLOAK_URL               Keycloak as seen from the host. Default http://localhost:18080
TEST_CLIENT_ID             Public client with direct access grants. Default test-client
USER_PASSWORD              Password of all test users. Default Passw0rd!
QMA_A_FEDERATION_SECRET    Secret of client qma-a-federation (realm dataspace-a). Default qma-a-federation-secret
QMA_B_SERVICE_SECRET       Secret of client qma-b-service (realm dataspace-b). Default qma-b-service-secret
QMA_C_FEDERATION_SECRET    Secret of client qma-c-federation (realm dataspace-c). Default qma-c-federation-secret
HTTP_TIMEOUT               Seconds per HTTP call of the suite. Default 30
NONE_MODE_ATTRIBUTE_HEADER true (default): with AUTH_MODE=none the suite sends the identity attributes
                           of the acting user (digest 5.3) as User-Attributes, because no token can
                           carry them. Has no effect with AUTH_MODE=oidc.
SKIP_TAGS                  Comma-separated tags to skip with the reason "missing in this environment",
                           for example federation,cbe,quality,stub for the upstream stack.
KEEP_TEST_DATA             true: do not clean up after a scenario (debugging). Default false
TEST_VERBOSE               true: print every request and response line. Default false
"""
import os
import sys

import parse
from behave import register_type

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from support import auth, client, config  # noqa: E402


@parse.with_pattern(r'"[^"]*"')
def parse_quoted(text):
    return text[1:-1]


@parse.with_pattern(r'"[^"]*"')
def parse_quoted_list(text):
    return [part.strip() for part in text[1:-1].split(",") if part.strip()]


@parse.with_pattern(r"-?\d+(?:\.\d+)?")
def parse_number(text):
    value = float(text)
    return int(value) if value.is_integer() else value


register_type(Q=parse_quoted, QList=parse_quoted_list, Num=parse_number)


def before_all(context):
    context.cfg = config.Config()
    context.tokens = auth.TokenProvider(context.cfg)


def before_scenario(context, scenario):
    import secrets
    tags = set(scenario.effective_tags)
    cfg = context.cfg
    run_token = "t" + secrets.token_hex(5)
    context.vars = {"token": run_token, "TOKEN": run_token.upper(), "a256": "a" * 256}
    context.rds = {}
    context.shapes = {}
    context.stub_scores = []
    context.pending_headers = {}
    context.response = None
    context.saved = {}
    if "wip" in tags and os.environ.get("RUN_WIP", "") != "true":
        scenario.skip("work in progress (browser steps come in a later step)")
        return
    if cfg.auth_mode == "none" and "rbac" in tags:
        scenario.skip("needs authentication (AUTH_MODE=none)")
        return
    for tag in cfg.skip_tags:
        if tag in tags:
            scenario.skip(f"missing in this environment (@{tag})")
            return


def after_scenario(context, scenario):
    if getattr(context, "cfg", None) is None or context.cfg.keep_data:
        return
    problems = []
    for rd in reversed(list(getattr(context, "rds", {}).values())):
        if not rd.published_on or rd.deleted:
            continue
        sd_hash = rd.sd_hash or rd.computed_hash()
        if not sd_hash:
            continue
        try:
            r = client.send(context, rd.published_by, "DELETE", rd.published_on,
                            f"/self-descriptions/{sd_hash}", consume_pending=False)
            if r.status not in (200, 204, 404):
                problems.append(f"delete {rd.alias}: {r.status}")
        except Exception as exc:  # clean-up must not hide the scenario result
            problems.append(f"delete {rd.alias}: {exc}")
    for shape in getattr(context, "shapes", {}).values():
        if not shape.uploaded_on:
            continue
        try:
            client.send(context, shape.uploaded_by, "DELETE", shape.uploaded_on,
                        f"/schemas/{getattr(shape, 'delete_id', None) or shape.name}", consume_pending=False)
        except Exception as exc:
            problems.append(f"delete shape {shape.name}: {exc}")
    from urllib.parse import quote
    for rd_id in getattr(context, "stub_scores", []):
        try:
            client.send(context, "anonymous", "DELETE", "quality-stub",
                        f"/stub/scores/{quote(rd_id, safe='')}", consume_pending=False)
        except Exception as exc:
            problems.append(f"delete stub score: {exc}")
    if problems and context.cfg.verbose:
        print("clean-up problems: " + "; ".join(problems))
