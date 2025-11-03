// API route for register - maintains backend compatibility
import type { NextApiRequest, NextApiResponse } from 'next';
import { getBackendEndpoints } from '../../utils/backend';

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  const endpoints = getBackendEndpoints();
  
  try {
    const response = await fetch(endpoints.registerUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      } as HeadersInit,
      body: JSON.stringify(req.body),
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
    console.error('Register API error:', error);
    res.status(500).json({ error: 'Failed to register with user service' });
  }
}
