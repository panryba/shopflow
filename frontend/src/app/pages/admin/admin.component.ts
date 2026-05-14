import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ToggleButtonModule } from 'primeng/togglebutton';
import { Select } from 'primeng/select';
import { MessageService } from 'primeng/api';
import { InventoryService } from '../../core/services/inventory.service';
import { PaymentService } from '../../core/services/payment.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule, ToggleButtonModule, Select],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss'
})
export class AdminComponent implements OnInit {
  private inventoryService = inject(InventoryService);
  private paymentService = inject(PaymentService);
  private messageService = inject(MessageService);

  accept = signal(true);
  paymentDelay = signal(0);
  inventoryDelay = signal(0);
  paymentCrash = signal(false);
  inventoryCrash = signal(false);

  readonly delayOptions = [
    { label: '0s', value: 0 },
    { label: '2s', value: 2 },
    { label: '4s', value: 4 },
    { label: '6s', value: 6 },
    { label: '8s', value: 8 },
  ];

  ngOnInit() {
    this.inventoryService.getMode().subscribe({
      next: mode => this.accept.set(mode),
      error: () => this.messageService.add({ severity: 'warn', summary: 'Could not read inventory mode' })
    });
    this.paymentService.getDelay().subscribe({
      next: d => this.paymentDelay.set(d),
      error: () => {}
    });
    this.inventoryService.getDelay().subscribe({
      next: d => this.inventoryDelay.set(d),
      error: () => {}
    });
    this.paymentService.getCrash().subscribe({
      next: v => this.paymentCrash.set(v),
      error: () => {}
    });
    this.inventoryService.getCrash().subscribe({
      next: v => this.inventoryCrash.set(v),
      error: () => {}
    });
  }

  setMode(checked: boolean) {
    this.inventoryService.setMode(checked).subscribe({
      next: () => {
        this.accept.set(checked);
        this.messageService.add({
          severity: 'success',
          summary: checked ? 'Inventory now accepting orders' : 'Inventory now rejecting orders'
        });
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Failed to update inventory mode' })
    });
  }

  setPaymentDelay(seconds: number) {
    this.paymentService.setDelay(seconds).subscribe({
      next: () => {
        this.paymentDelay.set(seconds);
        this.messageService.add({ severity: 'success', summary: `Payment delay set to ${seconds}s` });
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Failed to update payment delay' })
    });
  }

  setInventoryDelay(seconds: number) {
    this.inventoryService.setDelay(seconds).subscribe({
      next: () => {
        this.inventoryDelay.set(seconds);
        this.messageService.add({ severity: 'success', summary: `Inventory delay set to ${seconds}s` });
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Failed to update inventory delay' })
    });
  }

  setPaymentCrash(enabled: boolean) {
    this.paymentService.setCrash(enabled).subscribe({
      next: () => {
        this.paymentCrash.set(enabled);
        this.messageService.add({
          severity: enabled ? 'warn' : 'success',
          summary: enabled ? 'Payment consumer will CRASH on every message' : 'Payment consumer crash mode disabled'
        });
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Failed to update payment crash mode' })
    });
  }

  setInventoryCrash(enabled: boolean) {
    this.inventoryService.setCrash(enabled).subscribe({
      next: () => {
        this.inventoryCrash.set(enabled);
        this.messageService.add({
          severity: enabled ? 'warn' : 'success',
          summary: enabled ? 'Inventory consumer will CRASH on every message' : 'Inventory consumer crash mode disabled'
        });
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Failed to update inventory crash mode' })
    });
  }
}