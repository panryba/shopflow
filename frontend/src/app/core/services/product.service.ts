import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ImportResult, Product } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private http = inject(HttpClient);

  getAll(): Observable<Product[]> {
    return this.http.get<Product[]>('/api/products');
  }

  importProducts(file: File): Observable<ImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ImportResult>('/api/products/import', formData);
  }

  getCrash(): Observable<boolean> {
    return this.http.get<boolean>('/api/products/crash');
  }

  setCrash(enabled: boolean): Observable<void> {
    return this.http.put<void>(`/api/products/crash?enabled=${enabled}`, null);
  }
}