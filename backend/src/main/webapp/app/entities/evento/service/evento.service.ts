import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, map } from 'rxjs';

import dayjs from 'dayjs/esm';

import { isPresent } from 'app/core/util/operators';
import { DATE_FORMAT } from 'app/config/input.constants';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IEvento, NewEvento } from '../evento.model';

export type PartialUpdateEvento = Partial<IEvento> & Pick<IEvento, 'id'>;

type RestOf<T extends IEvento | NewEvento> = Omit<T, 'fecha'> & {
  fecha?: string | null;
};

export type RestEvento = RestOf<IEvento>;

export type NewRestEvento = RestOf<NewEvento>;

export type PartialUpdateRestEvento = RestOf<PartialUpdateEvento>;

export type EntityResponseType = HttpResponse<IEvento>;
export type EntityArrayResponseType = HttpResponse<IEvento[]>;

@Injectable({ providedIn: 'root' })
export class EventoService {
  protected readonly http = inject(HttpClient);
  protected readonly applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/eventos');

  create(evento: NewEvento): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(evento);
    return this.http
      .post<RestEvento>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(evento: IEvento): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(evento);
    return this.http
      .put<RestEvento>(`${this.resourceUrl}/${this.getEventoIdentifier(evento)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(evento: PartialUpdateEvento): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(evento);
    return this.http
      .patch<RestEvento>(`${this.resourceUrl}/${this.getEventoIdentifier(evento)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<RestEvento>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<RestEvento[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getEventoIdentifier(evento: Pick<IEvento, 'id'>): number {
    return evento.id;
  }

  compareEvento(o1: Pick<IEvento, 'id'> | null, o2: Pick<IEvento, 'id'> | null): boolean {
    return o1 && o2 ? this.getEventoIdentifier(o1) === this.getEventoIdentifier(o2) : o1 === o2;
  }

  addEventoToCollectionIfMissing<Type extends Pick<IEvento, 'id'>>(
    eventoCollection: Type[],
    ...eventosToCheck: (Type | null | undefined)[]
  ): Type[] {
    const eventos: Type[] = eventosToCheck.filter(isPresent);
    if (eventos.length > 0) {
      const eventoCollectionIdentifiers = eventoCollection.map(eventoItem => this.getEventoIdentifier(eventoItem));
      const eventosToAdd = eventos.filter(eventoItem => {
        const eventoIdentifier = this.getEventoIdentifier(eventoItem);
        if (eventoCollectionIdentifiers.includes(eventoIdentifier)) {
          return false;
        }
        eventoCollectionIdentifiers.push(eventoIdentifier);
        return true;
      });
      return [...eventosToAdd, ...eventoCollection];
    }
    return eventoCollection;
  }

  protected convertDateFromClient<T extends IEvento | NewEvento | PartialUpdateEvento>(evento: T): RestOf<T> {
    return {
      ...evento,
      fecha: evento.fecha?.format(DATE_FORMAT) ?? null,
    };
  }

  protected convertDateFromServer(restEvento: RestEvento): IEvento {
    return {
      ...restEvento,
      fecha: restEvento.fecha ? dayjs(restEvento.fecha) : undefined,
    };
  }

  protected convertResponseFromServer(res: HttpResponse<RestEvento>): HttpResponse<IEvento> {
    return res.clone({
      body: res.body ? this.convertDateFromServer(res.body) : null,
    });
  }

  protected convertResponseArrayFromServer(res: HttpResponse<RestEvento[]>): HttpResponse<IEvento[]> {
    return res.clone({
      body: res.body ? res.body.map(item => this.convertDateFromServer(item)) : null,
    });
  }
}
