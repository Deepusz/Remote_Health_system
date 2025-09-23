import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiCallerService {
  // Point to your Spring Boot backend
  private baseUrl = `${environment.apiBase}/ts`;

  // Use the same token you put in application.properties (or fetch from a proper auth flow)
  private apiToken = 'my-local-dev-token';

  private headers = new HttpHeaders({
    'Authorization': `Bearer ${this.apiToken}`,
    'Content-Type': 'application/json'
  });

  constructor(private http: HttpClient) {}

  /**
   * Read feeds from backend (ThingSpeak proxy) - returns the raw JSON
   * results: number of points (1..100)
   * field?: optional field number
   */
  getFeeds(results = 10, field?: number): Observable<any> {
    let params = new HttpParams().set('results', String(results));
    if (field) params = params.set('field', String(field));
    return this.http.get(`${this.baseUrl}/feeds`, { headers: this.headers, params });
  }

  /**
   * Periodic polling: returns an Observable that polls every `ms` milliseconds
   * and emits the result of getFeeds(results).
   */
  pollFeeds(results = 10, ms = 5000): Observable<any> {
    return timer(0, ms).pipe(
      switchMap(() => this.getFeeds(results))
    );
  }

  /**
   * Send update (write to ThingSpeak). Expect body: { fields: { field1: val, ... }, status?: 'text' }
   */
  sendUpdate(payload: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/update`, payload, { headers: this.headers });
  }
}
