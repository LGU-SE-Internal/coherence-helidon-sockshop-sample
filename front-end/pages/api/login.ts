// API route for login - maintains backend compatibility
import type { NextApiRequest, NextApiResponse } from 'next';
import { getBackendEndpoints } from '../../utils/backend';

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  const endpoints = getBackendEndpoints();
  
  // Original Express app used GET for login
  if (req.method !== 'GET') {
    return res.status(405).json({ error: 'Method not allowed' });
  }
  
  try {
    // Step 1: Authenticate with user service
    const authResponse = await fetch(endpoints.loginUrl, {
      method: 'GET',
      headers: {
        'Authorization': req.headers.authorization || '',
      } as HeadersInit,
    });
    
    if (authResponse.status !== 200) {
      return res.status(401).end();
    }
    
    const authData = await authResponse.json();
    const customerId = authData.user?.id;
    
    if (!customerId) {
      return res.status(401).end();
    }
    
    // Step 2: Merge carts (sessionId from cookie, customerId from auth)
    const sessionId = req.cookies['connect.sid'] || req.cookies['session'] || 'anonymous';
    
    try {
      await fetch(`${endpoints.cartsUrl}/${customerId}/merge?sessionId=${sessionId}`, {
        method: 'GET',
      });
      console.log('Carts merged for customer:', customerId);
    } catch (error) {
      // Log but don't fail login if cart merge fails
      console.error('Cart merge error:', error);
    }
    
    // Step 3: Set cookie and return success
    res.setHeader('Set-Cookie', [
      `logged_in=${customerId}; Path=/; HttpOnly; Max-Age=3600`,
      `customerId=${customerId}; Path=/; Max-Age=3600`,
    ]);
    
    res.status(200).send('Cookie is set');
  } catch (error) {
    console.error('Login API error:', error);
    res.status(401).end();
  }
}
