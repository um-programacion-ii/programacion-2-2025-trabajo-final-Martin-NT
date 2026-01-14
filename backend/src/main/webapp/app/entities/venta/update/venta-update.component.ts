import { Component, OnInit, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IEvento } from 'app/entities/evento/evento.model';
import { EventoService } from 'app/entities/evento/service/evento.service';
import { IAsiento } from 'app/entities/asiento/asiento.model';
import { AsientoService } from 'app/entities/asiento/service/asiento.service';
import { VentaEstado } from 'app/entities/enumerations/venta-estado.model';
import { VentaService } from '../service/venta.service';
import { IVenta } from '../venta.model';
import { VentaFormGroup, VentaFormService } from './venta-form.service';

@Component({
  selector: 'jhi-venta-update',
  templateUrl: './venta-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class VentaUpdateComponent implements OnInit {
  isSaving = false;
  venta: IVenta | null = null;
  ventaEstadoValues = Object.keys(VentaEstado);

  eventosSharedCollection: IEvento[] = [];
  asientosSharedCollection: IAsiento[] = [];

  protected ventaService = inject(VentaService);
  protected ventaFormService = inject(VentaFormService);
  protected eventoService = inject(EventoService);
  protected asientoService = inject(AsientoService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: VentaFormGroup = this.ventaFormService.createVentaFormGroup();

  compareEvento = (o1: IEvento | null, o2: IEvento | null): boolean => this.eventoService.compareEvento(o1, o2);

  compareAsiento = (o1: IAsiento | null, o2: IAsiento | null): boolean => this.asientoService.compareAsiento(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ venta }) => {
      this.venta = venta;
      if (venta) {
        this.updateForm(venta);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const venta = this.ventaFormService.getVenta(this.editForm);
    if (venta.id !== null) {
      this.subscribeToSaveResponse(this.ventaService.update(venta));
    } else {
      this.subscribeToSaveResponse(this.ventaService.create(venta));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IVenta>>): void {
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

  protected updateForm(venta: IVenta): void {
    this.venta = venta;
    this.ventaFormService.resetForm(this.editForm, venta);

    this.eventosSharedCollection = this.eventoService.addEventoToCollectionIfMissing<IEvento>(this.eventosSharedCollection, venta.evento);
    this.asientosSharedCollection = this.asientoService.addAsientoToCollectionIfMissing<IAsiento>(
      this.asientosSharedCollection,
      ...(venta.asientos ?? []),
    );
  }

  protected loadRelationshipsOptions(): void {
    this.eventoService
      .query()
      .pipe(map((res: HttpResponse<IEvento[]>) => res.body ?? []))
      .pipe(map((eventos: IEvento[]) => this.eventoService.addEventoToCollectionIfMissing<IEvento>(eventos, this.venta?.evento)))
      .subscribe((eventos: IEvento[]) => (this.eventosSharedCollection = eventos));

    this.asientoService
      .query()
      .pipe(map((res: HttpResponse<IAsiento[]>) => res.body ?? []))
      .pipe(
        map((asientos: IAsiento[]) =>
          this.asientoService.addAsientoToCollectionIfMissing<IAsiento>(asientos, ...(this.venta?.asientos ?? [])),
        ),
      )
      .subscribe((asientos: IAsiento[]) => (this.asientosSharedCollection = asientos));
  }
}
