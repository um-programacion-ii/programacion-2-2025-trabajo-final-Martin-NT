import { IAsiento, NewAsiento } from './asiento.model';

export const sampleWithRequiredData: IAsiento = {
  id: 13756,
  fila: 29094,
  columna: 15932,
  estado: 'BLOQUEADO',
};

export const sampleWithPartialData: IAsiento = {
  id: 7360,
  fila: 4314,
  columna: 32050,
  estado: 'BLOQUEADO',
  personaActual: 'middle yuck',
};

export const sampleWithFullData: IAsiento = {
  id: 26443,
  fila: 25934,
  columna: 7195,
  estado: 'BLOQUEADO',
  personaActual: 'pish than concerning',
};

export const sampleWithNewData: NewAsiento = {
  fila: 22982,
  columna: 11958,
  estado: 'OCUPADO',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
