import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../asiento.test-samples';

import { AsientoFormService } from './asiento-form.service';

describe('Asiento Form Service', () => {
  let service: AsientoFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AsientoFormService);
  });

  describe('Service methods', () => {
    describe('createAsientoFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createAsientoFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            fila: expect.any(Object),
            columna: expect.any(Object),
            estado: expect.any(Object),
            personaActual: expect.any(Object),
            evento: expect.any(Object),
            ventas: expect.any(Object),
          }),
        );
      });

      it('passing IAsiento should create a new form with FormGroup', () => {
        const formGroup = service.createAsientoFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            fila: expect.any(Object),
            columna: expect.any(Object),
            estado: expect.any(Object),
            personaActual: expect.any(Object),
            evento: expect.any(Object),
            ventas: expect.any(Object),
          }),
        );
      });
    });

    describe('getAsiento', () => {
      it('should return NewAsiento for default Asiento initial value', () => {
        const formGroup = service.createAsientoFormGroup(sampleWithNewData);

        const asiento = service.getAsiento(formGroup) as any;

        expect(asiento).toMatchObject(sampleWithNewData);
      });

      it('should return NewAsiento for empty Asiento initial value', () => {
        const formGroup = service.createAsientoFormGroup();

        const asiento = service.getAsiento(formGroup) as any;

        expect(asiento).toMatchObject({});
      });

      it('should return IAsiento', () => {
        const formGroup = service.createAsientoFormGroup(sampleWithRequiredData);

        const asiento = service.getAsiento(formGroup) as any;

        expect(asiento).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IAsiento should not enable id FormControl', () => {
        const formGroup = service.createAsientoFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewAsiento should disable id FormControl', () => {
        const formGroup = service.createAsientoFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
