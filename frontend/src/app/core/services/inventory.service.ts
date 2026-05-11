import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private http = inject(HttpClient);

  getMode(): Observable<boolean> {
    return this.http.get<boolean>('/api/inventory/mode');
  }

  setMode(accept: boolean): Observable<string> {
    return this.http.put(`/api/inventory/mode?accept=${accept}`, null, {
      responseType: 'text'
    });
  }
}