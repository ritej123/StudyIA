import { Component, Input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-circular-progress',
  standalone: true,
  imports: [CommonModule],
  template: `
    <svg class="circular-progress-svg" [attr.width]="size" [attr.height]="size" viewBox="0 0 100 100">
      <circle
        class="progress-track"
        cx="50" cy="50" r="42"
        stroke="var(--border-color)"
        stroke-width="8"
        fill="none"
        stroke-linecap="round"/>
      <circle
        class="progress-circle"
        cx="50" cy="50" r="42"
        [attr.stroke]="progressColor()"
        stroke-width="8"
        fill="none"
        stroke-linecap="round"
        [attr.stroke-dasharray]="strokeDasharray()"
        [attr.stroke-dashoffset]="strokeDashoffset()"
        transform="rotate(-90 50 50)"/>
      <g class="progress-center" alignment-baseline="middle" text-anchor="middle">
        <text x="50" y="48" class="progress-value">{{ percentage() }}%</text>
        <text x="50" y="60" class="progress-label">{{ label }}</text>
      </g>
    </svg>
  `,
  styles: [`
    :host {
      display: inline-block;
    }
    .circular-progress-svg {
      width: 100%;
      height: 100%;
      display: block;
    }
    .progress-track {
      opacity: 0.15;
    }
    .progress-circle {
      transition: stroke-dashoffset 0.6s cubic-bezier(0.4, 0, 0.2, 1);
    }
    .progress-center {
      pointer-events: none;
    }
    .progress-value {
      font-size: 16px;
      font-weight: 800;
      fill: var(--text-primary);
      dominant-baseline: middle;
    }
    .progress-label {
      font-size: 10px;
      fill: var(--text-secondary);
      dominant-baseline: middle;
      font-weight: 600;
      letter-spacing: 0.02em;
    }
  `]
})
export class CircularProgressComponent {
  @Input() value = 0;
  @Input() max = 100;
  @Input() size = 80;
  @Input() label = '';

  readonly percentage = computed(() => {
    const pct = Math.round((this.value / this.max) * 100);
    return Math.min(100, Math.max(0, pct));
  });

  readonly circumference = computed(() => 2 * Math.PI * 42);

  readonly strokeDasharray = computed(() => `${this.circumference()} ${this.circumference()}`);

  readonly strokeDashoffset = computed(() => {
    const pct = this.percentage();
    return this.circumference() - (pct / 100) * this.circumference();
  });

  readonly progressColor = computed(() => {
    const pct = this.percentage();
    if (pct >= 75) return 'var(--success)';
    if (pct >= 40) return 'var(--warning)';
    return 'var(--primary-blue)';
  });
}
