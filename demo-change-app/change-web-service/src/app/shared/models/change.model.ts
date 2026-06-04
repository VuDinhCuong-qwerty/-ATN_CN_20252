export type ChangeStatus    = 'DRAFT' | 'PENDING' | 'APPROVED' | 'EXECUTING' | 'SUCCESS' | 'FAIL';
export type JobType         = 'MERGE' | 'BUILD' | 'DEPLOY';
export type Phase           = 'PRE' | 'DURING' | 'ROLLBACK';
export type MemberRole      = 'DEV' | 'TESTER' | 'BA';
export type ApproverStatus  = 'PENDING' | 'APPROVED' | 'REJECTED';
export type TaskStatus      = 'READY' | 'SUCCESS' | 'FAIL';

export interface GoliveJob {
  id?:       number;
  name:      string;
  link?:     string;
  jobType:   JobType;
  orderNum:  number;
  status?:   number; // update only: 0 = soft-delete
}

export interface ChecklistItem {
  id?:            number;
  phase:          Phase;
  stepText:       string;
  orderNum:       number;
  assignedTo?:    string;
  assignedToCode?: string;
  taskStatus?:    TaskStatus; // READY/SUCCESS/FAIL — updated separately via /checklist/{id}/status
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
  approveStatus?: ApproverStatus; // API field name
  note?:         string;
  decidedAt?:    string;
  status?:       number;
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
}

// UserSummaryResponse.content item from /api/users/search
export interface UserSearchResult {
  userId:       number;
  username:     string;
  fullName:     string;
  employeeCode: string;
  status?:      string;
}
