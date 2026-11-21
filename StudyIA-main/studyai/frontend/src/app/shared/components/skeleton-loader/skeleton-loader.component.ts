import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton-loader',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="skeleton-container" [ngSwitch]="type">
      <!-- Text Skeleton -->
      <ng-container *ngSwitchCase="'text'">
        <div *ngFor="let item of items" class="skeleton-shimmer skeleton-line" [style.width]="getRandomWidth(item)"></div>
      </ng-container>

      <!-- Avatar Skeleton -->
      <ng-container *ngSwitchCase="'avatar'">
        <div class="skeleton-avatar-row" *ngFor="let item of items">
          <div class="skeleton-shimmer skeleton-circle"></div>
          <div class="skeleton-text-col">
            <div class="skeleton-shimmer skeleton-line short"></div>
            <div class="skeleton-shimmer skeleton-line very-short"></div>
          </div>
        </div>
      </ng-container>

      <!-- Card Skeleton -->
      <ng-container *ngSwitchCase="'card'">
        <div class="skeleton-cards-grid">
          <div *ngFor="let item of items" class="skeleton-card">
            <div class="skeleton-shimmer skeleton-card-icon"></div>
            <div class="skeleton-shimmer skeleton-line medium"></div>
            <div class="skeleton-shimmer skeleton-line short"></div>
          </div>
        </div>
      </ng-container>

      <!-- Table Row Skeleton -->
      <ng-container *ngSwitchCase="'table-row'">
        <div class="skeleton-table">
          <div *ngFor="let item of items" class="skeleton-row">
            <div class="skeleton-shimmer skeleton-circle sm"></div>
            <div class="skeleton-shimmer skeleton-cell" style="width: 25%"></div>
            <div class="skeleton-shimmer skeleton-cell" style="width: 30%"></div>
            <div class="skeleton-shimmer skeleton-cell" style="width: 15%"></div>
            <div class="skeleton-shimmer skeleton-cell" style="width: 15%"></div>
          </div>
        </div>
      </ng-container>

      <ng-container *ngSwitchDefault>
        <div *ngFor="let item of items" class="skeleton-shimmer skeleton-line"></div>
      </ng-container>
    </div>
  `,
  styles: [`
    .skeleton-container {
      width: 100%;
    }

    .skeleton-shimmer {
      background: linear-gradient(
        90deg,
        rgba(226, 232, 240, 0.6) 0%,
        rgba(241, 245, 249, 0.9) 50%,
        rgba(226, 232, 240, 0.6) 100%
      );
      background-size: 200% 100%;
      animation: shimmer 1.5s infinite linear;
      border-radius: 6px;
    }

    :host-context([data-theme="dark"]) .skeleton-shimmer {
      background: linear-gradient(
        90deg,
        rgba(30, 41, 59, 0.8) 0%,
        rgba(51, 65, 85, 0.8) 50%,
        rgba(30, 41, 59, 0.8) 100%
      );
      background-size: 200% 100%;
    }

    @keyframes shimmer {
      0% {
        background-position: -200% 0;
      }
      100% {
        background-position: 200% 0;
      }
    }

    .skeleton-line {
      height: 14px;
      margin-bottom: 12px;
      width: 100%;

      &.short {
        width: 60%;
      }

      &.very-short {
        width: 35%;
      }

      &.medium {
        width: 80%;
      }
    }

    .skeleton-circle {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      flex-shrink: 0;

      &.sm {
        width: 36px;
        height: 36px;
      }
    }

    .skeleton-avatar-row {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 16px;

      .skeleton-text-col {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
    }

    .skeleton-cards-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 20px;
    }

    .skeleton-card {
      padding: 20px;
      border-radius: 16px;
      border: 1px solid var(--border-color);
      display: flex;
      flex-direction: column;
      gap: 12px;

      .skeleton-card-icon {
        width: 44px;
        height: 44px;
        border-radius: 12px;
      }
    }

    .skeleton-table {
      display: flex;
      flex-direction: column;
      gap: 14px;
      width: 100%;

      .skeleton-row {
        display: flex;
        align-items: center;
        gap: 16px;
        padding: 12px 0;
        border-bottom: 1px solid var(--border-color);

        .skeleton-cell {
          height: 16px;
        }
      }
    }
  `]
})
export class SkeletonLoaderComponent {
  @Input() type: 'text' | 'avatar' | 'card' | 'table-row' = 'text';
  @Input() count = 3;

  get items(): number[] {
    return Array.from({ length: this.count }, (_, i) => i);
  }

  getRandomWidth(index: number): string {
    const widths = ['100%', '85%', '92%', '70%', '95%'];
    return widths[index % widths.length];
  }
}
