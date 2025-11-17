import { Component, OnInit, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IEvento } from '../evento.model';
import { EventoService } from '../service/evento.service';
import { EventoFormGroup, EventoFormService } from './evento-form.service';

@Component({
  selector: 'jhi-evento-update',
  templateUrl: './evento-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class EventoUpdateComponent implements OnInit {
  isSaving = false;
  evento: IEvento | null = null;

  protected eventoService = inject(EventoService);
  protected eventoFormService = inject(EventoFormService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: EventoFormGroup = this.eventoFormService.createEventoFormGroup();

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ evento }) => {
      this.evento = evento;
      if (evento) {
        this.updateForm(evento);
      }
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const evento = this.eventoFormService.getEvento(this.editForm);
    if (evento.id !== null) {
      this.subscribeToSaveResponse(this.eventoService.update(evento));
    } else {
      this.subscribeToSaveResponse(this.eventoService.create(evento));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IEvento>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(evento: IEvento): void {
    this.evento = evento;
    this.eventoFormService.resetForm(this.editForm, evento);
  }
}
