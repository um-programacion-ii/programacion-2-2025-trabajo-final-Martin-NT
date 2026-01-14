import dayjs from 'dayjs/esm';

import { IEvento, NewEvento } from './evento.model';

export const sampleWithRequiredData: IEvento = {
  id: 21654,
  titulo: 'inwardly chromakey',
  fecha: dayjs('2025-11-13'),
  hora: '08:19:00',
  cantidadAsientosTotales: 28310,
  filaAsientos: 18183,
  columnaAsientos: 26030,
};

export const sampleWithPartialData: IEvento = {
  id: 7704,
  titulo: 'wear',
  descripcion: 'larva',
  fecha: dayjs('2025-11-14'),
  hora: '13:15:00',
  organizador: 'despite cultivated',
  cantidadAsientosTotales: 3038,
  filaAsientos: 25384,
  columnaAsientos: 30648,
};

export const sampleWithFullData: IEvento = {
  id: 26545,
  titulo: 'outlaw upon',
  descripcion: 'duh labourer pleasure',
  fecha: dayjs('2025-11-13'),
  hora: '09:35:00',
  organizador: 'only populist hard-to-find',
  presentadores: 'immediately hourly that',
  cantidadAsientosTotales: 26854,
  filaAsientos: 16793,
  columnaAsientos: 25765,
};

export const sampleWithNewData: NewEvento = {
  titulo: 'wherever nor',
  fecha: dayjs('2025-11-13'),
  hora: '10:47:00',
  cantidadAsientosTotales: 26297,
  filaAsientos: 10587,
  columnaAsientos: 26857,
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
