import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { AsientoDetailComponent } from './asiento-detail.component';

describe('Asiento Management Detail Component', () => {
  let comp: AsientoDetailComponent;
  let fixture: ComponentFixture<AsientoDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AsientoDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              loadComponent: () => import('./asiento-detail.component').then(m => m.AsientoDetailComponent),
              resolve: { asiento: () => of({ id: 20063 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(AsientoDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AsientoDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('should load asiento on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', AsientoDetailComponent);

      // THEN
      expect(instance.asiento()).toEqual(expect.objectContaining({ id: 20063 }));
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
