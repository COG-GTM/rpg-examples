import { FormControl } from '@angular/forms';

import { DNI_RANGE_MESSAGE, dniIsValid, dniValidator, notBlankValidator } from './dni.validator';

describe('dniValidator (legacy dniIsValid: dni > 999999 and dni < 100000000)', () => {
  const check = (value: unknown) => dniValidator(new FormControl(value));

  it('rejects 999999 (lower bound is exclusive)', () => {
    expect(check('999999')).toEqual({ dni: DNI_RANGE_MESSAGE });
    expect(dniIsValid(999999)).toBeFalse();
  });

  it('accepts 1000000', () => {
    expect(check('1000000')).toBeNull();
    expect(dniIsValid(1000000)).toBeTrue();
  });

  it('accepts 99999999', () => {
    expect(check('99999999')).toBeNull();
    expect(dniIsValid(99999999)).toBeTrue();
  });

  it('rejects 100000000 (upper bound is exclusive)', () => {
    expect(check('100000000')).toEqual({ dni: DNI_RANGE_MESSAGE });
    expect(dniIsValid(100000000)).toBeFalse();
  });

  it('accepts numeric values as well as strings', () => {
    expect(check(12345678)).toBeNull();
    expect(check(999999)).toEqual({ dni: DNI_RANGE_MESSAGE });
  });

  it('rejects non-digits, signs and decimals', () => {
    for (const value of ['12a45678', '-1234567', '1234567.5', '1e7', ' 12 345678']) {
      expect(check(value)).withContext(value).toEqual({ dni: DNI_RANGE_MESSAGE });
    }
  });

  it('leaves empty values to Validators.required', () => {
    expect(check('')).toBeNull();
    expect(check(null)).toBeNull();
  });

  it('uses the expected message', () => {
    expect(DNI_RANGE_MESSAGE).toBe('DNI must be between 1,000,000 and 99,999,999');
  });
});

describe('notBlankValidator', () => {
  it('rejects whitespace-only names', () => {
    expect(notBlankValidator(new FormControl('   '))).toEqual({ blank: true });
  });

  it('accepts names with content and leaves empty to required', () => {
    expect(notBlankValidator(new FormControl(' Ana '))).toBeNull();
    expect(notBlankValidator(new FormControl(''))).toBeNull();
  });
});
