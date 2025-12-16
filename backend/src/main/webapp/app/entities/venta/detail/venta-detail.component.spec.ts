import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { VentaDetailComponent } from './venta-detail.component';

describe('Venta Management Detail Component', () => {
  let comp: VentaDetailComponent;
  let fixture: ComponentFixture<VentaDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VentaDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              loadComponent: () => import('./venta-detail.component').then(m => m.VentaDetailComponent),
              resolve: { venta: () => of({ id: 10395 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(VentaDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(VentaDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('should load venta on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', VentaDetailComponent);

      // THEN
      expect(instance.venta()).toEqual(expect.objectContaining({ id: 10395 }));
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
