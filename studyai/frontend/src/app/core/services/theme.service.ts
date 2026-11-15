import { Injectable, signal, effect } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  isDarkMode = signal<boolean>(this.loadTheme());

  constructor() {
    effect(() => {
      const dark = this.isDarkMode();
      document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
      document.body.classList.toggle('dark-theme', dark);
      localStorage.setItem('studyai-theme', dark ? 'dark' : 'light');
    });
  }

  toggleTheme(): void {
    this.isDarkMode.update(v => !v);
  }

  private loadTheme(): boolean {
    const saved = localStorage.getItem('studyai-theme');
    if (saved) return saved === 'dark';
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }
}
