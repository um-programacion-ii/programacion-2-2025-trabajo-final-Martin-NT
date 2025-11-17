import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IAsiento } from '../asiento.model';
import { AsientoService } from '../service/asiento.service';

@Component({
  templateUrl: './asiento-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class AsientoDeleteDialogComponent {
  asiento?: IAsiento;

  protected asientoService = inject(AsientoService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.asientoService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
