import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { IEvento, NewEvento } from '../evento.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IEvento for edit and NewEventoFormGroupInput for create.
 */
type EventoFormGroupInput = IEvento | PartialWithRequiredKeyOf<NewEvento>;

type EventoFormDefaults = Pick<NewEvento, 'id'>;

type EventoFormGroupContent = {
  id: FormControl<IEvento['id'] | NewEvento['id']>;
  titulo: FormControl<IEvento['titulo']>;
  descripcion: FormControl<IEvento['descripcion']>;
  fecha: FormControl<IEvento['fecha']>;
  hora: FormControl<IEvento['hora']>;
  organizador: FormControl<IEvento['organizador']>;
  presentadores: FormControl<IEvento['presentadores']>;
  cantidadAsientosTotales: FormControl<IEvento['cantidadAsientosTotales']>;
  filaAsientos: FormControl<IEvento['filaAsientos']>;
  columnaAsientos: FormControl<IEvento['columnaAsientos']>;
};

export type EventoFormGroup = FormGroup<EventoFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class EventoFormService {
  createEventoFormGroup(evento: EventoFormGroupInput = { id: null }): EventoFormGroup {
    const eventoRawValue = {
      ...this.getFormDefaults(),
      ...evento,
    };
    return new FormGroup<EventoFormGroupContent>({
      id: new FormControl(
        { value: eventoRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      titulo: new FormControl(eventoRawValue.titulo, {
        validators: [Validators.required],
      }),
      descripcion: new FormControl(eventoRawValue.descripcion),
      fecha: new FormControl(eventoRawValue.fecha, {
        validators: [Validators.required],
      }),
      hora: new FormControl(eventoRawValue.hora, {
        validators: [Validators.required],
      }),
      organizador: new FormControl(eventoRawValue.organizador),
      presentadores: new FormControl(eventoRawValue.presentadores),
      cantidadAsientosTotales: new FormControl(eventoRawValue.cantidadAsientosTotales, {
        validators: [Validators.required],
      }),
      filaAsientos: new FormControl(eventoRawValue.filaAsientos, {
        validators: [Validators.required],
      }),
      columnaAsientos: new FormControl(eventoRawValue.columnaAsientos, {
        validators: [Validators.required],
      }),
    });
  }

  getEvento(form: EventoFormGroup): IEvento | NewEvento {
    return form.getRawValue() as IEvento | NewEvento;
  }

  resetForm(form: EventoFormGroup, evento: EventoFormGroupInput): void {
    const eventoRawValue = { ...this.getFormDefaults(), ...evento };
    form.reset(
      {
        ...eventoRawValue,
        id: { value: eventoRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): EventoFormDefaults {
    return {
      id: null,
    };
  }
}
