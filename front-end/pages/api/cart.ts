// API route for cart - maintains backend compatibility
import type { NextApiRequest, NextApiResponse } from 'next';
import { getBackendEndpoints } from '../../utils/backend';

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  const endpoints = getBackendEndpoints();
  const { custId } = req.query;
  
  let url = endpoints.cartsUrl;
  if (custId) {
    url = `${url}/${custId}/items`;
  }
  
  try {
    const response = await fetch(url, {
      method: req.method,
      headers: {
        'Content-Type': 'application/json',
      } as HeadersInit,
      body: req.method !== 'GET' ? JSON.stringify(req.body) : undefined,
    });
    
    const data = await response.json();
    res.status(response.status).json(data);
  } catch (error) {
    console.error('Cart API error:', error);
    res.status(500).json({ error: 'Failed to fetch from cart service' });
  }
}
