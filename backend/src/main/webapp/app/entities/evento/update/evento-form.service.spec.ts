import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../evento.test-samples';

import { EventoFormService } from './evento-form.service';

describe('Evento Form Service', () => {
  let service: EventoFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EventoFormService);
  });

  describe('Service methods', () => {
    describe('createEventoFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createEventoFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            titulo: expect.any(Object),
            descripcion: expect.any(Object),
            fecha: expect.any(Object),
            hora: expect.any(Object),
            organizador: expect.any(Object),
            presentadores: expect.any(Object),
            cantidadAsientosTotales: expect.any(Object),
            filaAsientos: expect.any(Object),
            columnaAsientos: expect.any(Object),
          }),
        );
      });

      it('passing IEvento should create a new form with FormGroup', () => {
        const formGroup = service.createEventoFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            titulo: expect.any(Object),
            descripcion: expect.any(Object),
            fecha: expect.any(Object),
            hora: expect.any(Object),
            organizador: expect.any(Object),
            presentadores: expect.any(Object),
            cantidadAsientosTotales: expect.any(Object),
            filaAsientos: expect.any(Object),
            columnaAsientos: expect.any(Object),
          }),
        );
      });
    });

    describe('getEvento', () => {
      it('should return NewEvento for default Evento initial value', () => {
        const formGroup = service.createEventoFormGroup(sampleWithNewData);

        const evento = service.getEvento(formGroup) as any;

        expect(evento).toMatchObject(sampleWithNewData);
      });

      it('should return NewEvento for empty Evento initial value', () => {
        const formGroup = service.createEventoFormGroup();

        const evento = service.getEvento(formGroup) as any;

        expect(evento).toMatchObject({});
      });

      it('should return IEvento', () => {
        const formGroup = service.createEventoFormGroup(sampleWithRequiredData);

        const evento = service.getEvento(formGroup) as any;

        expect(evento).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IEvento should not enable id FormControl', () => {
        const formGroup = service.createEventoFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewEvento should disable id FormControl', () => {
        const formGroup = service.createEventoFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
