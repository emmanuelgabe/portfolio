/**
 * UUID generation that also works outside secure contexts.
 *
 * crypto.randomUUID is only exposed on HTTPS or localhost pages. The staging site is
 * served over plain HTTP through Tailscale, so fall back to crypto.getRandomValues,
 * which is available in every context.
 */

/**
 * Generate a random RFC 4122 version 4 UUID.
 * @returns UUID string such as '3f2b8c1e-9d4a-4f6b-8e2c-1a7d5b9c0e34'
 */
export function randomUuid(): string {
  if (typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  // Version 4 in the high nibble of byte 6, RFC 4122 variant (10xx) in byte 8
  bytes[6] = (bytes[6] % 16) + 0x40;
  bytes[8] = (bytes[8] % 64) + 0x80;
  const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}
