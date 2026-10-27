export type Status = 'ACTIVE' | 'DEPRECATED'

export interface VocabularyCategory {
  href: string
  contentHref: string
  name: string
  version: number
  description: string
  status: Status
  createdBy?: string
  creationDate: string
  lastUpdateDate: string
}

export interface VocabularyMetadata {
  href: string
  contentHref: string
  latestVersionHref?: string
  name: string
  version: number
  description: string
  changelog: string
  status: Status
  createdBy?: string
  creationDate: string
  lastUpdateDate: string
}

export interface ValidationWarning {
  code: string
  description: string
  location?: string
}

export interface VersionCreated extends VocabularyMetadata {
  validationWarnings: ValidationWarning[]
}

export interface SearchItem {
  href: string
  contentHref: string
  name: string
  version: number
  status: Status
  latest: boolean
}

export interface ProblemIssue {
  type?: string
  title?: string
  detail?: string
  in?: string
  name?: string
  value?: unknown
  location?: string
}

export interface Problem {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  issues?: ProblemIssue[]
  validationWarnings?: ValidationWarning[]
}

export interface Page<T> {
  page: number
  totalPages: number
  total: number
  pageSize: number
  items: T[]
}
