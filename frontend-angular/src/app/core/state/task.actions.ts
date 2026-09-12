import { createAction, props } from '@ngrx/store';
import { Task } from '../models/task.model';

export const loadTasks = createAction('[Task List] Load Tasks');
export const loadTasksSuccess = createAction(
  '[Task List] Load Tasks Success',
  props<{ tasks: Task[] }>()
);
export const loadTasksFailure = createAction(
  '[Task List] Load Tasks Failure',
  props<{ error: any }>()
);

export const createTask = createAction(
  '[Task List] Create Task',
  props<{ task: Task }>()
);
export const createTaskSuccess = createAction(
  '[Task List] Create Task Success',
  props<{ task: Task }>()
);
export const createTaskFailure = createAction(
  '[Task List] Create Task Failure',
  props<{ error: any }>()
);

export const updateTaskState = createAction(
  '[Task List] Update Task State',
  props<{ id: number; newState: string }>()
);
export const updateTaskStateSuccess = createAction(
  '[Task List] Update Task State Success',
  props<{ task: Task }>()
);
export const updateTaskStateFailure = createAction(
  '[Task List] Update Task State Failure',
  props<{ error: any; originalState: string; id: number }>() // Revert info
);
