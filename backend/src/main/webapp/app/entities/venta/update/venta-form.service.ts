import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { IVenta, NewVenta } from '../venta.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IVenta for edit and NewVentaFormGroupInput for create.
 */
type VentaFormGroupInput = IVenta | PartialWithRequiredKeyOf<NewVenta>;

type VentaFormDefaults = Pick<NewVenta, 'id' | 'asientos'>;

type VentaFormGroupContent = {
  id: FormControl<IVenta['id'] | NewVenta['id']>;
  fechaVenta: FormControl<IVenta['fechaVenta']>;
  estado: FormControl<IVenta['estado']>;
  descripcion: FormControl<IVenta['descripcion']>;
  precioVenta: FormControl<IVenta['precioVenta']>;
  cantidadAsientos: FormControl<IVenta['cantidadAsientos']>;
  evento: FormControl<IVenta['evento']>;
  asientos: FormControl<IVenta['asientos']>;
};

export type VentaFormGroup = FormGroup<VentaFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class VentaFormService {
  createVentaFormGroup(venta: VentaFormGroupInput = { id: null }): VentaFormGroup {
    const ventaRawValue = {
      ...this.getFormDefaults(),
      ...venta,
    };
    return new FormGroup<VentaFormGroupContent>({
      id: new FormControl(
        { value: ventaRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      fechaVenta: new FormControl(ventaRawValue.fechaVenta, {
        validators: [Validators.required],
      }),
      estado: new FormControl(ventaRawValue.estado, {
        validators: [Validators.required],
      }),
      descripcion: new FormControl(ventaRawValue.descripcion),
      precioVenta: new FormControl(ventaRawValue.precioVenta, {
        validators: [Validators.required],
      }),
      cantidadAsientos: new FormControl(ventaRawValue.cantidadAsientos, {
        validators: [Validators.required],
      }),
      evento: new FormControl(ventaRawValue.evento, {
        validators: [Validators.required],
      }),
      asientos: new FormControl(ventaRawValue.asientos ?? []),
    });
  }

  getVenta(form: VentaFormGroup): IVenta | NewVenta {
    return form.getRawValue() as IVenta | NewVenta;
  }

  resetForm(form: VentaFormGroup, venta: VentaFormGroupInput): void {
    const ventaRawValue = { ...this.getFormDefaults(), ...venta };
    form.reset(
      {
        ...ventaRawValue,
        id: { value: ventaRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): VentaFormDefaults {
    return {
      id: null,
      asientos: [],
    };
  }
}
