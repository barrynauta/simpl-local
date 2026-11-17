"""Compare behave JSON reports per scenario. Usage: compare_ui.py label=report.json ..."""
import json,sys
cols=[];data={}
for a in sys.argv[1:]:
    lab,p=a.split("=",1); cols.append(lab)
    for f in json.load(open(p)):
        for s in f.get("elements",[]):
            if s.get("type")!="scenario": continue
            err=""
            for st in s.get("steps",[]):
                r=st.get("result",{})
                if r.get("status")=="failed":
                    err=(st["keyword"]+" "+st["name"]+" :: "+" ".join(r.get("error_message",[""]) if isinstance(r.get("error_message"),list) else str(r.get("error_message")))).replace("\n"," ")[:230]
            data.setdefault(s["name"],{})[lab]=(s.get("status"),err)
for n,v in data.items():
    print(n); 
    for c in cols:
        st,e=v.get(c,("-",""))
        print(f"   {c:10} {st:7} {e}")
print("TOTAL", {c: sum(1 for v in data.values() if v.get(c,("",""))[0]=="passed") for c in cols}, "of", len(data))
