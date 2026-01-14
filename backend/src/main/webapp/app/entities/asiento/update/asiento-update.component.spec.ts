import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse, provideHttpClient } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, from, of } from 'rxjs';

import { IEvento } from 'app/entities/evento/evento.model';
import { EventoService } from 'app/entities/evento/service/evento.service';
import { IVenta } from 'app/entities/venta/venta.model';
import { VentaService } from 'app/entities/venta/service/venta.service';
import { IAsiento } from '../asiento.model';
import { AsientoService } from '../service/asiento.service';
import { AsientoFormService } from './asiento-form.service';

import { AsientoUpdateComponent } from './asiento-update.component';

describe('Asiento Management Update Component', () => {
  let comp: AsientoUpdateComponent;
  let fixture: ComponentFixture<AsientoUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let asientoFormService: AsientoFormService;
  let asientoService: AsientoService;
  let eventoService: EventoService;
  let ventaService: VentaService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AsientoUpdateComponent],
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
      .overrideTemplate(AsientoUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(AsientoUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    asientoFormService = TestBed.inject(AsientoFormService);
    asientoService = TestBed.inject(AsientoService);
    eventoService = TestBed.inject(EventoService);
    ventaService = TestBed.inject(VentaService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Evento query and add missing value', () => {
      const asiento: IAsiento = { id: 27821 };
      const evento: IEvento = { id: 11280 };
      asiento.evento = evento;

      const eventoCollection: IEvento[] = [{ id: 11280 }];
      jest.spyOn(eventoService, 'query').mockReturnValue(of(new HttpResponse({ body: eventoCollection })));
      const additionalEventos = [evento];
      const expectedCollection: IEvento[] = [...additionalEventos, ...eventoCollection];
      jest.spyOn(eventoService, 'addEventoToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ asiento });
      comp.ngOnInit();

      expect(eventoService.query).toHaveBeenCalled();
      expect(eventoService.addEventoToCollectionIfMissing).toHaveBeenCalledWith(
        eventoCollection,
        ...additionalEventos.map(expect.objectContaining),
      );
      expect(comp.eventosSharedCollection).toEqual(expectedCollection);
    });

    it('should call Venta query and add missing value', () => {
      const asiento: IAsiento = { id: 27821 };
      const ventas: IVenta[] = [{ id: 10395 }];
      asiento.ventas = ventas;

      const ventaCollection: IVenta[] = [{ id: 10395 }];
      jest.spyOn(ventaService, 'query').mockReturnValue(of(new HttpResponse({ body: ventaCollection })));
      const additionalVentas = [...ventas];
      const expectedCollection: IVenta[] = [...additionalVentas, ...ventaCollection];
      jest.spyOn(ventaService, 'addVentaToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ asiento });
      comp.ngOnInit();

      expect(ventaService.query).toHaveBeenCalled();
      expect(ventaService.addVentaToCollectionIfMissing).toHaveBeenCalledWith(
        ventaCollection,
        ...additionalVentas.map(expect.objectContaining),
      );
      expect(comp.ventasSharedCollection).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const asiento: IAsiento = { id: 27821 };
      const evento: IEvento = { id: 11280 };
      asiento.evento = evento;
      const ventas: IVenta = { id: 10395 };
      asiento.ventas = [ventas];

      activatedRoute.data = of({ asiento });
      comp.ngOnInit();

      expect(comp.eventosSharedCollection).toContainEqual(evento);
      expect(comp.ventasSharedCollection).toContainEqual(ventas);
      expect(comp.asiento).toEqual(asiento);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IAsiento>>();
      const asiento = { id: 20063 };
      jest.spyOn(asientoFormService, 'getAsiento').mockReturnValue(asiento);
      jest.spyOn(asientoService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ asiento });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: asiento }));
      saveSubject.complete();

      // THEN
      expect(asientoFormService.getAsiento).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(asientoService.update).toHaveBeenCalledWith(expect.objectContaining(asiento));
      expect(comp.isSaving).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IAsiento>>();
      const asiento = { id: 20063 };
      jest.spyOn(asientoFormService, 'getAsiento').mockReturnValue({ id: null });
      jest.spyOn(asientoService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ asiento: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: asiento }));
      saveSubject.complete();

      // THEN
      expect(asientoFormService.getAsiento).toHaveBeenCalled();
      expect(asientoService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IAsiento>>();
      const asiento = { id: 20063 };
      jest.spyOn(asientoService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ asiento });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(asientoService.update).toHaveBeenCalled();
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

    describe('compareVenta', () => {
      it('should forward to ventaService', () => {
        const entity = { id: 10395 };
        const entity2 = { id: 27589 };
        jest.spyOn(ventaService, 'compareVenta');
        comp.compareVenta(entity, entity2);
        expect(ventaService.compareVenta).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
