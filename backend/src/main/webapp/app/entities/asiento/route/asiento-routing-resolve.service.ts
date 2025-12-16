import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { EMPTY, Observable, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IAsiento } from '../asiento.model';
import { AsientoService } from '../service/asiento.service';

const asientoResolve = (route: ActivatedRouteSnapshot): Observable<null | IAsiento> => {
  const id = route.params.id;
  if (id) {
    return inject(AsientoService)
      .find(id)
      .pipe(
        mergeMap((asiento: HttpResponse<IAsiento>) => {
          if (asiento.body) {
            return of(asiento.body);
          }
          inject(Router).navigate(['404']);
          return EMPTY;
        }),
      );
  }
  return of(null);
};

export default asientoResolve;
