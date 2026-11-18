// Wire schemas of the Vocabulary Management API (names as in the OpenAPI, terms as in the glossary).

export type VocabularyStatus = 'ACTIVE' | 'DEPRECATED';

/** VocabularyCategory: the vocabulary with values of its latest version. */
export interface Vocabulary {
  href: string;
  contentHref: string;
  name: string;
  version: number;
  description: string;
  status: VocabularyStatus;
  createdBy: string;
  creationDate: string;
  lastUpdateDate: string;
}

/** VocabularyMetadata: one vocabulary version. */
export interface VocabularyVersion {
  href: string;
  contentHref: string;
  latestVersionHref: string;
  name: string;
  version: number;
  description: string;
  changelog: string;
  status: VocabularyStatus;
  createdBy: string;
  creationDate: string;
  lastUpdateDate: string;
}

export interface ValidationWarning {
  code: string;
  description: string;
  location: string;
}

export interface CreatedVersion extends VocabularyVersion {
  validationWarnings: ValidationWarning[];
}

export interface CollectionPage<T> {
  page: number;
  totalPages: number;
  total: number;
  pageSize: number;
  items: T[];
}

export interface ProblemIssue {
  detail?: string;
  in?: string;
  name?: string;
  location?: string;
}

export interface Problem {
  status: number;
  title?: string;
  detail?: string;
  issues?: ProblemIssue[];
}

/** One entry of a row's three-dot actions menu. */
export interface MenuAction {
  label: string;
  run: () => void;
}
