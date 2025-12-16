import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { DATE_FORMAT } from 'app/config/input.constants';
import { IEvento } from '../evento.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../evento.test-samples';

import { EventoService, RestEvento } from './evento.service';

const requireRestSample: RestEvento = {
  ...sampleWithRequiredData,
  fecha: sampleWithRequiredData.fecha?.format(DATE_FORMAT),
};

describe('Evento Service', () => {
  let service: EventoService;
  let httpMock: HttpTestingController;
  let expectedResult: IEvento | IEvento[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(EventoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  describe('Service methods', () => {
    it('should find an element', () => {
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.find(123).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should create a Evento', () => {
      const evento = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(evento).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Evento', () => {
      const evento = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(evento).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Evento', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Evento', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Evento', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addEventoToCollectionIfMissing', () => {
      it('should add a Evento to an empty array', () => {
        const evento: IEvento = sampleWithRequiredData;
        expectedResult = service.addEventoToCollectionIfMissing([], evento);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(evento);
      });

      it('should not add a Evento to an array that contains it', () => {
        const evento: IEvento = sampleWithRequiredData;
        const eventoCollection: IEvento[] = [
          {
            ...evento,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addEventoToCollectionIfMissing(eventoCollection, evento);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Evento to an array that doesn't contain it", () => {
        const evento: IEvento = sampleWithRequiredData;
        const eventoCollection: IEvento[] = [sampleWithPartialData];
        expectedResult = service.addEventoToCollectionIfMissing(eventoCollection, evento);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(evento);
      });

      it('should add only unique Evento to an array', () => {
        const eventoArray: IEvento[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const eventoCollection: IEvento[] = [sampleWithRequiredData];
        expectedResult = service.addEventoToCollectionIfMissing(eventoCollection, ...eventoArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const evento: IEvento = sampleWithRequiredData;
        const evento2: IEvento = sampleWithPartialData;
        expectedResult = service.addEventoToCollectionIfMissing([], evento, evento2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(evento);
        expect(expectedResult).toContain(evento2);
      });

      it('should accept null and undefined values', () => {
        const evento: IEvento = sampleWithRequiredData;
        expectedResult = service.addEventoToCollectionIfMissing([], null, evento, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(evento);
      });

      it('should return initial array if no Evento is added', () => {
        const eventoCollection: IEvento[] = [sampleWithRequiredData];
        expectedResult = service.addEventoToCollectionIfMissing(eventoCollection, undefined, null);
        expect(expectedResult).toEqual(eventoCollection);
      });
    });

    describe('compareEvento', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareEvento(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 11280 };
        const entity2 = null;

        const compareResult1 = service.compareEvento(entity1, entity2);
        const compareResult2 = service.compareEvento(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 11280 };
        const entity2 = { id: 12252 };

        const compareResult1 = service.compareEvento(entity1, entity2);
        const compareResult2 = service.compareEvento(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey matches', () => {
        const entity1 = { id: 11280 };
        const entity2 = { id: 11280 };

        const compareResult1 = service.compareEvento(entity1, entity2);
        const compareResult2 = service.compareEvento(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
