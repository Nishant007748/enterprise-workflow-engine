import { Component } from '@angular/core';
import { KanbanComponent } from './features/kanban/kanban.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [KanbanComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'frontend-angular';
}
