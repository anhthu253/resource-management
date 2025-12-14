import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ResourceResponseDto } from '../dtos/resource.dto';

@Injectable({
  providedIn: 'root',
})
class ResourceService {
  constructor(private http: HttpClient) {}
  getResources = (): Observable<ResourceResponseDto[]> => {
    return this.http.get<ResourceResponseDto[]>('http://localhost:8081/resource/all');
  };
}
