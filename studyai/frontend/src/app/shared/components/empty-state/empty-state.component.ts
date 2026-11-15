import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="empty-state-container fade-in">
      <div class="empty-icon-circle">
        <mat-icon>{{ icon || 'inbox' }}</mat-icon>
      </div>
      <h3 class="empty-title">{{ title }}</h3>
      <p class="empty-message">{{ message }}</p>
      <button
        *ngIf="buttonLabel"
        mat-flat-button
        class="btn-primary empty-button"
        type="button"
        (click)="buttonClick.emit()">
        {{ buttonLabel }}
      </button>
    </div>
  `,
  styles: [`
    .empty-state-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
      padding: 48px 24px;
      margin: 16px auto;
      max-width: 480px;
    }

    .empty-icon-circle {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 72px;
      height: 72px;
      border-radius: 50%;
      background: var(--brand-gradient-subtle);
      color: var(--primary-violet);
      margin-bottom: 20px;

      mat-icon {
        font-size: 36px;
        width: 36px;
        height: 36px;
      }
    }

    .empty-title {
      font-size: 1.25rem;
      font-weight: 600;
      color: var(--text-primary);
      margin-bottom: 8px;
    }

    .empty-message {
      font-size: 0.95rem;
      color: var(--text-secondary);
      line-height: 1.5;
      margin-bottom: 24px;
    }

    .empty-button {
      min-width: 140px;
    }
  `]
})
export class EmptyStateComponent {
  @Input({ required: true }) title!: string;
  @Input({ required: true }) message!: string;
  @Input() icon = 'inbox';
  @Input() buttonLabel?: string;

  @Output() buttonClick = new EventEmitter<void>();
}
