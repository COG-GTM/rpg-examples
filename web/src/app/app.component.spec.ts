import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AppComponent } from './app.component';
import { NotificationService } from './clients/notification.service';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('renders the app shell', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.brand')?.textContent).toContain(
      'Client Management',
    );
  });

  it('renders toasts from the NotificationService', () => {
    const fixture = TestBed.createComponent(AppComponent);
    TestBed.inject(NotificationService).error('DNI already exists');
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.toast-error')?.textContent).toContain(
      'DNI already exists',
    );
  });
});
