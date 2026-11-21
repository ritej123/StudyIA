import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  type?: 'danger' | 'warning' | 'info';
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="confirm-dialog" [ngClass]="data.type || 'info'">
      <div class="dialog-header">
        <div class="icon-wrapper">
          <mat-icon>{{ getIcon() }}</mat-icon>
        </div>
        <h2 mat-dialog-title>{{ data.title }}</h2>
      </div>

      <mat-dialog-content>
        <p class="dialog-message">{{ data.message }}</p>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button type="button" (click)="onCancel()">
          {{ data.cancelText || 'Annuler' }}
        </button>
        <button
          mat-flat-button
          [color]="data.type === 'danger' ? 'warn' : 'primary'"
          [ngClass]="data.type === 'danger' ? 'btn-danger-confirm' : 'btn-primary-confirm'"
          type="button"
          (click)="onConfirm()">
          {{ data.confirmText || 'Confirmer' }}
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .confirm-dialog {
      padding: 8px 4px;
    }

    .dialog-header {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 8px;

      h2 {
        margin: 0;
        font-size: 1.25rem;
        font-weight: 600;
        color: var(--text-primary);
      }
    }

    .icon-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 44px;
      height: 44px;
      border-radius: 12px;

      mat-icon {
        font-size: 24px;
        width: 24px;
        height: 24px;
      }
    }

    .danger .icon-wrapper {
      background-color: var(--danger-bg);
      color: var(--danger);
    }

    .warning .icon-wrapper {
      background-color: var(--warning-bg);
      color: var(--warning);
    }

    .info .icon-wrapper {
      background-color: var(--info-bg);
      color: var(--info);
    }

    .dialog-message {
      color: var(--text-secondary);
      font-size: 0.95rem;
      line-height: 1.5;
      margin: 8px 0;
    }

    mat-dialog-actions {
      padding-top: 16px;
      gap: 12px;

      button {
        border-radius: 8px;
        font-weight: 500;
        min-width: 90px;
      }
    }

    .btn-danger-confirm {
      background-color: var(--danger) !important;
      color: #ffffff !important;
    }

    .btn-primary-confirm {
      background: var(--brand-gradient) !important;
      color: #ffffff !important;
    }
  `]
})
export class ConfirmDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
  ) {}

  getIcon(): string {
    switch (this.data.type) {
      case 'danger':
        return 'delete_outline';
      case 'warning':
        return 'warning_amber';
      case 'info':
      default:
        return 'info_outline';
    }
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onConfirm(): void {
    this.dialogRef.close(true);
  }
}
