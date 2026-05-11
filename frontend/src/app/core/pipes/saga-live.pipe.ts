import { Pipe, PipeTransform } from '@angular/core';
import { StatusHistoryEntry } from '../models/order.model';

const TERMINAL = new Set(['INVENTORY_APPROVED', 'PAYMENT_FAILED', 'CANCELLED', 'PAYMENT_ROLLED_BACK']);

@Pipe({ name: 'sagaLive', standalone: true, pure: true })
export class SagaLivePipe implements PipeTransform {
  transform(history: StatusHistoryEntry[] | null | undefined): boolean {
    if (!history?.length) return true;
    return !TERMINAL.has(history[history.length - 1].status);
  }
}
