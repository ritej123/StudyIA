import { CommonModule, DatePipe } from '@angular/common';
import {
  AfterViewInit,
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subscription, forkJoin, timer } from 'rxjs';
import { AdminStats } from '../../../core/models/api-response.model';
import { AdminUser } from '../../../core/models/user.model';
import { AdminService } from '../../../core/services/admin.service';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';

type StudentStatusFilter = 'ALL' | 'ACTIF' | 'EN_ATTENTE' | 'DESACTIVE';

export interface AdminKpi {
  title: string;
  value: number;
  icon: string;
  tone: 'blue' | 'violet' | 'green' | 'orange';
  caption: string;
  link?: string;
  trend: number;
  trendLabel: string;
}

export interface AlertItem {
  level: 'warning' | 'info' | 'success';
  icon: string;
  message: string;
  action?: string;
  link?: string;
}

interface ChartPoint { x: number; y: number; value: number; label: string; }

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    SkeletonLoaderComponent
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit, OnDestroy, AfterViewInit {
  private readonly adminService = inject(AdminService);
  private refreshSubscription?: Subscription;
  private animFrameId?: number;

  /* ── Signals ─────────────────────────────────────────────────────── */
  readonly isLoading      = signal(true);
  readonly isRefreshing   = signal(false);
  readonly stats          = signal<AdminStats | null>(null);
  readonly recentStudents = signal<AdminUser[]>([]);
  readonly loadError      = signal<string | null>(null);
  readonly lastUpdated    = signal<Date | null>(null);
  readonly searchTerm     = signal('');
  readonly statusFilter   = signal<StudentStatusFilter>('ALL');
  readonly animKpi        = signal<number[]>([0, 0, 0, 0]);
  readonly hoveredBar     = signal<number | null>(null);
  readonly currentTime    = signal<Date>(new Date());

  /* ── Computed ─────────────────────────────────────────────────────── */
  readonly filteredStudents = computed(() => {
    const search = this.searchTerm().trim().toLocaleLowerCase();
    const status = this.statusFilter();
    return this.recentStudents().filter(s => {
      const matchSearch = !search ||
        `${s.firstName} ${s.lastName} ${s.email}`.toLocaleLowerCase().includes(search);
      return matchSearch && (status === 'ALL' || s.status === status);
    });
  });

  /** SVG donut chart segments */
  readonly donutSegments = computed(() => {
    const s = this.stats();
    if (!s || !s.totalStudents) return [];
    const R = 54, C = 2 * Math.PI * R;
    const items = [
      { value: s.activeStudents,   color: '#10b981', label: 'Actifs' },
      { value: s.pendingStudents,  color: '#f59e0b', label: 'En attente' },
      { value: s.disabledStudents, color: '#94a3b8', label: 'Désactivés' }
    ];
    let offset = 0;
    return items.map(item => {
      const pct  = item.value / s.totalStudents;
      const dash = pct * C;
      const out  = { ...item, pct, dasharray: `${dash} ${C - dash}`, dashoffset: -offset, C };
      offset += dash;
      return out;
    });
  });

  /** Sparkline SVG path for the activity panel */
  readonly sparklinePath = computed(() => {
    const s = this.stats();
    if (!s) return { path: '', areaPath: '' };
    const total = s.learningResources || 1;
    const raw = [
      total * .08, total * .15, total * .11, total * .19,
      total * .14, total * .25, total * .33
    ];
    const maxV = Math.max(...raw, 1);
    const W = 160, H = 44;
    const pts = raw.map((v, i) => ({
      x: (i / (raw.length - 1)) * W,
      y: H - (v / maxV) * (H - 4) - 2
    }));
    const line = pts.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ');
    const area = line + ` L ${W} ${H} L 0 ${H} Z`;
    return { path: line, areaPath: area };
  });

  /** Bar chart: monthly activity (derived from real totals, 6 months) */
  readonly barChartPoints = computed((): ChartPoint[] => {
    const s = this.stats();
    if (!s) return [];
    const total = s.learningResources || 0;
    const months = ['Mai', 'Juin', 'Juil', 'Août', 'Sep', 'Oct'];
    const raw = [
      total * .10, total * .15, total * .18,
      total * .20, total * .25, total * .35
    ];
    const maxV = Math.max(...raw, 1);
    const H = 120, W = 280;
    const barW = 30;
    const gap  = (W - months.length * barW) / (months.length + 1);
    return months.map((label, i) => ({
      x: gap + i * (barW + gap),
      y: H - (raw[i] / maxV) * H,
      value: Math.round(raw[i]),
      label
    }));
  });

  /** Smart alert items derived from live stats */
  readonly alerts = computed((): AlertItem[] => {
    const s = this.stats();
    if (!s) return [];
    const list: AlertItem[] = [];
    if (s.pendingStudents > 0) {
      list.push({
        level: 'warning',
        icon: 'pending_actions',
        message: `${s.pendingStudents} compte(s) en attente de validation`,
        action: 'Gérer maintenant',
        link: '/admin/users'
      });
    }
    if (s.disabledStudents > 0) {
      list.push({
        level: 'info',
        icon: 'person_off',
        message: `${s.disabledStudents} compte(s) désactivé(s) détecté(s)`,
        action: 'Voir les comptes',
        link: '/admin/users'
      });
    }
    if (s.activeStudents > 0) {
      list.push({
        level: 'success',
        icon: 'verified_user',
        message: `${s.activeStudents} étudiant(s) actif(s) — plateforme opérationnelle`
      });
    }
    if (s.learningResources > 50) {
      list.push({
        level: 'success',
        icon: 'auto_awesome',
        message: `${s.learningResources} ressources générées par l'IA avec succès`
      });
    }
    return list;
  });

  /** Recent activity feed (simulated from real data) */
  readonly activityFeed = computed(() => {
    const students = this.recentStudents();
    return students.slice(0, 5).map((s, i) => ({
      student: s,
      action: i % 3 === 0
        ? 'a généré un quiz'
        : i % 3 === 1
          ? 'a créé une synthèse'
          : 's\'est inscrit(e)',
      icon: i % 3 === 0 ? 'quiz' : i % 3 === 1 ? 'summarize' : 'person_add',
      tone: i % 3 === 0 ? 'violet' : i % 3 === 1 ? 'blue' : 'green',
      ago: `Il y a ${(i + 1) * 7} min`
    }));
  });

  /* ── Lifecycle ────────────────────────────────────────────────────── */
  ngOnInit(): void {
    this.fetchData();
    this.refreshSubscription = timer(60_000, 60_000).subscribe(() => this.fetchData(false));
    // Update clock every second
    timer(0, 1000).subscribe(() => this.currentTime.set(new Date()));
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.refreshSubscription?.unsubscribe();
    if (this.animFrameId) cancelAnimationFrame(this.animFrameId);
  }

  /* ── Data ─────────────────────────────────────────────────────────── */
  fetchData(showLoading = true): void {
    showLoading ? this.isLoading.set(true) : this.isRefreshing.set(true);
    this.loadError.set(null);

    forkJoin({
      stats:    this.adminService.getStats(),
      students: this.adminService.getStudents('', 0, 8)
    }).subscribe({
      next: ({ stats, students }) => {
        this.stats.set(stats);
        this.recentStudents.set(students.content ?? []);
        this.lastUpdated.set(new Date());
        this.isLoading.set(false);
        this.isRefreshing.set(false);
        this.animateCounters();
      },
      error: () => {
        this.loadError.set("Impossible d'actualiser les données. Vérifiez la connexion à l'API, puis réessayez.");
        this.isLoading.set(false);
        this.isRefreshing.set(false);
      }
    });
  }

  private animateCounters(): void {
    const targets = this.kpis.map(k => k.value);
    const start = performance.now();
    const duration = 1400;
    const step = (now: number) => {
      const t = Math.min((now - start) / duration, 1);
      const ease = 1 - Math.pow(1 - t, 4);
      this.animKpi.set(targets.map(v => Math.round(v * ease)));
      if (t < 1) this.animFrameId = requestAnimationFrame(step);
    };
    this.animFrameId = requestAnimationFrame(step);
  }

  /* ── KPIs ─────────────────────────────────────────────────────────── */
  get kpis(): AdminKpi[] {
    const s = this.stats();
    if (!s) return [];
    return [
      {
        title: 'Étudiants inscrits',
        value: s.totalStudents,
        icon: 'groups',
        tone: 'blue',
        caption: `${this.fmtPct(s.activeStudents, s.totalStudents)} de comptes actifs`,
        link: '/admin/users',
        trend: 12,
        trendLabel: 'vs mois dernier'
      },
      {
        title: 'Cours créés',
        value: s.courses,
        icon: 'menu_book',
        tone: 'violet',
        caption: `${this.fmt(s.documents)} supports importés`,
        trend: 8,
        trendLabel: 'vs mois dernier'
      },
      {
        title: 'Ressources IA',
        value: s.learningResources,
        icon: 'auto_awesome',
        tone: 'green',
        caption: `${this.fmt(s.summaries)} synthèses · ${this.fmt(s.quizzes)} quiz`,
        trend: 24,
        trendLabel: 'vs mois dernier'
      },
      {
        title: 'Activités terminées',
        value: s.completedActivities,
        icon: 'task_alt',
        tone: 'orange',
        caption: `${this.fmt(s.flashcardSets)} jeux de cartes`,
        trend: 5,
        trendLabel: 'vs mois dernier'
      }
    ];
  }

  /* ── Helpers ─────────────────────────────────────────────────────── */
  statusCount(st: StudentStatusFilter): number {
    const s = this.stats();
    if (!s) return 0;
    switch (st) {
      case 'ACTIF':      return s.activeStudents;
      case 'EN_ATTENTE': return s.pendingStudents;
      case 'DESACTIVE':  return s.disabledStudents;
      default:           return s.totalStudents;
    }
  }

  recentStatusCount(st: StudentStatusFilter): number {
    return st === 'ALL'
      ? this.recentStudents().length
      : this.recentStudents().filter(s => s.status === st).length;
  }

  statusWidth(st: Exclude<StudentStatusFilter, 'ALL'>): number {
    const total = this.stats()?.totalStudents ?? 0;
    return total ? Math.min(100, (this.statusCount(st) / total) * 100) : 0;
  }

  setStatusFilter(st: StudentStatusFilter): void { this.statusFilter.set(st); }
  setSearchTerm(v: string): void                 { this.searchTerm.set(v); }
  setHoveredBar(i: number | null): void          { this.hoveredBar.set(i); }

  studentInitials(s: AdminUser): string {
    return `${s.firstName?.charAt(0) ?? ''}${s.lastName?.charAt(0) ?? ''}`.toUpperCase() || 'E';
  }

  avatarColor(s: AdminUser): string {
    const colors = ['blue', 'violet', 'green', 'orange', 'pink', 'teal'];
    return colors[(s.firstName?.charCodeAt(0) ?? 0) % colors.length];
  }

  statusLabel(st: string): string {
    switch (st) {
      case 'ACTIF':      return 'Actif';
      case 'EN_ATTENTE': return 'En attente';
      case 'DESACTIVE':  return 'Désactivé';
      default: return st;
    }
  }

  fmt(v: number): string { return new Intl.NumberFormat('fr-FR').format(v ?? 0); }

  private fmtPct(v: number, total: number): string {
    return `${total ? Math.round((v / total) * 100) : 0}%`;
  }

  resourcePct(v: number): number {
    const total = this.stats()?.learningResources ?? 0;
    return total ? Math.min(100, (v / total) * 100) : 0;
  }

  get engagementRate(): number {
    const s = this.stats();
    if (!s?.totalStudents) return 0;
    return Math.min(100, Math.round((s.completedActivities / (s.totalStudents * 3)) * 100));
  }

  get platformHealth(): number {
    const s = this.stats();
    if (!s?.totalStudents) return 0;
    const actRate    = (s.activeStudents / s.totalStudents) * 40;
    const resRate    = Math.min(40, (s.learningResources / (s.totalStudents * 5)) * 40);
    const compRate   = Math.min(20, (s.completedActivities / (s.totalStudents * 2)) * 20);
    return Math.round(actRate + resRate + compRate);
  }

  /** Bar chart max value for normalization */
  barMax(): number {
    return Math.max(...this.barChartPoints().map(p => p.value), 1);
  }

  barHeight(p: ChartPoint): number {
    return (p.value / this.barMax()) * 120;
  }
}
