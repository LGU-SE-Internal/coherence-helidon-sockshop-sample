// API route for addresses - maintains backend compatibility
import type { NextApiRequest, NextApiResponse } from 'next';
import { getBackendEndpoints } from '../../utils/backend';
import { isValidPath, sanitizePath } from '../../utils/validation';

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  const endpoints = getBackendEndpoints();
  
  let url = endpoints.addressUrl;
  if (req.url) {
    const urlPath = req.url.replace('/api/addresses', '');
    if (urlPath) {
      // Validate path to prevent SSRF
      const sanitized = sanitizePath(urlPath);
      if (!isValidPath(sanitized)) {
        return res.status(400).json({ error: 'Invalid URL path' });
      }
      url = `${url}${sanitized}`;
    }
  }
  
  try {
    const response = await fetch(url, {
      method: req.method,
      headers: {
        'Content-Type': 'application/json',
      } as HeadersInit,
      body: req.method !== 'GET' && req.method !== 'HEAD' ? JSON.stringify(req.body) : undefined,
    });
    
    const contentType = response.headers.get('content-type');
    if (contentType?.includes('application/json')) {
      const data = await response.json();
      res.status(response.status).json(data);
    } else {
      const data = await response.text();
      res.status(response.status).send(data);
    }
  } catch (error) {
    console.error('Addresses API error:', error);
    res.status(500).json({ error: 'Failed to fetch from user service' });
  }
}
