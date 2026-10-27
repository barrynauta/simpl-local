import { Parser } from 'n3'

export interface PrecheckIssue {
  detail: string
  location: string
}

const RDF_TYPE = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'
const DECLARATIONS = new Set([
  'http://www.w3.org/2002/07/owl#Ontology',
  'http://www.w3.org/2004/02/skos/core#ConceptScheme',
])

/**
 * Client-side pre-check on file selection (SIMPL-18180 AC1): Turtle syntax and the D16 structure rules.
 * The browser parser stops at the first syntax error; the server check on submit is complete and authoritative.
 */
export function precheck(text: string): PrecheckIssue[] {
  try {
    const quads = new Parser({ baseIRI: 'https://precheck.invalid/' }).parse(text)
    const issues: PrecheckIssue[] = []
    if (quads.length === 0) {
      issues.push({ detail: 'The vocabulary file contains no RDF statements.', location: 'document' })
    }
    if (!quads.some((q) => q.predicate.value === RDF_TYPE && DECLARATIONS.has(q.object.value))) {
      issues.push({
        detail: 'The vocabulary must declare at least one owl:Ontology or skos:ConceptScheme.',
        location: 'document',
      })
    }
    return issues
  } catch (e) {
    const err = e as Error & { context?: { line?: number } }
    const line = err.context?.line ?? /line (\d+)/.exec(err.message)?.[1]
    return [{ detail: `Invalid Turtle syntax: ${err.message}`, location: line ? `line ${line}` : 'document' }]
  }
}

export interface InternalRef {
  name: string
  version?: number
  namespace: string
}

/** Finds references to internal vocabularies, using the D18 convention {base}/{Name}/{n}# or {base}/{Name}#. */
export function internalReferences(text: string, base: string): InternalRef[] {
  const escaped = base.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const re = new RegExp(`${escaped}/([A-Z][A-Za-z0-9]*)(?:/(\\d+))?#`, 'g')
  const seen = new Map<string, InternalRef>()
  for (const m of text.matchAll(re)) {
    const version = m[2] ? Number(m[2]) : undefined
    const key = `${m[1]}:${version ?? ''}`
    if (!seen.has(key)) seen.set(key, { name: m[1], version, namespace: m[0] })
  }
  return [...seen.values()]
}
