// API route for tags - maintains backend compatibility
import type { NextApiRequest, NextApiResponse } from 'next';
import { getBackendEndpoints } from '../../utils/backend';

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  const endpoints = getBackendEndpoints();
  
  try {
    const response = await fetch(endpoints.tagsUrl, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      } as HeadersInit,
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
    console.error('Tags API error:', error);
    res.status(500).json({ error: 'Failed to fetch tags from catalogue service' });
  }
}
