"""Compare newman JSON results of upstream and build: status code and field sets per request."""
import json,sys,os
d=os.path.dirname(os.path.abspath(__file__))
def load(t):
    r=json.load(open(f"{d}/results/{t}.json")); out={}
    for e in r["run"]["executions"]:
        rs=e.get("response",{}); b=bytes(rs.get("stream",{}).get("data",[])).decode(errors="replace")
        try: j=json.loads(b)
        except Exception: j=None
        if isinstance(j,dict) and isinstance(j.get("items"),list) and j["items"]: keys=set(j)|{"items[]."+k for k in j["items"][0]}
        else: keys=set(j) if isinstance(j,dict) else set()
        fails=[a["assertion"] for a in e.get("assertions",[]) if a.get("error")]
        out[e["item"]["name"]]=(rs.get("code"),keys,len(e.get("assertions",[])),len(fails))
    return out
u,b=load("upstream"),load("build")
for n in u:
    cu,ku,au,fu=u[n]; cb,kb,ab,fb=b[n]
    print(f"{n}: upstream {cu} ({au-fu}/{au}) build {cb} ({ab-fb}/{ab})")
    if ku-kb: print("   only upstream:",sorted(ku-kb))
    if kb-ku: print("   only build:   ",sorted(kb-ku))
