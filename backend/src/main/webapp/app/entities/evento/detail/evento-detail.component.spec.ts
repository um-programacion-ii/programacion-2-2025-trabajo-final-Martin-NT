import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { EventoDetailComponent } from './evento-detail.component';

describe('Evento Management Detail Component', () => {
  let comp: EventoDetailComponent;
  let fixture: ComponentFixture<EventoDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EventoDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              loadComponent: () => import('./evento-detail.component').then(m => m.EventoDetailComponent),
              resolve: { evento: () => of({ id: 11280 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(EventoDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EventoDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('should load evento on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', EventoDetailComponent);

      // THEN
      expect(instance.evento()).toEqual(expect.objectContaining({ id: 11280 }));
    });
  });

  describe('PreviousState', () => {
    it('should navigate to previous state', () => {
      jest.spyOn(window.history, 'back');
      comp.previousState();
      expect(window.history.back).toHaveBeenCalled();
    });
  });
});
