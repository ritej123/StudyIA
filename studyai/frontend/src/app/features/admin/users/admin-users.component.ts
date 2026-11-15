import { Component, inject, OnInit, ViewChild, signal, DestroyRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AdminService } from '../../../core/services/admin.service';
import { AdminUser } from '../../../core/models/user.model';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { EditUserDialogComponent } from './edit-user-dialog.component';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatSnackBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    EmptyStateComponent,
    SkeletonLoaderComponent
  ],
  templateUrl: './admin-users.component.html',
  styleUrls: ['./admin-users.component.scss']
})
export class AdminUsersComponent implements OnInit {
  private adminService = inject(AdminService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);

  displayedColumns: string[] = ['avatar', 'fullName', 'email', 'status', 'createdAt', 'actions'];
  dataSource = new MatTableDataSource<AdminUser>([]);

  totalElements = signal<number>(0);
  pageSize = signal<number>(10);
  pageIndex = signal<number>(0);
  isLoading = signal<boolean>(false);

  searchControl = new FormControl('');

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  ngOnInit(): void {
    this.loadStudents();

    this.searchControl.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.pageIndex.set(0);
      this.loadStudents();
    });
  }

  loadStudents(): void {
    this.isLoading.set(true);
    const search = this.searchControl.value || '';
    const page = this.pageIndex();
    const size = this.pageSize();

    this.adminService.getStudents(search, page, size).subscribe({
      next: (response) => {
        this.dataSource.data = response.content || [];
        this.totalElements.set(response.totalElements || 0);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err.status === 404) {
          this.snackBar.open('Compte introuvable', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
        } else {
          this.snackBar.open('Erreur lors du chargement des étudiants', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
        }
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadStudents();
  }

  openEditDialog(user: AdminUser): void {
    const dialogRef = this.dialog.open(EditUserDialogComponent, {
      data: user,
      width: '460px',
      autoFocus: false
    });

    dialogRef.afterClosed().subscribe(result => {
      if (!result) return;

      this.adminService.updateUser(user.id, result).subscribe({
        next: () => {
          this.snackBar.open('Étudiant mis à jour avec succès', 'Fermer', {
            duration: 3500,
            panelClass: ['snack-success']
          });
          this.loadStudents();
        },
        error: (err) => {
          if (err.status === 404) {
            this.snackBar.open('Compte introuvable', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
          } else {
            this.snackBar.open('Échec de la mise à jour de l\'étudiant', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
          }
        }
      });
    });
  }

  toggleStatus(user: AdminUser): void {
    const isCurrentlyActive = user.status === 'ACTIF';
    const newStatus = isCurrentlyActive ? 'DESACTIVE' : 'ACTIF';
    const actionLabel = isCurrentlyActive ? 'désactiver' : 'activer';

    const dialogData: ConfirmDialogData = {
      title: `${isCurrentlyActive ? 'Désactiver' : 'Activer'} le compte`,
      message: `Êtes-vous sûr de vouloir ${actionLabel} le compte de ${user.firstName} ${user.lastName} ?`,
      confirmText: isCurrentlyActive ? 'Désactiver' : 'Activer',
      cancelText: 'Annuler',
      type: isCurrentlyActive ? 'warning' : 'info'
    };

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: dialogData,
      width: '420px'
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;

      this.adminService.updateStatus(user.id, newStatus).subscribe({
        next: () => {
          this.snackBar.open(`Statut mis à jour : compte ${newStatus.toLowerCase()}`, 'Fermer', {
            duration: 3500,
            panelClass: ['snack-success']
          });
          this.loadStudents();
        },
        error: (err) => {
          if (err.status === 404) {
            this.snackBar.open('Compte introuvable', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
          } else {
            this.snackBar.open('Échec du changement de statut', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
          }
        }
      });
    });
  }

  deleteUser(user: AdminUser): void {
    const dialogData: ConfirmDialogData = {
      title: 'Suppression définitive',
      message: `Êtes-vous sûr de vouloir supprimer ${user.firstName} ${user.lastName} ? Cette action est irréversible.`,
      confirmText: 'Supprimer',
      cancelText: 'Annuler',
      type: 'danger'
    };

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: dialogData,
      width: '440px'
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;

      this.adminService.deleteUser(user.id).subscribe({
        next: () => {
          this.snackBar.open('Compte supprimé avec succès', 'Fermer', {
            duration: 3500,
            panelClass: ['snack-success']
          });
          this.loadStudents();
        },
        error: (err) => {
          if (err.status === 404) {
            this.snackBar.open('Compte introuvable', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
          } else {
            this.snackBar.open('Échec de la suppression du compte', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
          }
        }
      });
    });
  }

  clearSearch(): void {
    this.searchControl.setValue('');
  }
}
