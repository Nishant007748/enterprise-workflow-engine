export interface Task {
  id?: number;
  title: string;
  description?: string;
  state: string; // 'TODO', 'IN_PROGRESS', 'DONE'
}
