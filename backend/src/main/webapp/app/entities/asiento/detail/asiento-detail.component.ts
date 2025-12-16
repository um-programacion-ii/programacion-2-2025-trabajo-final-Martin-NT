import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { IAsiento } from '../asiento.model';

@Component({
  selector: 'jhi-asiento-detail',
  templateUrl: './asiento-detail.component.html',
  imports: [SharedModule, RouterModule],
})
export class AsientoDetailComponent {
  asiento = input<IAsiento | null>(null);

  previousState(): void {
    window.history.back();
  }
}
