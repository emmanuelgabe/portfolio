import { randomUuid } from './uuid.utils';

const UUID_V4_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

describe('randomUuid', () => {
  // ========== Secure context ==========

  it('should_delegateToCryptoRandomUuid_when_randomUuidCalledInSecureContext', () => {
    // Arrange
    const expected = '3f2b8c1e-9d4a-4f6b-8e2c-1a7d5b9c0e34';
    spyOn(crypto, 'randomUUID').and.returnValue(expected);

    // Act
    const result = randomUuid();

    // Assert
    expect(result).toBe(expected);
  });

  // ========== Insecure context fallback ==========

  it('should_returnVersion4Uuid_when_randomUuidCalledWithoutCryptoRandomUuid', () => {
    // Arrange: randomUUID lives on Crypto.prototype, shadow it as an insecure context does
    Object.defineProperty(crypto, 'randomUUID', { value: undefined, configurable: true });

    try {
      // Act
      const first = randomUuid();
      const second = randomUuid();

      // Assert
      expect(first).toMatch(UUID_V4_PATTERN);
      expect(second).toMatch(UUID_V4_PATTERN);
      expect(first).not.toBe(second);
    } finally {
      Reflect.deleteProperty(crypto, 'randomUUID');
    }
  });
});
