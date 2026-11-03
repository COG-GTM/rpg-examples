import { AbstractControl, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';

/** Mirrors the RPG dniIsValid rule: dni > 999999 and dni < 100000000. */
export const DNI_MIN = 1_000_000;
export const DNI_MAX = 99_999_999;
export const DNI_RANGE_MESSAGE = 'DNI must be between 1,000,000 and 99,999,999';
export const NAME_MAX_LENGTH = 30;

export function isValidDni(value: unknown): boolean {
  if (value === null || value === undefined) return false;
  const text = String(value).trim();
  if (!/^\d+$/.test(text)) return false;
  const dni = Number(text);
  return Number.isSafeInteger(dni) && dni >= DNI_MIN && dni <= DNI_MAX;
}

/**
 * Reusable DNI validator. Empty values are left to `Validators.required`;
 * non-integers report `dniInteger`, out-of-range integers report `dniRange`.
 */
export const dniValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  if (value === null || value === undefined || String(value).trim() === '') return null;
  const text = String(value).trim();
  if (!/^\d+$/.test(text)) {
    return { dniInteger: { message: 'DNI must be a whole number' } };
  }
  if (!isValidDni(text)) {
    return { dniRange: { min: DNI_MIN, max: DNI_MAX, message: DNI_RANGE_MESSAGE } };
  }
  return null;
};

/** Rejects names that are empty or only whitespace. */
export const notBlankValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  if (typeof value === 'string' && value.length > 0 && value.trim().length === 0) {
    return { blank: true };
  }
  return null;
};

export const dniValidators: ValidatorFn[] = [Validators.required, dniValidator];
export const nameValidators: ValidatorFn[] = [
  Validators.required,
  notBlankValidator,
  Validators.maxLength(NAME_MAX_LENGTH),
];

export function dniErrorMessage(control: AbstractControl | null): string | null {
  if (!control || !control.errors) return null;
  if (control.errors['required']) return 'DNI is required';
  if (control.errors['dniInteger']) return 'DNI must be a whole number';
  if (control.errors['dniRange']) return DNI_RANGE_MESSAGE;
  return null;
}

export function nameErrorMessage(control: AbstractControl | null): string | null {
  if (!control || !control.errors) return null;
  if (control.errors['required'] || control.errors['blank']) return 'Name is required';
  if (control.errors['maxlength']) return `Name must be at most ${NAME_MAX_LENGTH} characters`;
  return null;
}
