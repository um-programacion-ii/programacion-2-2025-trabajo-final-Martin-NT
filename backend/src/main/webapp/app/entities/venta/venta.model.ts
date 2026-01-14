import dayjs from 'dayjs/esm';
import { IEvento } from 'app/entities/evento/evento.model';
import { IAsiento } from 'app/entities/asiento/asiento.model';
import { VentaEstado } from 'app/entities/enumerations/venta-estado.model';

export interface IVenta {
  id: number;
  fechaVenta?: dayjs.Dayjs | null;
  estado?: keyof typeof VentaEstado | null;
  descripcion?: string | null;
  precioVenta?: number | null;
  cantidadAsientos?: number | null;
  evento?: Pick<IEvento, 'id' | 'titulo'> | null;
  asientos?: Pick<IAsiento, 'id'>[] | null;
}

export type NewVenta = Omit<IVenta, 'id'> & { id: null };
