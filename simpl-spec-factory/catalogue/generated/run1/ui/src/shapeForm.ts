import { Parser, type Quad } from 'n3';

const SH = 'http://www.w3.org/ns/shacl#';
const RDF_TYPE = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type';
const SIMPL = 'http://w3id.org/gaia-x/simpl#';
const XSD = 'http://www.w3.org/2001/XMLSchema#';
const NUMERIC = new Set([`${XSD}decimal`, `${XSD}integer`, `${XSD}double`]);

export interface FormField { property: string; numeric: boolean }
export interface FormSection { nodeType: string; nodeLocalName: string; fields: FormField[] }

const local = (iri: string) => iri.split(/[/#:]/).pop() as string;
const compact = (iri: string) => (iri.startsWith(SIMPL) ? `simpl:${iri.slice(SIMPL.length)}` : iri);

/**
 * D74: sections from the top-level shape's sh:property entries that have sh:node; one section per referenced
 * node shape, keyed by its sh:targetClass; one field per sh:property path local name.
 */
export function buildForm(turtle: string): FormSection[] {
  const quads: Quad[] = new Parser().parse(turtle);
  const objects = (s: string, p: string) => quads.filter((q) => q.subject.value === s && q.predicate.value === p).map((q) => q.object.value);
  const nodeShapes = quads.filter((q) => q.predicate.value === RDF_TYPE && q.object.value === `${SH}NodeShape`).map((q) => q.subject.value);
  const referenced = new Set(quads.filter((q) => q.predicate.value === `${SH}node`).map((q) => q.object.value));
  const top = nodeShapes.find((s) => !referenced.has(s));
  if (!top) return [];
  const sections: FormSection[] = [];
  for (const prop of objects(top, `${SH}property`)) {
    for (const node of objects(prop, `${SH}node`)) {
      const target = objects(node, `${SH}targetClass`)[0];
      if (!target) continue;
      const fields: FormField[] = objects(node, `${SH}property`).flatMap((p) => {
        const path = objects(p, `${SH}path`)[0];
        if (!path) return [];
        const dt = objects(p, `${SH}datatype`)[0];
        return [{ property: local(path), numeric: !!dt && NUMERIC.has(dt) }];
      });
      sections.push({ nodeType: compact(target), nodeLocalName: local(target), fields });
    }
  }
  return sections;
}

export interface FieldValue { text: string; op1: string; value1: string; op2: string; value2: string }

export function emptyValue(): FieldValue {
  return { text: '', op1: '=', value1: '', op2: '', value2: '' };
}

/** Only filled fields are sent, in the body format of D29. */
export function buildCriteria(sections: FormSection[], values: Record<string, FieldValue>): Record<string, unknown> {
  const body: Record<string, unknown> = {};
  for (const s of sections) {
    const node: Record<string, unknown> = {};
    for (const f of s.fields) {
      const v = values[`${s.nodeLocalName}-${f.property}`];
      if (!v) continue;
      if (f.numeric) {
        if (v.value1.trim() === '') continue;
        const range: Record<string, unknown> = { op1: v.op1 || '=', value1: Number(v.value1) };
        if (v.op2 && v.value2.trim() !== '') {
          range.op2 = v.op2;
          range.value2 = Number(v.value2);
        }
        node[f.property] = range;
      } else if (v.text.trim() !== '') {
        node[f.property] = v.text.trim();
      }
    }
    if (Object.keys(node).length > 0) body[s.nodeType] = node;
  }
  return body;
}
