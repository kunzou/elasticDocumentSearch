import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Document } from './domain/document';

@Injectable({
  providedIn: 'root'
})
export class SearchService {
  SERVER_URL: string = environment.baseUrl;
  constructor(private httpClient: HttpClient) { }

  public search(keyword): Observable<any> {
    const url = `${this.SERVER_URL}/search/${keyword}`;
    return this.httpClient.get<Document[]>(url);
  }

  public getAllDocuments(): Observable<any> {
    const url = `${this.SERVER_URL}/documents`;
    return this.httpClient.get<Document[]>(url);
  }
}
