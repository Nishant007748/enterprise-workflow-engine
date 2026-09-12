import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { Task } from '../../core/models/task.model';
import * as TaskActions from '../../core/state/task.actions';
import { selectTasksByState } from '../../core/state/task.selectors';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-kanban',
  standalone: true,
  imports: [CommonModule, DragDropModule, FormsModule],
  templateUrl: './kanban.component.html',
  styleUrls: ['./kanban.component.css']
})
export class KanbanComponent implements OnInit {
  private store = inject(Store);

  todoTasks$: Observable<Task[]> = this.store.select(selectTasksByState('TODO'));
  inProgressTasks$: Observable<Task[]> = this.store.select(selectTasksByState('IN_PROGRESS'));
  doneTasks$: Observable<Task[]> = this.store.select(selectTasksByState('DONE'));

  newTaskTitle = '';

  ngOnInit(): void {
    this.store.dispatch(TaskActions.loadTasks());
  }

  drop(event: CdkDragDrop<Task[] | null>, newState: string) {
    if (event.previousContainer === event.container) {
      // In a real app we might update order, but skipping for now
      if (event.container.data) {
        moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      }
    } else {
      if (event.previousContainer.data && event.container.data) {
        const task = event.previousContainer.data[event.previousIndex];
        // Dispatch NgRx action for optimistic update
        this.store.dispatch(TaskActions.updateTaskState({ id: task.id!, newState }));
      }
    }
  }

  addTask() {
    if (this.newTaskTitle.trim()) {
      const task: Task = {
        title: this.newTaskTitle,
        state: 'TODO'
      };
      this.store.dispatch(TaskActions.createTask({ task }));
      this.newTaskTitle = '';
    }
  }
}
