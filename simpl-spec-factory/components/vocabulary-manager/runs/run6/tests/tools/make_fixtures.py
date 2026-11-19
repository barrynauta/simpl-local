"""Writes the Turtle fixtures under fixtures/turtle. Run once; the files are committed with the suite.

Tokens such as {{run}} are filled in by the suite at run time (vmtest/world.py).
"""
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "fixtures" / "turtle"

FILES = {
    # D9 syntax: the first parse error is on line 3 ('@@@' where an object is expected).
    "syntax_error_line3.ttl": """@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix own: <https://example.org/{{run}}/syntax#> .
own:Scheme a skos:ConceptScheme ; skos:prefLabel @@@ .
own:Other a skos:ConceptScheme .
""",
    # D9: semantic errors (S1, S4) before a syntax error; only the syntax error is reported.
    "syntax_error_after_semantic_errors.ttl": """@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix own: <https://example.org/{{run}}/mixed#> .
own:hasPart a owl:ObjectProperty .
own:A own:hasPart "a literal" .
own:A skos:prefLabel "One"@en , "Two"@en .
own:B a skos:Concept ;; ] .
""",
    "s1_literal_for_object_property.ttl": """@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix own: <https://example.org/{{run}}/s1#> .

own:hasPart a owl:ObjectProperty .
own:Engine own:hasPart "a literal value" .
""",
    "s2_resource_for_datatype_property.ttl": """@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix own: <https://example.org/{{run}}/s2#> .

own:age a owl:DatatypeProperty .
own:Person own:age own:Forty .
""",
    "s3_object_and_datatype_property.ttl": """@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix own: <https://example.org/{{run}}/s3#> .

own:size a owl:ObjectProperty , owl:DatatypeProperty .
""",
    "s4_duplicate_preflabel.ttl": """@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix own: <https://example.org/{{run}}/s4#> .

own:Concept a skos:Concept ;
    skos:prefLabel "First label"@en , "Second label"@en .
""",
    "s4_two_untagged_preflabels.ttl": """@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix own: <https://example.org/{{run}}/s4u#> .

own:Concept a skos:Concept ;
    skos:prefLabel "First label" , "Second label" .
""",
    "s4_untagged_and_tagged_preflabel.ttl": """@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix own: <https://example.org/{{run}}/s4t#> .

own:Concept a skos:Concept ;
    skos:prefLabel "Label without language" , "Label in English"@en .
""",
    "s4_labels_in_three_languages.ttl": """@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix own: <https://example.org/{{run}}/s4l#> .

own:Concept a skos:Concept ;
    skos:prefLabel "Health"@en , "Gesundheit"@de , "Sante"@fr .
""",
    "s5_disjoint_classes.ttl": """@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix own: <https://example.org/{{run}}/s5#> .

own:Cat a owl:Class .
own:Dog a owl:Class ;
    owl:disjointWith own:Cat .
own:Tom a own:Cat , own:Dog .
""",
    "s6_no_statements.ttl": """# A vocabulary file that parses as Turtle but holds no triples ({{run}}).
""",
    # S1, S3 and S4 in one file: three errors in one response.
    "several_semantic_errors.ttl": """@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix own: <https://example.org/{{run}}/many#> .

own:hasPart a owl:ObjectProperty .
own:Engine own:hasPart "a literal value" .
own:size a owl:ObjectProperty , owl:DatatypeProperty .
own:Concept a skos:Concept ;
    skos:prefLabel "First label"@en , "Second label"@en .
""",
    # D57: relative IRIs resolve against the content URL of the new version.
    "relative_iris.ttl": """@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix rel: <#> .

<#Scheme> a skos:ConceptScheme ;
    skos:prefLabel "Relative scheme {{run}}"@en .
rel:Concept a skos:Concept ;
    skos:inScheme <#Scheme> .
""",
    "w3c_namespaces_only.ttl": """@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix own: <https://example.org/{{run}}/w3c#> .

own:Scheme rdf:type skos:ConceptScheme ;
    rdfs:label "W3C only {{run}}"@en .
own:code a owl:DatatypeProperty ;
    rdfs:range xsd:string .
own:Scheme own:code "S1" .
""",
    # D1 and SIMPL-393: a CPSV-like vocabulary uploaded through the generic create operation.
    "cpsv_like.ttl": """@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix cpsv: <https://example.org/{{run}}/cpsv#> .

cpsv:PublicService a owl:Class ;
    rdfs:label "Public Service"@en ;
    rdfs:comment "An activity that a public organisation performs for a citizen or business ({{run}})."@en .
cpsv:Rule a owl:Class ;
    rdfs:label "Rule"@en .
cpsv:follows a owl:ObjectProperty ;
    rdfs:domain cpsv:PublicService ;
    rdfs:range cpsv:Rule .
""",
    "utf8_content.ttl": """@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix own: <https://example.org/{{run}}/utf8#> .

# Content with non-ASCII text, kept byte for byte.
own:Scheme a skos:ConceptScheme ;
    skos:prefLabel "Gesundheitsfürsorge {{run}}"@de , "Santé publique"@fr , "Υγεία"@el .
""",
    # D30: registration checks syntax only, so these semantic errors do not block.
    "external_with_semantic_error.ttl": """@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix ext: <https://ext.example.org/{{run}}/semantic/> .

ext:hasPart a owl:ObjectProperty .
ext:Engine ext:hasPart "a literal value" .
ext:size a owl:ObjectProperty , owl:DatatypeProperty .
""",
    # SIMPL-18184 AC4 in the UI: a new version that references version 1 of 'Base'.
    "reference_to_base_v1.ttl": """@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix own: <https://example.org/{{run}}/user-ui#> .
@prefix base: <{{base}}/content/{{name:Base}}/versions/1#> .

own:Scheme a skos:ConceptScheme ;
    skos:prefLabel "User scheme {{run}}"@en ;
    skos:related base:Scheme .
""",
    # D57 with D12: a relative prefix that names another vocabulary only under the D57 base
    # (<contentBase>/content/<Name>/versions/1), where Ghost<run> does not exist (S7).
    "relative_reference_to_ghost.ttl": """@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix own: <https://example.org/{{run}}/relref#> .
@prefix ghost: <../../{{name:Ghost}}/versions/1#> .

own:Scheme a skos:ConceptScheme ;
    skos:prefLabel "Relative reference {{run}}"@en ;
    skos:related ghost:Thing .
""",
    # D12: a SPARQL-style PREFIX declaration of an unregistered external namespace.
    "sparql_prefix_unregistered.ttl": """PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX own: <https://example.org/{{run}}/sparql#>
PREFIX dct: <{{ext:SparqlTerms}}/>

own:Scheme a skos:ConceptScheme ;
    skos:prefLabel "Sparql style {{run}}"@en ;
    skos:related dct:title .
""",
    "external_syntax_error.ttl": """@prefix ext: <https://ext.example.org/{{run}}/broken/> .
ext:title ext:label @@@ .
""",
}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, text in FILES.items():
        (OUT / name).write_text(text, encoding="utf-8")
    print(f"{len(FILES)} fixtures written to {OUT}")


if __name__ == "__main__":
    main()
