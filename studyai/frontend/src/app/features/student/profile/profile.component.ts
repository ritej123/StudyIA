import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';

export const passwordChangeMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const newPassword = control.get('newPassword');
  const confirmNewPassword = control.get('confirmNewPassword');
  if (!newPassword || !confirmNewPassword) return null;
  return newPassword.value === confirmNewPassword.value ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatProgressBarModule
  ],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  readonly authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);

  isSavingProfile = signal<boolean>(false);
  isChangingPassword = signal<boolean>(false);
  isUploadingPhoto = signal<boolean>(false);

  profileError = signal<string | null>(null);
  passwordError = signal<string | null>(null);

  hideCurrentPassword = signal<boolean>(true);
  hideNewPassword = signal<boolean>(true);
  hideConfirmNewPassword = signal<boolean>(true);

  previewPhotoUrl = signal<string | null>(null);

  profileForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    email: [{ value: '', disabled: true }]
  });

  securityForm: FormGroup = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmNewPassword: ['', [Validators.required]]
  }, { validators: passwordChangeMatchValidator });

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    const current = this.authService.currentUser();
    if (current) {
      this.profileForm.patchValue({
        firstName: current.firstName,
        lastName: current.lastName,
        email: current.email
      });
      if (current.photoUrl) {
        this.previewPhotoUrl.set(current.photoUrl);
      }
    }

    this.userService.getProfile().subscribe({
      next: (user) => {
        this.profileForm.patchValue({
          firstName: user.firstName,
          lastName: user.lastName,
          email: user.email
        });
        if (user.photoUrl) {
          this.previewPhotoUrl.set(user.photoUrl);
        }
      },
      error: () => {
        // Silencieux si déjà pré-rempli avec authService
      }
    });
  }

  onFileSelected(event: Event): void {
    const target = event.target as HTMLInputElement;
    if (!target.files || target.files.length === 0) return;

    const file = target.files[0];
    const validTypes = ['image/jpeg', 'image/png', 'image/webp'];

    if (!validTypes.includes(file.type)) {
      this.snackBar.open('Format invalide. Choisissez un fichier JPG, PNG ou WebP.', 'Fermer', {
        duration: 4000,
        panelClass: ['snack-error']
      });
      return;
    }

    if (file.size > 2 * 1024 * 1024) {
      this.snackBar.open('La photo ne doit pas dépasser 2 Mo.', 'Fermer', {
        duration: 4000,
        panelClass: ['snack-error']
      });
      return;
    }

    // Prévisualisation immédiate
    const reader = new FileReader();
    reader.onload = () => {
      this.previewPhotoUrl.set(reader.result as string);
    };
    reader.readAsDataURL(file);

    // Upload vers le backend
    this.isUploadingPhoto.set(true);
    this.userService.uploadPhoto(file).subscribe({
      next: (updatedUser) => {
        this.isUploadingPhoto.set(false);
        this.authService.updateCurrentUser({
          photoUrl: updatedUser.photoUrl
        });
        this.snackBar.open('Photo de profil mise à jour avec succès', 'Fermer', {
          duration: 3000,
          panelClass: ['snack-success']
        });
      },
      error: () => {
        this.isUploadingPhoto.set(false);
        this.snackBar.open("Échec du téléversement de la photo, réessayez plus tard.", 'Fermer', {
          duration: 4000,
          panelClass: ['snack-error']
        });
      }
    });
  }

  onUpdateProfile(): void {
    if (this.profileForm.invalid || this.isSavingProfile()) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isSavingProfile.set(true);
    this.profileError.set(null);

    const { firstName, lastName } = this.profileForm.getRawValue();

    this.userService.updateProfile({ firstName, lastName }).subscribe({
      next: (user) => {
        this.isSavingProfile.set(false);
        this.authService.updateCurrentUser({
          firstName: user.firstName,
          lastName: user.lastName
        });
        this.snackBar.open('Profil mis à jour avec succès', 'Fermer', {
          duration: 3000,
          panelClass: ['snack-success']
        });
      },
      error: () => {
        this.isSavingProfile.set(false);
        this.profileError.set('Échec de la mise à jour, réessayez plus tard');
      }
    });
  }

  getPasswordStrength(): { level: number; label: string; color: string } {
    const password = this.securityForm.get('newPassword')?.value || '';
    if (!password) {
      return { level: 0, label: '', color: 'transparent' };
    }

    let score = 0;
    if (password.length >= 8) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;

    switch (score) {
      case 1:
        return { level: 25, label: 'Faible', color: '#ef4444' };
      case 2:
        return { level: 50, label: 'Moyen', color: '#f59e0b' };
      case 3:
        return { level: 75, label: 'Fort', color: '#3b82f6' };
      case 4:
        return { level: 100, label: 'Très fort', color: '#10b981' };
      default:
        return { level: 10, label: 'Trop court', color: '#ef4444' };
    }
  }

  onChangePassword(): void {
    if (this.securityForm.invalid || this.isChangingPassword()) {
      this.securityForm.markAllAsTouched();
      return;
    }

    this.isChangingPassword.set(true);
    this.passwordError.set(null);

    const { currentPassword, newPassword } = this.securityForm.value;

    this.userService.changePassword({ currentPassword, newPassword }).subscribe({
      next: () => {
        this.isChangingPassword.set(false);
        this.securityForm.reset();
        this.snackBar.open('Mot de passe mis à jour avec succès', 'Fermer', {
          duration: 3000,
          panelClass: ['snack-success']
        });
      },
      error: (err) => {
        this.isChangingPassword.set(false);
        if (err.status === 400 && (err.error?.code === 'INVALID_PASSWORD' || err.error?.message?.includes('incorrect'))) {
          this.passwordError.set("L'ancien mot de passe est incorrect");
        } else if (err.error?.message) {
          this.passwordError.set(err.error.message);
        } else {
          this.passwordError.set('Échec de la modification du mot de passe');
        }
      }
    });
  }
}
