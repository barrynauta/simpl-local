"""Placeholder expansion for step arguments, tables and doc strings.

${Alpha}          the real name of the scenario alias Alpha (Alpha + scenario uid)
${Alpha.upper}    the same name in upper case (case-insensitive uniqueness, D37, D54)
${Alpha.lower}    the same name in lower case
${uid}            the scenario uid (lower case letters and digits)
${contentBase}    contentBase of E2
${email:ga-admin} the email claim of a test user (D27)
${saved:key}      a value remembered earlier in the scenario
{{repeat:x:1000}} the character x repeated 1000 times
{{name:65}}       a name of exactly 65 characters that matches the name pattern and contains the uid
"""
import re

from . import client, config

_PLACEHOLDER = re.compile(r"\$\{([^}]+)\}")
_FUNC = re.compile(r"\{\{([a-z]+):([^}]*)\}\}")


class Names:
    def __init__(self, uid):
        self.uid = uid
        self.map = {}

    def real(self, alias):
        if alias not in self.map:
            if not re.match(r"^[A-Z][A-Za-z0-9]*$", alias):
                raise ValueError("alias %r must match ^[A-Z][A-Za-z0-9]*$" % alias)
            self.map[alias] = alias + self.uid
        return self.map[alias]


def expand(context, text):
    if text is None:
        return None

    def func(m):
        kind, arg = m.group(1), m.group(2)
        if kind == "repeat":
            ch, n = arg.rsplit(":", 1)
            return ch * int(n)
        if kind == "name":
            n = int(arg)
            base = "N" + context.names.uid
            return (base + "a" * n)[:n] if n >= len(base) else ("N" + "a" * (n - 1))
        raise ValueError("unknown function %s" % kind)

    def ph(m):
        key = m.group(1)
        if key == "uid":
            return context.names.uid
        if key == "contentBase":
            return client.content_base()
        if key.startswith("email:"):
            return config.USER_EMAILS[key.split(":", 1)[1]]
        if key.startswith("saved:"):
            return str(context.saved[key.split(":", 1)[1]])
        if "." in key:
            alias, mod = key.split(".", 1)
            real = context.names.real(alias)
            if mod == "upper":
                return real.upper()
            if mod == "lower":
                return real.lower()
            raise ValueError("unknown modifier %s" % mod)
        return context.names.real(key)

    text = _FUNC.sub(func, text)
    return _PLACEHOLDER.sub(ph, text)
