// API route for catalogue - maintains backend compatibility
import type { NextApiRequest, NextApiResponse } from 'next';
import { getBackendEndpoints } from '../../../utils/backend';

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  const endpoints = getBackendEndpoints();
  const url = `${endpoints.catalogueUrl}${req.url?.replace('/api/catalogue', '') || ''}`;
  
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
