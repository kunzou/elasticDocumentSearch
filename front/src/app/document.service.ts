import { Injectable } from '@angular/core';
import { environment } from 'src/environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  SERVER_URL: string = environment.baseUrl;
  constructor(private httpClient: HttpClient) { }

  public upload(formData) {
    const url = `${this.SERVER_URL}/upload`;
    return this.httpClient.post<any>(url, formData, {
      reportProgress: true,
      observe: 'events'
    });
  }

  public delete(id) {
    const url = `${this.SERVER_URL}/delete/${id}`;
    return this.httpClient.delete<any>(url);    
  }

  download(id): Observable<any> {
    // return this.httpClient.get(`${this.SERVER_URL}/download/` + id, { responseType: 'blob' }).pipe(map((response)=>{
    //     return {
    //         filename: 'yourFileName.pdf',
    //         data: response.blob()
    //     };
    // }));
    return this.httpClient.get(`${this.SERVER_URL}/download/`+id, {
      responseType: 'blob',
      observe: 'response'
    })
    .pipe(
      map((res: any) => {
        return new Blob([res.body]);
      })
    );    
  }
}
