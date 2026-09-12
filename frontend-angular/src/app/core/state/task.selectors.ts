import { createFeatureSelector, createSelector } from '@ngrx/store';
import { TaskState, adapter } from './task.reducer';

export const selectTaskState = createFeatureSelector<TaskState>('tasks');

const {
  selectIds,
  selectEntities,
  selectAll,
  selectTotal,
} = adapter.getSelectors();

export const selectAllTasks = createSelector(
  selectTaskState,
  selectAll
);

export const selectTasksByState = (taskState: string) => createSelector(
  selectAllTasks,
  tasks => tasks.filter(task => task.state === taskState)
);

export const selectTasksLoading = createSelector(
  selectTaskState,
  state => state.loading
);

export const selectTasksError = createSelector(
  selectTaskState,
  state => state.error
);
