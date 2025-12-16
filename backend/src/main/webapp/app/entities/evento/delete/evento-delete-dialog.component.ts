import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IEvento } from '../evento.model';
import { EventoService } from '../service/evento.service';

@Component({
  templateUrl: './evento-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class EventoDeleteDialogComponent {
  evento?: IEvento;

  protected eventoService = inject(EventoService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.eventoService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
