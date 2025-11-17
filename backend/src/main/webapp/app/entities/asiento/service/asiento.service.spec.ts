import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { IAsiento } from '../asiento.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../asiento.test-samples';

import { AsientoService } from './asiento.service';

const requireRestSample: IAsiento = {
  ...sampleWithRequiredData,
};

describe('Asiento Service', () => {
  let service: AsientoService;
  let httpMock: HttpTestingController;
  let expectedResult: IAsiento | IAsiento[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(AsientoService);
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

    it('should create a Asiento', () => {
      const asiento = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(asiento).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Asiento', () => {
      const asiento = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(asiento).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Asiento', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Asiento', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Asiento', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addAsientoToCollectionIfMissing', () => {
      it('should add a Asiento to an empty array', () => {
        const asiento: IAsiento = sampleWithRequiredData;
        expectedResult = service.addAsientoToCollectionIfMissing([], asiento);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(asiento);
      });

      it('should not add a Asiento to an array that contains it', () => {
        const asiento: IAsiento = sampleWithRequiredData;
        const asientoCollection: IAsiento[] = [
          {
            ...asiento,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addAsientoToCollectionIfMissing(asientoCollection, asiento);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Asiento to an array that doesn't contain it", () => {
        const asiento: IAsiento = sampleWithRequiredData;
        const asientoCollection: IAsiento[] = [sampleWithPartialData];
        expectedResult = service.addAsientoToCollectionIfMissing(asientoCollection, asiento);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(asiento);
      });

      it('should add only unique Asiento to an array', () => {
        const asientoArray: IAsiento[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const asientoCollection: IAsiento[] = [sampleWithRequiredData];
        expectedResult = service.addAsientoToCollectionIfMissing(asientoCollection, ...asientoArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const asiento: IAsiento = sampleWithRequiredData;
        const asiento2: IAsiento = sampleWithPartialData;
        expectedResult = service.addAsientoToCollectionIfMissing([], asiento, asiento2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(asiento);
        expect(expectedResult).toContain(asiento2);
      });

      it('should accept null and undefined values', () => {
        const asiento: IAsiento = sampleWithRequiredData;
        expectedResult = service.addAsientoToCollectionIfMissing([], null, asiento, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(asiento);
      });

      it('should return initial array if no Asiento is added', () => {
        const asientoCollection: IAsiento[] = [sampleWithRequiredData];
        expectedResult = service.addAsientoToCollectionIfMissing(asientoCollection, undefined, null);
        expect(expectedResult).toEqual(asientoCollection);
      });
    });

    describe('compareAsiento', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareAsiento(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 20063 };
        const entity2 = null;

        const compareResult1 = service.compareAsiento(entity1, entity2);
        const compareResult2 = service.compareAsiento(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 20063 };
        const entity2 = { id: 27821 };

        const compareResult1 = service.compareAsiento(entity1, entity2);
        const compareResult2 = service.compareAsiento(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey matches', () => {
        const entity1 = { id: 20063 };
        const entity2 = { id: 20063 };

        const compareResult1 = service.compareAsiento(entity1, entity2);
        const compareResult2 = service.compareAsiento(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
