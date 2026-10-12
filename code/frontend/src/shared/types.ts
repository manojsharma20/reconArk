// TypeScript copies of the backend records (backend/common/.../model and the service responses).

export type Side = 'LEFT' | 'RIGHT';
export type MatchStatus = 'MATCHED' | 'MISMATCHED' | 'ONLY_IN_LEFT' | 'ONLY_IN_RIGHT';
export type RunStatus = 'LOADING' | 'MATCHING' | 'COMPLETED' | 'FAILED';

export interface SourceSpec {
  name: string;
  format: string;
  formatOptions: Record<string, string>;
  fieldMapping: Record<string, string>;
}

export interface CompareRule {
  field: string;
  comparator: string;
  options: Record<string, string>;
}

export interface ReconDefinition {
  id: string | null;
  name: string;
  left: SourceSpec;
  right: SourceSpec;
  matchFields: string[];
  compareRules: CompareRule[];
}

export interface PluginInfo {
  kind: string;
  id: string;
  description: string;
}

export interface UploadedFile {
  id: string;
  definitionId: string;
  side: Side;
  originalName: string;
  storageKey: string;
  sizeBytes: number;
  sha256: string;
  uploadedAt: string;
}

export interface Run {
  id: string;
  definitionId: string;
  definitionName: string;
  leftFileId: string;
  rightFileId: string;
  status: RunStatus;
  bucketCount: number;
  leftLoaded: number;
  rightLoaded: number;
  rejected: number;
  matched: number;
  mismatched: number;
  onlyInLeft: number;
  onlyInRight: number;
  error: string | null;
  createdAt: string;
  finishedAt: string | null;
}

export interface Job {
  id: string;
  type: 'LOAD' | 'MATCH';
  side: Side | null;
  bucket: number | null;
  status: 'QUEUED' | 'DONE' | 'FAILED';
  attempts: number;
  recordsRead: number;
  recordsRejected: number;
  resultsWritten: number;
  workerId: string | null;
  error: string | null;
}

export interface Difference {
  field: string;
  leftValue: string | null;
  rightValue: string | null;
  comparator: string;
}

export interface ResultRow {
  matchKey: string;
  status: MatchStatus;
  left: Record<string, string> | null;
  right: Record<string, string> | null;
  differences: Difference[];
}

export interface RunSummary {
  runId: string;
  definitionName: string;
  status: RunStatus;
  leftLoaded: number;
  rightLoaded: number;
  rejected: number;
  matched: number;
  mismatched: number;
  onlyInLeft: number;
  onlyInRight: number;
  matchRatePercent: number;
  fields: string[];
  createdAt: string;
  finishedAt: string | null;
}

export interface ResultPage {
  page: number;
  size: number;
  total: number;
  items: ResultRow[];
}
