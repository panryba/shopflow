import { HttpClient, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { interval, Observable } from 'rxjs';
import { startWith, switchMap, takeWhile } from 'rxjs/operators';
import { CreateOrderRequest, OrderResponse, OrderStatus } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private http = inject(HttpClient);

  getAll(): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>('/api/orders');
  }

  getById(id: string): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(`/api/orders/${id}`);
  }

  create(request: CreateOrderRequest): Observable<HttpResponse<void>> {
    const idempotencyKey = crypto.randomUUID();
    return this.http.post<void>('/api/orders', request, {
      observe: 'response',
      headers: { 'Idempotency-Key': idempotencyKey }
    });
  }

  cancel(id: string): Observable<OrderResponse> {
    return this.http.put<OrderResponse>(`/api/orders/${id}/cancel`, null);
  }

  watchOrder(id: string): Observable<OrderResponse> {
    const TERMINAL = new Set<string>([OrderStatus.INVENTORY_APPROVED, OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED, 'PAYMENT_ROLLED_BACK']);
    return interval(2000).pipe(
      startWith(0),
      switchMap(() => this.http.get<OrderResponse>(`/api/orders/${id}`)),
      takeWhile(order => {
        const last = order.history?.at(-1)?.status;
        return !last || !TERMINAL.has(last);
      }, true)
    );
  }
}
