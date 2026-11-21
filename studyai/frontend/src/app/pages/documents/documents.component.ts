import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DocumentService, DocumentDto } from '../../services/document.service';

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './documents.component.html',
  styleUrl: './documents.component.scss'
})
export class DocumentsComponent implements OnInit {
  private service = inject(DocumentService);

  studentId = 1;                 // à remplacer par l'id de l'utilisateur connecté
  documents: DocumentDto[] = [];
  selectedFile: File | null = null;
  uploading = false;
  dragOver = false;
  error = '';
  success = '';

  readonly maxSize = 20 * 1024 * 1024; // 20 Mo
  readonly allowed = ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'txt', 'xls', 'xlsx'];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.service.list(this.studentId).subscribe({
      next: d => (this.documents = d),
      error: () => (this.error = 'Impossible de charger les documents')
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) this.setFile(input.files[0]);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
    if (event.dataTransfer?.files.length) this.setFile(event.dataTransfer.files[0]);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = true;
  }

  private setFile(file: File): void {
    this.error = '';
    this.success = '';
    const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
    if (!this.allowed.includes(ext)) {
      this.error = 'Type non autorisé (PDF, Word, PowerPoint, Excel, TXT)';
      return;
    }
    if (file.size > this.maxSize) {
      this.error = 'Fichier trop volumineux (max 20 Mo)';
      return;
    }
    this.selectedFile = file;
  }

  upload(): void {
    if (!this.selectedFile) return;
    this.uploading = true;
    this.error = '';

    this.service.upload(this.selectedFile, this.studentId).subscribe({
      next: () => {
        this.uploading = false;
        this.success = 'Document importé avec succès ✅';
        this.selectedFile = null;
        this.load();
      },
      error: err => {
        this.uploading = false;
        this.error = err.error?.message ?? "Erreur lors de l'import";
      }
    });
  }

  download(doc: DocumentDto): void {
    this.service.download(doc.id).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = doc.originalName;
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  remove(doc: DocumentDto): void {
    if (!confirm(`Supprimer « ${doc.originalName} » ?`)) return;
    this.service.delete(doc.id).subscribe(() => this.load());
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' o';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' Ko';
    return (bytes / (1024 * 1024)).toFixed(1) + ' Mo';
  }
}