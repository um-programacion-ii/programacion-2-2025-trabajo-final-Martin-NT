import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import AsientoResolve from './route/asiento-routing-resolve.service';

const asientoRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/asiento.component').then(m => m.AsientoComponent),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/asiento-detail.component').then(m => m.AsientoDetailComponent),
    resolve: {
      asiento: AsientoResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/asiento-update.component').then(m => m.AsientoUpdateComponent),
    resolve: {
      asiento: AsientoResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/asiento-update.component').then(m => m.AsientoUpdateComponent),
    resolve: {
      asiento: AsientoResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default asientoRoute;
