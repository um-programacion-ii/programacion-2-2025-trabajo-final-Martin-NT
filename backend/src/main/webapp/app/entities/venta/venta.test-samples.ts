import dayjs from 'dayjs/esm';

import { IVenta, NewVenta } from './venta.model';

export const sampleWithRequiredData: IVenta = {
  id: 21245,
  fechaVenta: dayjs('2025-11-14'),
  estado: 'PENDIENTE',
  precioVenta: 28896.61,
  cantidadAsientos: 4081,
};

export const sampleWithPartialData: IVenta = {
  id: 28926,
  fechaVenta: dayjs('2025-11-14'),
  estado: 'PENDIENTE',
  descripcion: 'pack',
  precioVenta: 462.49,
  cantidadAsientos: 13647,
};

export const sampleWithFullData: IVenta = {
  id: 8723,
  fechaVenta: dayjs('2025-11-14'),
  estado: 'FALLIDA',
  descripcion: 'misjudge duh',
  precioVenta: 31953.1,
  cantidadAsientos: 10721,
};

export const sampleWithNewData: NewVenta = {
  fechaVenta: dayjs('2025-11-14'),
  estado: 'EXITOSA',
  precioVenta: 4592.68,
  cantidadAsientos: 16357,
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
