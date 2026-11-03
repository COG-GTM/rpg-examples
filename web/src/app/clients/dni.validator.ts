import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** CLIESRV dniIsValid: DNI must be > 999999 and < 100000000. */
export const DNI_MIN = 1_000_000;
export const DNI_MAX = 99_999_999;
export const NAME_MAX_LENGTH = 30;

export function dniIsValid(dni: number): boolean {
  return Number.isInteger(dni) && dni > 999_999 && dni < 100_000_000;
}

export const dniValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const raw = control.value;
  if (raw === null || raw === undefined || raw === '') {
    return null; // let Validators.required report emptiness
  }
  return dniIsValid(Number(raw)) ? null : { dni: 'DNI not valid' };
};
