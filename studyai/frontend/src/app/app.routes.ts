import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { noAuthGuard } from './core/guards/no-auth.guard';
import { DocumentsComponent } from './pages/documents/documents.component';

export const routes: Routes = [
  { path: '', redirectTo: '/auth/login', pathMatch: 'full' },
  {
    path: 'auth',
    canActivate: [noAuthGuard],
    children: [
      { path: '', redirectTo: 'login', pathMatch: 'full' },
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
      },
      {
        path: 'register',
        loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
      }
    ]
  },
  {
    path: 'student',
    loadComponent: () => import('./layout/student-layout/student-layout.component').then(m => m.StudentLayoutComponent),
    canActivate: [authGuard, roleGuard],
    data: { role: 'ETUDIANT' },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/student/dashboard/student-dashboard.component').then(m => m.StudentDashboardComponent)
      },
      {
        path: 'learning',
        loadComponent: () => import('./features/student/learning/learning.component').then(m => m.LearningComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/student/profile/profile.component').then(m => m.ProfileComponent)
      }
    ]
  },
  {
    path: 'admin',
    loadComponent: () => import('./layout/admin-layout/admin-layout.component').then(m => m.AdminLayoutComponent),
    canActivate: [authGuard, roleGuard],
    data: { role: 'ADMIN' },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/admin/dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent)
      },
      {
        path: 'users',
        loadComponent: () => import('./features/admin/users/admin-users.component').then(m => m.AdminUsersComponent)
      }
    ]
  },
  {
    path: 'documents',
    component: DocumentsComponent,
    canActivate: [authGuard]
  },
  { path: '**', redirectTo: '/auth/login' }
];
