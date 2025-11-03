// API route for cart - maintains backend compatibility
import type { NextApiRequest, NextApiResponse } from 'next';
import { getBackendEndpoints } from '../../utils/backend';
import { isValidId } from '../../utils/validation';

// Helper to get customer ID from request (from session or query)
function getCustomerId(req: NextApiRequest): string {
  // Check query parameter first
  if (req.query.custId && typeof req.query.custId === 'string') {
    return req.query.custId;
  }
  
  // Check session cookie (logged_in cookie contains customer ID)
  if (req.cookies.logged_in) {
    return req.cookies.logged_in;
  }
  
  // Default for anonymous users
  return 'anonymous';
}

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  const endpoints = getBackendEndpoints();
  const custId = getCustomerId(req);
  
  // Validate custId to prevent SSRF
  if (custId !== 'anonymous' && !isValidId(custId)) {
    return res.status(400).json({ error: 'Invalid customer ID format' });
  }
  
  // Handle different methods
  if (req.method === 'GET') {
    // GET /cart - list items in cart
    const url = `${endpoints.cartsUrl}/${custId}/items`;
    
    try {
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        } as HeadersInit,
      });
      
      const data = await response.json();
      res.status(response.status).json(data);
    } catch (error) {
      console.error('Cart GET error:', error);
      res.status(500).json({ error: 'Failed to fetch cart' });
    }
  } else if (req.method === 'POST') {
    // POST /cart - add item to cart
    const url = `${endpoints.cartsUrl}/${custId}/items`;
    
    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        } as HeadersInit,
        body: JSON.stringify(req.body),
      });
      
      const data = await response.json();
      res.status(response.status).json(data);
    } catch (error) {
      console.error('Cart POST error:', error);
      res.status(500).json({ error: 'Failed to add to cart' });
    }
  } else if (req.method === 'DELETE') {
    // DELETE /cart - delete cart
    const url = `${endpoints.cartsUrl}/${custId}`;
    
    try {
      const response = await fetch(url, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
        } as HeadersInit,
      });
      
      const data = await response.status === 202 ? {} : await response.json();
      res.status(response.status).json(data);
    } catch (error) {
      console.error('Cart DELETE error:', error);
      res.status(500).json({ error: 'Failed to delete cart' });
    }
  } else if (req.method === 'PATCH') {
    // PATCH /cart/:itemId - update item in cart
    const url = `${endpoints.cartsUrl}/${custId}/items`;
    
    try {
      const response = await fetch(url, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
        } as HeadersInit,
        body: JSON.stringify(req.body),
      });
      
      const data = await response.json();
      res.status(response.status).json(data);
    } catch (error) {
      console.error('Cart PATCH error:', error);
      res.status(500).json({ error: 'Failed to update cart' });
    }
  } else {
    res.status(405).json({ error: 'Method not allowed' });
  }
}
