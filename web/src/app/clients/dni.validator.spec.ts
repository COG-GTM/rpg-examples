import { FormControl } from '@angular/forms';

import {
  DNI_RANGE_MESSAGE,
  dniErrorMessage,
  dniValidator,
  dniValidators,
  isValidDni,
  nameErrorMessage,
  nameValidators,
} from './dni.validator';

describe('dniValidator', () => {
  const validate = (value: unknown) => dniValidator(new FormControl(value));

  it('rejects 999999 (lower boundary, exclusive)', () => {
    expect(validate(999999)).toEqual(jasmine.objectContaining({ dniRange: jasmine.anything() }));
    expect(validate('999999')?.['dniRange']).toBeTruthy();
  });

  it('accepts 1000000 (smallest valid DNI)', () => {
    expect(validate(1000000)).toBeNull();
    expect(validate('1000000')).toBeNull();
  });

  it('accepts 99999999 (largest valid DNI)', () => {
    expect(validate(99999999)).toBeNull();
    expect(validate('99999999')).toBeNull();
  });

  it('rejects 100000000 (upper boundary, exclusive)', () => {
    expect(validate(100000000)?.['dniRange']).toBeTruthy();
    expect(validate('100000000')?.['dniRange']).toBeTruthy();
  });

  it('rejects non-integers', () => {
    expect(validate('12345.67')?.['dniInteger']).toBeTruthy();
    expect(validate('12ab5678')?.['dniInteger']).toBeTruthy();
    expect(validate('-1234567')?.['dniInteger']).toBeTruthy();
    expect(validate('1e7')?.['dniInteger']).toBeTruthy();
  });

  it('leaves empty values to Validators.required', () => {
    expect(validate('')).toBeNull();
    expect(validate(null)).toBeNull();
    const control = new FormControl('', dniValidators);
    expect(control.errors?.['required']).toBeTrue();
    expect(dniErrorMessage(control)).toBe('DNI is required');
  });

  it('reports the RPG range message for out-of-range values', () => {
    const control = new FormControl('999999', dniValidators);
    expect(dniErrorMessage(control)).toBe(DNI_RANGE_MESSAGE);
    expect(DNI_RANGE_MESSAGE).toBe('DNI must be between 1,000,000 and 99,999,999');
  });

  it('isValidDni mirrors the same boundaries', () => {
    expect(isValidDni(999999)).toBeFalse();
    expect(isValidDni(1000000)).toBeTrue();
    expect(isValidDni(99999999)).toBeTrue();
    expect(isValidDni(100000000)).toBeFalse();
  });
});

describe('nameValidators', () => {
  const control = (value: string) => new FormControl(value, nameValidators);

  it('requires a non-blank name', () => {
    expect(nameErrorMessage(control(''))).toBe('Name is required');
    expect(nameErrorMessage(control('   '))).toBe('Name is required');
  });

  it('allows up to 30 characters', () => {
    expect(control('A'.repeat(30)).valid).toBeTrue();
    expect(nameErrorMessage(control('A'.repeat(31)))).toBe('Name must be at most 30 characters');
  });
});
