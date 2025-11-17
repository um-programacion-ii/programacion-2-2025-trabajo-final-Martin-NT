import dayjs from 'dayjs/esm';

export interface IEvento {
  id: number;
  titulo?: string | null;
  descripcion?: string | null;
  fecha?: dayjs.Dayjs | null;
  hora?: string | null;
  organizador?: string | null;
  presentadores?: string | null;
  cantidadAsientosTotales?: number | null;
  filaAsientos?: number | null;
  columnaAsientos?: number | null;
}

export type NewEvento = Omit<IEvento, 'id'> & { id: null };
