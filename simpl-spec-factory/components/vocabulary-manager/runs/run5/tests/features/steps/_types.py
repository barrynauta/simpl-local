"""Custom step parameter types. Loaded first (file name sorts first).

Q: a quoted value without double quotes, so "{name:Q}" never swallows the rest
of a longer step text.
"""
import parse
from behave import register_type


@parse.with_pattern(r'[^"]*')
def _quoted(text):
    return text


register_type(Q=_quoted)
