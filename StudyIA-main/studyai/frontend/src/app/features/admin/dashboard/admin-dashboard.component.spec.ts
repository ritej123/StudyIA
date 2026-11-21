import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { CommonModule, DatePipe, registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError, Subject } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AdminDashboardComponent } from './admin-dashboard.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { AdminService } from '../../../core/services/admin.service';
import { AdminStats, PagedResponse } from '../../../core/models/api-response.model';
import { AdminUser } from '../../../core/models/user.model';

registerLocaleData(localeFr);

let rafCallbacks: Array<(time: number) => void> = [];

class MockAdminService {
  getStats = jasmine.createSpy('getStats').and.returnValue(of(mockStats));
  getStudents = jasmine.createSpy('getStudents').and.returnValue(of(mockPagedResponse));
}

const mockStats: AdminStats = {
  totalStudents: 100,
  activeStudents: 70,
  pendingStudents: 20,
  disabledStudents: 10,
  courses: 25,
  documents: 60,
  learningResources: 95,
  summaries: 40,
  quizzes: 35,
  flashcardSets: 20,
  completedActivities: 150
};

const mockStudent: AdminUser = {
  id: 1,
  firstName: 'Sophie',
  lastName: 'Dupont',
  email: 'sophie.dupont@example.com',
  role: 'ETUDIANT',
  status: 'ACTIF',
  photoUrl: undefined,
  createdAt: '2026-01-10T08:30:00Z',
  updatedAt: '2026-01-10T08:30:00Z'
};

const mockPagedResponse: PagedResponse<AdminUser> = {
  content: [mockStudent],
  pageNumber: 0,
  pageSize: 8,
  totalElements: 1,
  totalPages: 1,
  last: true
};

function loadData(c: AdminDashboardComponent): void {
  c.stats.set(mockStats);
  c.recentStudents.set([mockStudent]);
  c.lastUpdated.set(new Date());
  c.isLoading.set(false);
  c.isRefreshing.set(false);
  c.loadError.set(null);
}

describe('AdminDashboardComponent', () => {
  let component: AdminDashboardComponent;
  let fixture: ComponentFixture<AdminDashboardComponent>;
  let adminService: MockAdminService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        AdminDashboardComponent,
        CommonModule,
        RouterTestingModule,
        MatButtonModule,
        MatIconModule,
        MatTooltipModule,
        SkeletonLoaderComponent
      ],
      providers: [
        DatePipe,
        { provide: AdminService, useClass: MockAdminService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboardComponent);
    component = fixture.componentInstance;
    adminService = TestBed.inject(AdminService) as unknown as MockAdminService;

    rafCallbacks = [];
    spyOn(window, 'requestAnimationFrame').and.callFake((cb: (time: number) => void) => {
      rafCallbacks.push(cb);
      return rafCallbacks.length;
    });
    spyOn(window, 'cancelAnimationFrame').and.stub();
  });

  describe('initial state', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should start in loading state with empty stats', () => {
      expect(component.isLoading()).toBeTrue();
      expect(component.stats()).toBeNull();
      expect(component.recentStudents()).toEqual([]);
      expect(component.loadError()).toBeNull();
    });

    it('should call getStats and getStudents on init', () => {
      fixture.detectChanges();
      expect(adminService.getStats).toHaveBeenCalled();
      expect(adminService.getStudents).toHaveBeenCalledWith('', 0, 8);
    });

    it('should populate signals after data load', () => {
      fixture.detectChanges();
      expect(component.isLoading()).toBeFalse();
      expect(component.stats()).toEqual(mockStats);
      expect(component.recentStudents()).toEqual([mockStudent]);
      expect(component.loadError()).toBeNull();
      expect(component.lastUpdated()).not.toBeNull();
    });
  });

  describe('fetchData', () => {
    it('should set isLoading true and loadError null when fetching', () => {
      const statsSubject = new Subject<AdminStats>();
      const studentsSubject = new Subject<PagedResponse<AdminUser>>();
      adminService.getStats.and.returnValue(statsSubject.asObservable());
      adminService.getStudents.and.returnValue(studentsSubject.asObservable());

      component.loadError.set('old error');
      component.isLoading.set(false);

      component.fetchData();

      expect(component.isLoading()).toBeTrue();
      expect(component.loadError()).toBeNull();

      statsSubject.next(mockStats);
      statsSubject.complete();
      studentsSubject.next(mockPagedResponse);
      studentsSubject.complete();
    });

    it('should set isRefreshing true when showLoading is false', () => {
      const statsSubject = new Subject<AdminStats>();
      const studentsSubject = new Subject<PagedResponse<AdminUser>>();
      adminService.getStats.and.returnValue(statsSubject.asObservable());
      adminService.getStudents.and.returnValue(studentsSubject.asObservable());

      component.fetchData(false);
      expect(component.isRefreshing()).toBeTrue();

      statsSubject.next(mockStats);
      statsSubject.complete();
      studentsSubject.next(mockPagedResponse);
      studentsSubject.complete();
      expect(component.isRefreshing()).toBeFalse();
    });

    it('should update lastUpdated and signals on success', fakeAsync(() => {
      component.fetchData();
      tick();

      expect(component.lastUpdated()).not.toBeNull();
      expect(component.isLoading()).toBeFalse();
      expect(component.isRefreshing()).toBeFalse();
      expect(component.stats()).toEqual(mockStats);
      expect(component.recentStudents()).toEqual([mockStudent]);
    }));

    it('should set loadError and stop loading on API error', fakeAsync(() => {
      adminService.getStats.and.returnValue(throwError(() => ({ status: 500 })));
      component.fetchData();
      tick();

      expect(component.isLoading()).toBeFalse();
      expect(component.isRefreshing()).toBeFalse();
      expect(component.loadError()).toContain("Impossible d'actualiser");
    }));
  });

  describe('kpis', () => {
    beforeEach(() => loadData(component));

    it('should return 4 KPI cards derived from stats', () => {
      const kpis = component.kpis;
      expect(kpis.length).toBe(4);

      expect(kpis[0].title).toBe('Étudiants inscrits');
      expect(kpis[0].value).toBe(100);
      expect(kpis[0].tone).toBe('blue');
      expect(kpis[0].link).toBe('/admin/users');

      expect(kpis[1].title).toBe('Cours créés');
      expect(kpis[1].value).toBe(25);
      expect(kpis[1].tone).toBe('violet');

      expect(kpis[2].title).toBe('Ressources IA');
      expect(kpis[2].value).toBe(95);
      expect(kpis[2].tone).toBe('green');

      expect(kpis[3].title).toBe('Activités terminées');
      expect(kpis[3].value).toBe(150);
      expect(kpis[3].tone).toBe('orange');
    });

    it('should return empty array when stats is null', () => {
      component.stats.set(null);
      expect(component.kpis).toEqual([]);
    });
  });

  describe('donutSegments', () => {
    beforeEach(() => loadData(component));

    it('should compute donut segments from stats', () => {
      const segments = component.donutSegments();
      expect(segments.length).toBe(3);

      const actif = segments.find(s => s.label === 'Actifs')!;
      expect(actif.value).toBe(70);
      expect(actif.color).toBe('#10b981');

      const attente = segments.find(s => s.label === 'En attente')!;
      expect(attente.value).toBe(20);
      expect(attente.color).toBe('#f59e0b');

      const desactive = segments.find(s => s.label === 'Désactivés')!;
      expect(desactive.value).toBe(10);
      expect(desactive.color).toBe('#94a3b8');
    });

    it('should return empty array when no students', () => {
      component.stats.set({ ...mockStats, totalStudents: 0, activeStudents: 0, pendingStudents: 0, disabledStudents: 0 });
      expect(component.donutSegments()).toEqual([]);
    });
  });

  describe('barChartPoints', () => {
    beforeEach(() => loadData(component));

    it('should return 6 monthly bar points', () => {
      const points = component.barChartPoints();
      expect(points.length).toBe(6);
      expect(points[0].label).toBe('Mai');
      expect(points[5].label).toBe('Oct');
      expect(points[0].value).toBeGreaterThanOrEqual(0);
    });

    it('should return empty array when stats is null', () => {
      component.stats.set(null);
      expect(component.barChartPoints()).toEqual([]);
    });
  });

  describe('alerts', () => {
    beforeEach(() => loadData(component));

    it('should generate warning alert for pending students', () => {
      const alerts = component.alerts();
      const warning = alerts.find(a => a.level === 'warning')!;
      expect(warning.message).toContain('20 compte(s) en attente de validation');
      expect(warning.link).toBe('/admin/users');
    });

    it('should generate info alert for disabled students', () => {
      const alerts = component.alerts();
      const info = alerts.find(a => a.level === 'info')!;
      expect(info.message).toContain('10 compte(s) désactivé(s) détecté(s)');
    });

    it('should generate success alert for active students', () => {
      const alerts = component.alerts();
      const success = alerts.find(a => a.level === 'success' && a.message.includes('étudiant(s) actif(s)'));
      expect(success).toBeDefined();
    });

    it('should generate success alert when learning resources > 50', () => {
      const alerts = component.alerts();
      const resourcesAlert = alerts.find(a => a.message.includes("générées par l'IA"));
      expect(resourcesAlert).toBeDefined();
    });

    it('should return empty array when stats is null', () => {
      component.stats.set(null);
      expect(component.alerts()).toEqual([]);
    });
  });

  describe('activityFeed', () => {
    beforeEach(() => loadData(component));

    it('should build activity feed from recent students', () => {
      const feed = component.activityFeed();
      expect(feed.length).toBe(1);
      expect(feed[0].student).toEqual(mockStudent);
      expect(['a généré un quiz', 'a créé une synthèse', "s'est inscrit(e)"]).toContain(feed[0].action);
    });

    it('should limit feed to 5 items', () => {
      const manyStudents: AdminUser[] = Array.from({ length: 10 }, (_, i) => ({
        ...mockStudent,
        id: i + 1,
        firstName: `Etudiant${i + 1}`
      }));
      component.recentStudents.set(manyStudents);
      const feed = component.activityFeed();
      expect(feed.length).toBe(5);
    });
  });

  describe('filteredStudents', () => {
    beforeEach(() => loadData(component));

    it('should return all students when no search and ALL filter', () => {
      expect(component.filteredStudents().length).toBe(1);
    });

    it('should filter by search term', () => {
      component.setSearchTerm('Sophie');
      expect(component.filteredStudents().length).toBe(1);

      component.setSearchTerm('Inconnu');
      expect(component.filteredStudents().length).toBe(0);
    });

    it('should filter by status', () => {
      component.setStatusFilter('ACTIF');
      expect(component.filteredStudents().length).toBe(1);

      component.setStatusFilter('EN_ATTENTE');
      expect(component.filteredStudents().length).toBe(0);
    });

    it('should combine search and status filter', () => {
      component.setSearchTerm('Dupont');
      component.setStatusFilter('ACTIF');
      expect(component.filteredStudents().length).toBe(1);

      component.setStatusFilter('DESACTIVE');
      expect(component.filteredStudents().length).toBe(0);
    });
  });

  describe('statusCount', () => {
    beforeEach(() => loadData(component));

    it('should return correct count for each status', () => {
      expect(component.statusCount('ACTIF')).toBe(70);
      expect(component.statusCount('EN_ATTENTE')).toBe(20);
      expect(component.statusCount('DESACTIVE')).toBe(10);
      expect(component.statusCount('ALL')).toBe(100);
    });

    it('should return 0 when stats is null', () => {
      component.stats.set(null);
      expect(component.statusCount('ACTIF')).toBe(0);
    });
  });

  describe('recentStatusCount', () => {
    beforeEach(() => loadData(component));

    it('should count recent students by status', () => {
      expect(component.recentStatusCount('ALL')).toBe(1);
      expect(component.recentStatusCount('ACTIF')).toBe(1);
      expect(component.recentStatusCount('EN_ATTENTE')).toBe(0);
    });
  });

  describe('statusWidth', () => {
    beforeEach(() => loadData(component));

    it('should compute percentage width for a status', () => {
      expect(component.statusWidth('ACTIF')).toBe(70);
      expect(component.statusWidth('EN_ATTENTE')).toBe(20);
      expect(component.statusWidth('DESACTIVE')).toBe(10);
    });

    it('should return 0 when totalStudents is 0', () => {
      component.stats.set({ ...mockStats, totalStudents: 0, activeStudents: 0, pendingStudents: 0, disabledStudents: 0 });
      expect(component.statusWidth('ACTIF')).toBe(0);
    });
  });

  describe('engagementRate and platformHealth', () => {
    beforeEach(() => loadData(component));

    it('should compute engagement rate', () => {
      expect(component.engagementRate).toBe(50);
    });

    it('should return 0 engagement rate when no students', () => {
      component.stats.set({ ...mockStats, totalStudents: 0, completedActivities: 0 });
      expect(component.engagementRate).toBe(0);
    });

    it('should compute platform health', () => {
      const health = component.platformHealth;
      expect(health).toBeGreaterThanOrEqual(0);
      expect(health).toBeLessThanOrEqual(100);
    });

    it('should return 0 platform health when no students', () => {
      component.stats.set({ ...mockStats, totalStudents: 0 });
      expect(component.platformHealth).toBe(0);
    });
  });

  describe('barMax and barHeight', () => {
    beforeEach(() => loadData(component));

    it('should compute bar max and heights', () => {
      const points = component.barChartPoints();
      const max = component.barMax();
      expect(max).toBeGreaterThan(0);

      const first = points[0];
      const height = component.barHeight(first);
      expect(height).toBe((first.value / max) * 120);
    });
  });

  describe('helpers', () => {
    beforeEach(() => loadData(component));

    it('studentInitials should return uppercased initials', () => {
      expect(component.studentInitials(mockStudent)).toBe('SD');
      expect(component.studentInitials({ ...mockStudent, firstName: '', lastName: '' })).toBe('E');
    });

    it('avatarColor should return a valid color', () => {
      const color = component.avatarColor(mockStudent);
      expect(['blue', 'violet', 'green', 'orange', 'pink', 'teal']).toContain(color);
    });

    it('statusLabel should translate status codes', () => {
      expect(component.statusLabel('ACTIF')).toBe('Actif');
      expect(component.statusLabel('EN_ATTENTE')).toBe('En attente');
      expect(component.statusLabel('DESACTIVE')).toBe('Désactivé');
      expect(component.statusLabel('UNKNOWN')).toBe('UNKNOWN');
    });

    it('fmt should format numbers with fr-FR locale', () => {
      expect(component.fmt(1234)).toBe(new Intl.NumberFormat('fr-FR').format(1234));
      expect(component.fmt(0)).toBe('0');
    });

    it('fmtPct should return percentage string', () => {
      expect((component as any).fmtPct(70, 100)).toBe('70%');
      expect((component as any).fmtPct(0, 100)).toBe('0%');
      expect((component as any).fmtPct(5, 0)).toBe('0%');
    });

    it('resourcePct should return resource percentage', () => {
      const pct = component.resourcePct(40);
      expect(pct).toBeCloseTo(42.1, 1);
    });

    it('setSearchTerm, setStatusFilter, setHoveredBar should update signals', () => {
      component.setSearchTerm('test');
      expect(component.searchTerm()).toBe('test');

      component.setStatusFilter('EN_ATTENTE');
      expect(component.statusFilter()).toBe('EN_ATTENTE');

      component.setHoveredBar(2);
      expect(component.hoveredBar()).toBe(2);
      component.setHoveredBar(null);
      expect(component.hoveredBar()).toBeNull();
    });
  });

  describe('animation counters', () => {
    beforeEach(() => loadData(component));

    it('animateCounters should animate kpi values up to targets', fakeAsync(() => {
      const targets = component.kpis.map(k => k.value);
      (component as any).animateCounters();
      const now = performance.now();
      rafCallbacks.forEach(cb => cb(now + 10000));
      expect(component.animKpi()).toEqual(targets);
    }));
  });
});
