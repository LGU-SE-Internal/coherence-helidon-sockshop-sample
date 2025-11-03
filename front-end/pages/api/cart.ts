// API route for cart - maintains backend compatibility
import type { NextApiRequest, NextApiResponse } from 'next';
import { getBackendEndpoints } from '../../utils/backend';
import { isValidId } from '../../utils/validation';

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  const endpoints = getBackendEndpoints();
  const { custId } = req.query;
  
  // Validate custId to prevent SSRF
  if (custId && typeof custId === 'string' && !isValidId(custId)) {
    return res.status(400).json({ error: 'Invalid customer ID format' });
  }
  
  let url = endpoints.cartsUrl;
  if (custId && typeof custId === 'string') {
    url = `${url}/${encodeURIComponent(custId)}/items`;
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
