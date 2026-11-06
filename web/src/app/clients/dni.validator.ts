import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const DNI_MIN_EXCLUSIVE = 999999;
export const DNI_MAX_EXCLUSIVE = 100000000;
export const DNI_RANGE_MESSAGE = 'DNI must be between 1,000,000 and 99,999,999';
export const NAME_MAX_LENGTH = 30;

/** Mirrors the legacy RPG `dniIsValid`: `dni > 999999 and dni < 100000000`. */
export function dniIsValid(dni: number): boolean {
  return Number.isInteger(dni) && dni > DNI_MIN_EXCLUSIVE && dni < DNI_MAX_EXCLUSIVE;
}

/** DNI: digits only and inside the legacy range. Empty values are left to `Validators.required`. */
export const dniValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const raw = control.value;
  if (raw === null || raw === undefined || raw === '') {
    return null;
  }
  const text = String(raw).trim();
  if (!/^\d+$/.test(text) || !dniIsValid(Number(text))) {
    return { dni: DNI_RANGE_MESSAGE };
  }
  return null;
};

/** Rejects whitespace-only strings (`Validators.required` accepts them). */
export const notBlankValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const raw = control.value;
  return typeof raw === 'string' && raw.length > 0 && raw.trim().length === 0 ? { blank: true } : null;
};
