import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const snackBar = inject(MatSnackBar);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let frenchMessage = 'Une erreur inattendue est survenue.';

      if (error.status === 0) {
        frenchMessage = 'Impossible de contacter le serveur. Vérifiez votre connexion.';
      } else if (error.error && typeof error.error === 'object' && error.error.message) {
        frenchMessage = error.error.message;
      } else {
        switch (error.status) {
          case 400:
            frenchMessage = 'Données de requête invalides.';
            break;
          case 401:
            frenchMessage = 'Non autorisé ou session expirée.';
            break;
          case 403:
            frenchMessage = 'Accès interdit ou compte non autorisé.';
            break;
          case 404:
            frenchMessage = 'Ressource introuvable.';
            break;
          case 409:
            frenchMessage = 'Un conflit est survenu avec les données existantes.';
            break;
          case 422:
            frenchMessage = 'Données soumises non conformes.';
            break;
          case 429:
            frenchMessage = 'Trop de tentatives. Veuillez patienter.';
            break;
          case 500:
            frenchMessage = 'Erreur interne du serveur. Veuillez réessayer plus tard.';
            break;
          case 503:
            frenchMessage = 'Service temporairement indisponible.';
            break;
        }
      }

      // Si c'est une erreur 500 ou 0, afficher un toast global pour l'utilisateur
      if (error.status === 0 || error.status >= 500) {
        snackBar.open(frenchMessage, 'Fermer', {
          duration: 5000,
          horizontalPosition: 'end',
          verticalPosition: 'bottom',
          panelClass: ['snack-error']
        });
      }

      return throwError(() => ({
        ...error,
        userMessage: frenchMessage
      }));
    })
  );
};
