"""Custom parse types shared by all step modules.

Q      a double-quoted value, returned without the quotes ("" allowed)
Party  provider | consumer
Method a quoted HTTP method
"""
import parse
from behave import register_type


@parse.with_pattern(r'"[^"]*"')
def parse_quoted(text):
    return text[1:-1]


@parse.with_pattern(r"provider|consumer")
def parse_party(text):
    return text


@parse.with_pattern(r'"(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)"')
def parse_method(text):
    return text[1:-1]


register_type(Q=parse_quoted, Party=parse_party, Method=parse_method)
