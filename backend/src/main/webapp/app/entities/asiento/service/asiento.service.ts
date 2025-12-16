import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IAsiento, NewAsiento } from '../asiento.model';

export type PartialUpdateAsiento = Partial<IAsiento> & Pick<IAsiento, 'id'>;

export type EntityResponseType = HttpResponse<IAsiento>;
export type EntityArrayResponseType = HttpResponse<IAsiento[]>;

@Injectable({ providedIn: 'root' })
export class AsientoService {
  protected readonly http = inject(HttpClient);
  protected readonly applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/asientos');

  create(asiento: NewAsiento): Observable<EntityResponseType> {
    return this.http.post<IAsiento>(this.resourceUrl, asiento, { observe: 'response' });
  }

  update(asiento: IAsiento): Observable<EntityResponseType> {
    return this.http.put<IAsiento>(`${this.resourceUrl}/${this.getAsientoIdentifier(asiento)}`, asiento, { observe: 'response' });
  }

  partialUpdate(asiento: PartialUpdateAsiento): Observable<EntityResponseType> {
    return this.http.patch<IAsiento>(`${this.resourceUrl}/${this.getAsientoIdentifier(asiento)}`, asiento, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IAsiento>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IAsiento[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getAsientoIdentifier(asiento: Pick<IAsiento, 'id'>): number {
    return asiento.id;
  }

  compareAsiento(o1: Pick<IAsiento, 'id'> | null, o2: Pick<IAsiento, 'id'> | null): boolean {
    return o1 && o2 ? this.getAsientoIdentifier(o1) === this.getAsientoIdentifier(o2) : o1 === o2;
  }

  addAsientoToCollectionIfMissing<Type extends Pick<IAsiento, 'id'>>(
    asientoCollection: Type[],
    ...asientosToCheck: (Type | null | undefined)[]
  ): Type[] {
    const asientos: Type[] = asientosToCheck.filter(isPresent);
    if (asientos.length > 0) {
      const asientoCollectionIdentifiers = asientoCollection.map(asientoItem => this.getAsientoIdentifier(asientoItem));
      const asientosToAdd = asientos.filter(asientoItem => {
        const asientoIdentifier = this.getAsientoIdentifier(asientoItem);
        if (asientoCollectionIdentifiers.includes(asientoIdentifier)) {
          return false;
        }
        asientoCollectionIdentifiers.push(asientoIdentifier);
        return true;
      });
      return [...asientosToAdd, ...asientoCollection];
    }
    return asientoCollection;
  }
}
