// API route for orders - maintains backend compatibility
import type { NextApiRequest, NextApiResponse } from 'next';
import { getBackendEndpoints } from '../../utils/backend';

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  const endpoints = getBackendEndpoints();
  const { custId } = req.query;
  
  let url = endpoints.ordersUrl;
  if (req.url) {
    const urlPath = req.url.replace('/api/orders', '');
    if (urlPath) {
      url = `${url}${urlPath}`;
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
    console.error('Orders API error:', error);
    res.status(500).json({ error: 'Failed to fetch from orders service' });
  }
}
