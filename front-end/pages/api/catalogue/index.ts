// API route for catalogue - maintains backend compatibility
import type { NextApiRequest, NextApiResponse } from 'next';
import { getBackendEndpoints } from '../../../utils/backend';
import { isValidPath, sanitizePath } from '../../../utils/validation';

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  const endpoints = getBackendEndpoints();
  
  let urlPath = req.url?.replace('/api/catalogue', '') || '';
  if (urlPath) {
    // Validate path to prevent SSRF
    const sanitized = sanitizePath(urlPath);
    if (!isValidPath(sanitized)) {
      return res.status(400).json({ error: 'Invalid URL path' });
    }
    urlPath = sanitized;
  }
  
  const url = `${endpoints.catalogueUrl}${urlPath}`;
  
  try {
    const response = await fetch(url, {
      method: req.method,
      headers: {
        'Content-Type': 'application/json',
      } as HeadersInit,
    });
    
    const data = await response.text();
    res.status(response.status).send(data);
  } catch (error) {
    console.error('Catalogue API error:', error);
    res.status(500).json({ error: 'Failed to fetch from catalogue service' });
  }
}
