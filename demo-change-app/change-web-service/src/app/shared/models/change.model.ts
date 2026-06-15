export type ChangeStatus    = 'DRAFT' | 'PENDING' | 'APPROVED' | 'EXECUTING' | 'SUCCESS' | 'FAIL';
export type JobType         = 'MERGE' | 'BUILD' | 'DEPLOY';
export type JobStatus       = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAIL';
export type Phase           = 'PRE' | 'DURING' | 'ROLLBACK';
export type MemberRole      = 'DEV' | 'TESTER' | 'BA';
export type ApproverStatus  = 'PENDING' | 'APPROVED' | 'REJECTED';
export type TaskStatus      = 'READY' | 'SUCCESS' | 'FAIL';
export type DocType         = 'GIT' | 'JIRA' | 'CONFLUENCE' | 'BUILD' | 'OTHER';

export interface GoliveJob {
  id?:          number;
  name:         string;
  link?:        string;
  jobType:      JobType;
  orderNum:     number;
  jobStatus?:   JobStatus;
  startedAt?:   string;
  completedAt?: string;
  resultNote?:  string;
  status?:      number; // update only: 0 = soft-delete
}

export interface ChecklistItem {
  id?:            number;
  phase:          Phase;
  stepText:       string;
  orderNum:       number;
  assignedTo?:    string;
  assignedToCode?: string;
  taskStatus?:    TaskStatus;
  status?:        number;
}

export interface TeamMember {
  id?:          number;
  userId?:      string;
  username:     string;
  fullName:     string;
  employeeCode?: string;
  memberRole:   MemberRole;
  isLead:       number;
  status?:      number;
}

export interface Approver {
  id?:           number;
  userId?:       string;
  username:      string;
  fullName:      string;
  employeeCode:  string;
  approveStatus?: ApproverStatus;
  note?:         string;
  decidedAt?:    string;
  status?:       number;
}

export interface Document {
  id?:            number;
  changeRequestId?: number;
  docType:        DocType;
  title:          string;
  url?:           string;
  note?:          string;
  createdBy?:     string;
  createdByCode?: string;
  createdAt?:     string;
}

export interface AuditLogEntry {
  id?:             number;
  action:          string;
  fromStatus?:     string;
  toStatus?:       string;
  performedBy?:    string;
  performedByCode?: string;
  note?:           string;
  createdAt?:      string;
}

export interface ChangeRequest {
  id?:           number;
  changeId?:     string;
  changeName:    string;
  content?:      string;
  gitLink?:      string;
  goliveAt?:     string;
  status:        ChangeStatus;
  createdBy?:    string;
  createdByCode?: string;
  createdAt?:    string;
  updatedAt?:    string;
  jobs?:         GoliveJob[];
  checklistItems?: ChecklistItem[];
  teamMembers?:  TeamMember[];
  approvers?:    Approver[];
  documents?:    Document[];
  auditLogs?:    AuditLogEntry[];
}

export interface DashboardStats {
  totalThisMonth: number;
  pendingApproval: number;
  executing: number;
  successRate: number;
  recentChanges: ChangeListItem[];
}

export interface ChangeListItem {
  id?: number;
  changeId?: string;
  changeName: string;
  status: ChangeStatus;
  goliveAt?: string;
  createdBy?: string;
  createdByCode?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ReportSummary {
  byStatus: Record<string, number>;
  successRate: number;
  topCreators: Array<{ createdBy: string; createdByCode: string; count: number }>;
  totalAll: number;
}

// UserSummaryResponse.content item from /api/users/search
export interface UserSearchResult {
  userId:       number;
  username:     string;
  fullName:     string;
  employeeCode: string;
  status?:      string;
}
