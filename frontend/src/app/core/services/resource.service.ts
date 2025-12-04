import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ResourceService {
  constructor(private http: HttpClient) {}
  getResources = (): Observable<string[]> => {
    return this.http.get<string[]>('http://localhost:8081/resource/all');
  };
}
