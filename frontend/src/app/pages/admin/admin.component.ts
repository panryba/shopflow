import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ToggleButtonModule } from 'primeng/togglebutton';
import { MessageService } from 'primeng/api';
import { InventoryService } from '../../core/services/inventory.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule, ToggleButtonModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss'
})
export class AdminComponent implements OnInit {
  private inventoryService = inject(InventoryService);
  private messageService = inject(MessageService);

  accept = signal(true);

  ngOnInit() {
    this.inventoryService.getMode().subscribe({
      next: mode => this.accept.set(mode),
      error: () => this.messageService.add({ severity: 'warn', summary: 'Could not read inventory mode' })
    });
  }

  setMode(checked: boolean) {
    this.accept.set(checked);
    this.inventoryService.setMode(checked).subscribe({
      next: () => this.messageService.add({
        severity: 'success',
        summary: checked ? 'Inventory now accepting orders' : 'Inventory now rejecting orders'
      }),
      error: () => this.messageService.add({ severity: 'error', summary: 'Failed to update inventory mode' })
    });
  }
}