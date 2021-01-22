import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { DocumentService } from '../document.service';
import { catchError, map } from 'rxjs/operators';
import { HttpErrorResponse, HttpEventType } from '@angular/common/http';
import { of, Subject } from 'rxjs';
import { Document } from '../domain/document';
import { SearchService } from '../search.service';
import { animate, state, style, transition, trigger } from '@angular/animations';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  animations : [
    // Here we are defining what are the states our panel can be in 
    // and the style each state corresponds to.
    trigger('panelState', [
      state('closed', style({ height: '32px', overflow: 'hidden' })),
      state('open', style({ height: '*' })),
      transition('closed <=> open', animate('300ms ease-in-out')),
    ]),
  ],  
})
export class DashboardComponent implements OnInit {
  @ViewChild("fileUpload", { static: false }) fileUpload: ElementRef; files = [];
  private _success = new Subject<String>();
  fileUploadResponse: String;
  fileUploadResponseAlertType: any;
  folded = [];
  documents: Document[];
  page :number = 1
  pageSize :number = 10    
  
  constructor(
    private router: Router,
    private documentService: DocumentService,
    private searchService: SearchService,
    public auth: AuthService
  ) {
  }

  ngOnInit() {
    this._success.subscribe((message) => this.fileUploadResponse = message);
    this.getAllDocuments();
  }

  upload(): void {
    this.files = [];
    this.fileUploadResponse = "";
    const fileUpload = this.fileUpload.nativeElement; fileUpload.onchange = () => {
        Array.from(fileUpload.files).forEach(element => {
        this.files.push({ data: element, inProgress: false, progress: 0 });
      });  
        
      this.uploadFiles();
    };
    fileUpload.click();
  } 
  
  private uploadFiles(): void {
    this.fileUpload.nativeElement.value = '';
    this.files.forEach(file => {
      this.uploadFile(file);
    });
  }  

  private uploadFile(file): void {
    const formData = new FormData();
    formData.append('file', file.data);
    file.inProgress = true;
    this.documentService.upload(formData).pipe(
      map(event => {
        switch (event.type) {
          case HttpEventType.UploadProgress:
            file.progress = Math.round(event.loaded * 100 / event.total);
            break;
          case HttpEventType.Response:
            this.fileUploadResponseAlertType = "success";
            this.fileUploadResponse = "Upload successful";
            this.showMessage();             
            return event;
        }
      }),
      catchError((error: HttpErrorResponse) => {
        file.inProgress = false;
        this.fileUploadResponseAlertType = "danger";
        this.fileUploadResponse = error.error.message;
        this.showMessage();
        return of(`${file.data.address} upload failed.`);
      })).subscribe(
        (event: any) => {
          if (typeof (event) === 'object') {
            this.documents.push(event.body);
          }
        }        
      );
  }

  delay(ms: number) {
    return new Promise( resolve => setTimeout(resolve, ms) );
  }

  getAllDocuments(): void {
    this.searchService.getAllDocuments().subscribe(results => {
      this.documents = results;
      for (let index = 0; index < results.length; index++) {
        this.folded[index] = 'closed';
      }
    })    
  }

  showMessage() {
    this._success.next(this.fileUploadResponse);
  }    

  delete(id): void {
    this.documentService.delete(id).subscribe(
      (response) => {
        const deleted = this.documents.find(document => document.id === id);
        this.documents.splice(this.documents.indexOf(deleted), 1);
        this.fileUploadResponseAlertType = "success";
        this.fileUploadResponse = "Document successfully deleted";
        this.showMessage();        
      },
      (error) => {
        console.log(error);
        this.fileUploadResponseAlertType = "danger";
        this.fileUploadResponse = error.error.message;
        this.showMessage();
      }
    )
  }

  toggleFold(index){
    this.folded[index] = this.folded[index] === 'open' ? 'closed' : 'open';
  }  
}
