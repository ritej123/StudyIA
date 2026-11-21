/// <reference types="jasmine" />

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DocumentsComponent } from './documents.component';
import { DocumentService } from '../../services/document.service';

describe('DocumentsComponent', () => {
  let component: DocumentsComponent;
  let fixture: ComponentFixture<DocumentsComponent>;
  let documentService: jasmine.SpyObj<DocumentService>;

  beforeEach(async () => {
    documentService = jasmine.createSpyObj<DocumentService>(
      'DocumentService',
      ['list', 'upload', 'download', 'delete']
    );
    documentService.list.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [DocumentsComponent],
      providers: [
        { provide: DocumentService, useValue: documentService }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DocumentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load the current student documents on init', () => {
    expect(documentService.list).toHaveBeenCalledWith(1);
    expect(component.documents).toEqual([]);
  });
});
