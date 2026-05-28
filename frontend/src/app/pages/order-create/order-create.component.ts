import { Component, inject, effect, signal } from '@angular/core';
import { DecimalPipe, NgOptimizedImage } from '@angular/common';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CarouselModule } from 'primeng/carousel';
import { MessageService } from 'primeng/api';
import { OrderService } from '../../core/services/order.service';
import { CartService } from '../../core/services/cart.service';
import { PRODUCTS } from '../../core/constants/products';

@Component({
  selector: 'app-order-create',
  standalone: true,
  imports: [ButtonModule, CardModule, CarouselModule, DecimalPipe, NgOptimizedImage],
  templateUrl: './order-create.component.html',
  styleUrl: './order-create.component.scss'
})
export class OrderCreateComponent {
  private orderService = inject(OrderService);
  private router = inject(Router);
  private messageService = inject(MessageService);

  protected cartService = inject(CartService);

  readonly products = PRODUCTS;
  readonly submitting = signal(false);

  constructor() {
    effect(() => {
      this.cartService.cart();
      Promise.resolve().then(() => window.dispatchEvent(new Event('resize')));
    });
  }

  readonly responsiveOptions = [
    { breakpoint: '1024px', numVisible: 3, numScroll: 1 },
    { breakpoint: '768px',  numVisible: 2, numScroll: 1 },
    { breakpoint: '560px',  numVisible: 1, numScroll: 1 }
  ];

  submit(): void {
    if (this.submitting()) return;
    const items = this.cartService.cart().map(i => ({
      productId: i.product.id,
      quantity: i.quantity,
      price: i.product.price
    }));
    if (!items.length) return;

    this.submitting.set(true);
    this.orderService.create({ items }).subscribe({
      next: response => {
        const orderId = response.headers.get('Location')?.split('/').pop();
        const corrId = response.headers.get('X-Correlation-ID') ?? undefined;
        if (orderId) {
          this.cartService.clear();
          void this.router.navigate(['/orders', orderId], { state: { corrId } });
        }
      },
      error: () => {
        this.submitting.set(false);
        this.messageService.add({ severity: 'error', summary: 'Failed to submit order' });
      }
    });
  }
}
