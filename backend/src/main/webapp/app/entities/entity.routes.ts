import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: 'authority',
    data: { pageTitle: 'backendCatedraApp.adminAuthority.home.title' },
    loadChildren: () => import('./admin/authority/authority.routes'),
  },
  {
    path: 'evento',
    data: { pageTitle: 'backendCatedraApp.evento.home.title' },
    loadChildren: () => import('./evento/evento.routes'),
  },
  {
    path: 'asiento',
    data: { pageTitle: 'backendCatedraApp.asiento.home.title' },
    loadChildren: () => import('./asiento/asiento.routes'),
  },
  {
    path: 'venta',
    data: { pageTitle: 'backendCatedraApp.venta.home.title' },
    loadChildren: () => import('./venta/venta.routes'),
  },
  /* jhipster-needle-add-entity-route - JHipster will add entity modules routes here */
];

export default routes;
