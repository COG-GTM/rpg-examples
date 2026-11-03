import { FormControl } from '@angular/forms';
import { dniIsValid, dniValidator } from './dni.validator';

describe('dniValidator (mirrors CLIESRV dniIsValid)', () => {
  it('accepts 1000000..99999999 only', () => {
    expect(dniIsValid(999_999)).toBeFalse();
    expect(dniIsValid(1_000_000)).toBeTrue();
    expect(dniIsValid(99_999_999)).toBeTrue();
    expect(dniIsValid(100_000_000)).toBeFalse();
    expect(dniIsValid(1_234_567.5)).toBeFalse();
  });

  it('reports the legacy message and leaves empty values to required', () => {
    expect(dniValidator(new FormControl(5))).toEqual({ dni: 'DNI not valid' });
    expect(dniValidator(new FormControl(12345678))).toBeNull();
    expect(dniValidator(new FormControl(null))).toBeNull();
  });
});
