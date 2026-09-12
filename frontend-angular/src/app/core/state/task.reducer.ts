import { createReducer, on } from '@ngrx/store';
import { EntityState, EntityAdapter, createEntityAdapter } from '@ngrx/entity';
import { Task } from '../models/task.model';
import * as TaskActions from './task.actions';

export interface TaskState extends EntityState<Task> {
  error: any | null;
  loading: boolean;
}

export const adapter: EntityAdapter<Task> = createEntityAdapter<Task>({
  selectId: (task: Task) => task.id!
});

export const initialState: TaskState = adapter.getInitialState({
  error: null,
  loading: false
});

export const taskReducer = createReducer(
  initialState,
  on(TaskActions.loadTasks, state => ({ ...state, loading: true, error: null })),
  on(TaskActions.loadTasksSuccess, (state, { tasks }) => {
    return adapter.setAll(tasks, { ...state, loading: false });
  }),
  on(TaskActions.loadTasksFailure, (state, { error }) => ({ ...state, loading: false, error })),
  
  on(TaskActions.createTaskSuccess, (state, { task }) => {
    return adapter.addOne(task, state);
  }),
  
  on(TaskActions.updateTaskState, (state, { id, newState }) => {
    // Optimistic update
    return adapter.updateOne({ id, changes: { state: newState } }, state);
  }),
  on(TaskActions.updateTaskStateSuccess, (state, { task }) => {
    // Confirm update with server data if needed, or leave as is
    return adapter.updateOne({ id: task.id!, changes: { ...task } }, state);
  }),
  on(TaskActions.updateTaskStateFailure, (state, { id, originalState, error }) => {
    // Revert on failure
    return adapter.updateOne({ id, changes: { state: originalState } }, { ...state, error });
  })
);
