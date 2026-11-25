import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../auth/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let logoutSpy: jasmine.Spy;
  let tokenSignal: ReturnType<typeof signal<string | null>>;

  beforeEach(() => {
    logoutSpy = jasmine.createSpy('logout');
    tokenSignal = signal<string | null>('jwt-123');

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: {
            token: tokenSignal,
            logout: logoutSpy,
          },
        },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('attaches Bearer token when available', () => {
    http.get('/api/loans/my').subscribe();

    const req = httpMock.expectOne('/api/loans/my');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-123');
    req.flush([]);
  });

  it('calls logout on 401 responses for protected endpoints', () => {
    http.get('/api/loans/my').subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/loans/my');
    req.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(logoutSpy).toHaveBeenCalledTimes(1);
  });

  it('does not call logout on 401 from /auth/login', () => {
    tokenSignal.set(null);
    http.post('/api/auth/login', {}).subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(logoutSpy).not.toHaveBeenCalled();
  });
});
