import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AdminUser } from '../../../core/models/user.model';

@Component({
  selector: 'app-edit-user-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <div class="edit-dialog-container">
      <div class="dialog-header">
        <div class="header-icon">
          <mat-icon>edit</mat-icon>
        </div>
        <div>
          <h2 mat-dialog-title>Modifier l'étudiant</h2>
          <p class="dialog-subtitle">Mettez à jour les informations de {{ data.firstName }} {{ data.lastName }}</p>
        </div>
      </div>

      <form [formGroup]="editForm" (ngSubmit)="onSave()">
        <mat-dialog-content>
          <div class="form-grid">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Prénom</mat-label>
              <input matInput formControlName="firstName" placeholder="Prénom">
              <mat-error *ngIf="editForm.get('firstName')?.hasError('required')">
                Le prénom est requis
              </mat-error>
              <mat-error *ngIf="editForm.get('firstName')?.hasError('minlength')">
                Minimum 2 caractères
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Nom</mat-label>
              <input matInput formControlName="lastName" placeholder="Nom">
              <mat-error *ngIf="editForm.get('lastName')?.hasError('required')">
                Le nom est requis
              </mat-error>
              <mat-error *ngIf="editForm.get('lastName')?.hasError('minlength')">
                Minimum 2 caractères
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Adresse email</mat-label>
              <input matInput type="email" formControlName="email" placeholder="email@exemple.fr">
              <mat-icon matPrefix>email</mat-icon>
              <mat-error *ngIf="editForm.get('email')?.hasError('required')">
                L'email est requis
              </mat-error>
              <mat-error *ngIf="editForm.get('email')?.hasError('email')">
                Veuillez entrer une adresse email valide
              </mat-error>
            </mat-form-field>
          </div>
        </mat-dialog-content>

        <mat-dialog-actions align="end">
          <button mat-button type="button" (click)="onCancel()">Annuler</button>
          <button mat-flat-button class="btn-primary" type="submit" [disabled]="editForm.invalid">
            Enregistrer les modifications
          </button>
        </mat-dialog-actions>
      </form>
    </div>
  `,
  styles: [`
    .edit-dialog-container {
      padding: 12px 6px;
      min-width: 380px;
    }

    .dialog-header {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 16px;

      .header-icon {
        width: 44px;
        height: 44px;
        border-radius: 12px;
        background: var(--brand-gradient-subtle);
        color: var(--primary-blue);
        display: flex;
        align-items: center;
        justify-content: center;

        mat-icon {
          font-size: 24px;
          width: 24px;
          height: 24px;
        }
      }

      h2 {
        margin: 0;
        font-size: 1.25rem;
        font-weight: 700;
        color: var(--text-primary);
      }

      .dialog-subtitle {
        color: var(--text-secondary);
        font-size: 0.85rem;
        margin: 2px 0 0;
      }
    }

    .form-grid {
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding-top: 8px;
    }

    .full-width {
      width: 100%;
    }

    mat-dialog-actions {
      padding-top: 16px;
      gap: 10px;
    }
  `]
})
export class EditUserDialogComponent {
  private fb = inject(FormBuilder);
  dialogRef = inject(MatDialogRef<EditUserDialogComponent>);

  editForm: FormGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: AdminUser) {
    this.editForm = this.fb.group({
      firstName: [data.firstName, [Validators.required, Validators.minLength(2)]],
      lastName: [data.lastName, [Validators.required, Validators.minLength(2)]],
      email: [data.email, [Validators.required, Validators.email]]
    });
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }

  onSave(): void {
    if (this.editForm.valid) {
      this.dialogRef.close(this.editForm.value);
    }
  }
}
