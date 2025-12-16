import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse, provideHttpClient } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, from, of } from 'rxjs';

import { IEvento } from 'app/entities/evento/evento.model';
import { EventoService } from 'app/entities/evento/service/evento.service';
import { IAsiento } from 'app/entities/asiento/asiento.model';
import { AsientoService } from 'app/entities/asiento/service/asiento.service';
import { IVenta } from '../venta.model';
import { VentaService } from '../service/venta.service';
import { VentaFormService } from './venta-form.service';

import { VentaUpdateComponent } from './venta-update.component';

describe('Venta Management Update Component', () => {
  let comp: VentaUpdateComponent;
  let fixture: ComponentFixture<VentaUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let ventaFormService: VentaFormService;
  let ventaService: VentaService;
  let eventoService: EventoService;
  let asientoService: AsientoService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [VentaUpdateComponent],
      providers: [
        provideHttpClient(),
        FormBuilder,
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
          },
        },
      ],
    })
      .overrideTemplate(VentaUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(VentaUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    ventaFormService = TestBed.inject(VentaFormService);
    ventaService = TestBed.inject(VentaService);
    eventoService = TestBed.inject(EventoService);
    asientoService = TestBed.inject(AsientoService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Evento query and add missing value', () => {
      const venta: IVenta = { id: 27589 };
      const evento: IEvento = { id: 11280 };
      venta.evento = evento;

      const eventoCollection: IEvento[] = [{ id: 11280 }];
      jest.spyOn(eventoService, 'query').mockReturnValue(of(new HttpResponse({ body: eventoCollection })));
      const additionalEventos = [evento];
      const expectedCollection: IEvento[] = [...additionalEventos, ...eventoCollection];
      jest.spyOn(eventoService, 'addEventoToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ venta });
      comp.ngOnInit();

      expect(eventoService.query).toHaveBeenCalled();
      expect(eventoService.addEventoToCollectionIfMissing).toHaveBeenCalledWith(
        eventoCollection,
        ...additionalEventos.map(expect.objectContaining),
      );
      expect(comp.eventosSharedCollection).toEqual(expectedCollection);
    });

    it('should call Asiento query and add missing value', () => {
      const venta: IVenta = { id: 27589 };
      const asientos: IAsiento[] = [{ id: 20063 }];
      venta.asientos = asientos;

      const asientoCollection: IAsiento[] = [{ id: 20063 }];
      jest.spyOn(asientoService, 'query').mockReturnValue(of(new HttpResponse({ body: asientoCollection })));
      const additionalAsientos = [...asientos];
      const expectedCollection: IAsiento[] = [...additionalAsientos, ...asientoCollection];
      jest.spyOn(asientoService, 'addAsientoToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ venta });
      comp.ngOnInit();

      expect(asientoService.query).toHaveBeenCalled();
      expect(asientoService.addAsientoToCollectionIfMissing).toHaveBeenCalledWith(
        asientoCollection,
        ...additionalAsientos.map(expect.objectContaining),
      );
      expect(comp.asientosSharedCollection).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const venta: IVenta = { id: 27589 };
      const evento: IEvento = { id: 11280 };
      venta.evento = evento;
      const asientos: IAsiento = { id: 20063 };
      venta.asientos = [asientos];

      activatedRoute.data = of({ venta });
      comp.ngOnInit();

      expect(comp.eventosSharedCollection).toContainEqual(evento);
      expect(comp.asientosSharedCollection).toContainEqual(asientos);
      expect(comp.venta).toEqual(venta);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IVenta>>();
      const venta = { id: 10395 };
      jest.spyOn(ventaFormService, 'getVenta').mockReturnValue(venta);
      jest.spyOn(ventaService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ venta });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: venta }));
      saveSubject.complete();

      // THEN
      expect(ventaFormService.getVenta).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(ventaService.update).toHaveBeenCalledWith(expect.objectContaining(venta));
      expect(comp.isSaving).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IVenta>>();
      const venta = { id: 10395 };
      jest.spyOn(ventaFormService, 'getVenta').mockReturnValue({ id: null });
      jest.spyOn(ventaService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ venta: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: venta }));
      saveSubject.complete();

      // THEN
      expect(ventaFormService.getVenta).toHaveBeenCalled();
      expect(ventaService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IVenta>>();
      const venta = { id: 10395 };
      jest.spyOn(ventaService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ venta });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(ventaService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareEvento', () => {
      it('should forward to eventoService', () => {
        const entity = { id: 11280 };
        const entity2 = { id: 12252 };
        jest.spyOn(eventoService, 'compareEvento');
        comp.compareEvento(entity, entity2);
        expect(eventoService.compareEvento).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareAsiento', () => {
      it('should forward to asientoService', () => {
        const entity = { id: 20063 };
        const entity2 = { id: 27821 };
        jest.spyOn(asientoService, 'compareAsiento');
        comp.compareAsiento(entity, entity2);
        expect(asientoService.compareAsiento).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
