import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, mergeMap, switchMap } from 'rxjs/operators';
import { TaskService } from '../services/task.service';
import * as TaskActions from './task.actions';

@Injectable()
export class TaskEffects {
  private actions$ = inject(Actions);
  private taskService = inject(TaskService);

  loadTasks$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TaskActions.loadTasks),
      switchMap(() =>
        this.taskService.getTasks().pipe(
          map(tasks => TaskActions.loadTasksSuccess({ tasks })),
          catchError(error => of(TaskActions.loadTasksFailure({ error })))
        )
      )
    )
  );

  createTask$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TaskActions.createTask),
      mergeMap(({ task }) =>
        this.taskService.createTask(task).pipe(
          map(createdTask => TaskActions.createTaskSuccess({ task: createdTask })),
          catchError(error => of(TaskActions.createTaskFailure({ error })))
        )
      )
    )
  );

  updateTaskState$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TaskActions.updateTaskState),
      mergeMap(({ id, newState }) => {
        // Find a way to pass originalState if optimistic update fails
        // For simplicity, we assume we know original state from UI or store
        return this.taskService.updateTaskState(id, newState).pipe(
          map(updatedTask => TaskActions.updateTaskStateSuccess({ task: updatedTask })),
          catchError(error => of(TaskActions.updateTaskStateFailure({ error, id, originalState: 'TODO' }))) // simplified
        )
      })
    )
  );
}
