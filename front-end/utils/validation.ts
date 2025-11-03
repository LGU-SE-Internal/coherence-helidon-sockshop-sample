// Validation utilities for API routes to prevent SSRF and injection attacks

/**
 * Validates that a URL path is safe and doesn't contain suspicious patterns
 * Prevents SSRF attacks by ensuring the path doesn't try to access other hosts
 */
export function isValidPath(path: string): boolean {
  if (!path) return true; // Empty path is valid
  
  // Reject paths that try to access parent directories or other protocols
  const dangerousPatterns = [
    '../',
    '://',
    'file:',
    'javascript:',
    'data:',
    '@',
    '\\',
  ];
  
  const lowerPath = path.toLowerCase();
  return !dangerousPatterns.some(pattern => lowerPath.includes(pattern));
}

/**
 * Validates that an ID parameter contains only safe characters
 * Allows alphanumeric, hyphens, and underscores
 */
export function isValidId(id: string): boolean {
  return /^[a-zA-Z0-9_-]+$/.test(id);
}

/**
 * Sanitizes a path by removing any query parameters and fragments
 */
export function sanitizePath(path: string): string {
  return path.split('?')[0].split('#')[0];
}
