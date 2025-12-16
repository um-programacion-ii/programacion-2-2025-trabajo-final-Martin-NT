import { IEvento } from 'app/entities/evento/evento.model';
import { IVenta } from 'app/entities/venta/venta.model';
import { AsientoEstado } from 'app/entities/enumerations/asiento-estado.model';

export interface IAsiento {
  id: number;
  fila?: number | null;
  columna?: number | null;
  estado?: keyof typeof AsientoEstado | null;
  personaActual?: string | null;
  evento?: Pick<IEvento, 'id' | 'titulo'> | null;
  ventas?: Pick<IVenta, 'id'>[] | null;
}

export type NewAsiento = Omit<IAsiento, 'id'> & { id: null };
