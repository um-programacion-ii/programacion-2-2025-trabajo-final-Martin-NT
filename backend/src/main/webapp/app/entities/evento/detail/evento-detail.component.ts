import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { FormatMediumDatePipe } from 'app/shared/date';
import { IEvento } from '../evento.model';

@Component({
  selector: 'jhi-evento-detail',
  templateUrl: './evento-detail.component.html',
  imports: [SharedModule, RouterModule, FormatMediumDatePipe],
})
export class EventoDetailComponent {
  evento = input<IEvento | null>(null);

  previousState(): void {
    window.history.back();
  }
}
