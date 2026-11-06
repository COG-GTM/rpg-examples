import { Injectable, signal } from '@angular/core';

/** One-shot success message shown above the routed page (e.g. after a save). */
@Injectable({ providedIn: 'root' })
export class FlashService {
  readonly message = signal<string | null>(null);

  show(message: string): void {
    this.message.set(message);
  }

  clear(): void {
    this.message.set(null);
  }
}
