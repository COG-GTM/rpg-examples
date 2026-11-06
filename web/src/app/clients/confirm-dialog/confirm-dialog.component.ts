import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-confirm-dialog',
  template: `
    <div class="backdrop" (click)="cancelled.emit()">
      <div
        class="dialog"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        (click)="$event.stopPropagation()"
      >
        <h2 id="confirm-title">{{ title() }}</h2>
        <p>{{ message() }}</p>
        <div class="actions">
          <button type="button" class="btn" (click)="cancelled.emit()" [disabled]="busy()">Cancel</button>
          <button type="button" class="btn btn-danger" (click)="confirmed.emit()" [disabled]="busy()">
            {{ busy() ? 'Working…' : confirmLabel() }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: `
    .backdrop {
      position: fixed;
      inset: 0;
      background: rgba(17, 24, 39, 0.45);
      display: grid;
      place-items: center;
      z-index: 10;
    }
    .dialog {
      background: #fff;
      border-radius: 8px;
      padding: 1.5rem;
      width: min(420px, 92vw);
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.25);
    }
    h2 {
      margin: 0 0 0.5rem;
      font-size: 1.15rem;
    }
    .actions {
      display: flex;
      justify-content: flex-end;
      gap: 0.5rem;
      margin-top: 1.25rem;
    }
  `,
})
export class ConfirmDialogComponent {
  readonly title = input('Are you sure?');
  readonly message = input.required<string>();
  readonly confirmLabel = input('Confirm');
  readonly busy = input(false);
  readonly confirmed = output<void>();
  readonly cancelled = output<void>();
}
