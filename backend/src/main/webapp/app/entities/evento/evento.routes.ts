import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import EventoResolve from './route/evento-routing-resolve.service';

const eventoRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/evento.component').then(m => m.EventoComponent),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/evento-detail.component').then(m => m.EventoDetailComponent),
    resolve: {
      evento: EventoResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/evento-update.component').then(m => m.EventoUpdateComponent),
    resolve: {
      evento: EventoResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/evento-update.component').then(m => m.EventoUpdateComponent),
    resolve: {
      evento: EventoResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default eventoRoute;
