// API route for register - maintains backend compatibility
import type { NextApiRequest, NextApiResponse } from 'next';
import { getBackendEndpoints } from '../../utils/backend';

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }
  
  const endpoints = getBackendEndpoints();
  
  try {
    // Step 1: Register user
    const registerResponse = await fetch(endpoints.registerUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      } as HeadersInit,
      body: JSON.stringify(req.body),
    });
    
    if (registerResponse.status !== 200) {
      const errorData = await registerResponse.text();
      return res.status(registerResponse.status).send(errorData);
    }
    
    const registerData = await registerResponse.json();
    
    if (registerData.error) {
      return res.status(400).json(registerData);
    }
    
    const customerId = registerData.id;
    
    if (!customerId) {
      return res.status(500).json({ error: 'Registration failed - no customer ID returned' });
    }
    
    // Step 2: Login the newly registered user
    const loginResponse = await fetch(endpoints.loginUrl, {
      method: 'GET',
      headers: {
        'Authorization': `Basic ${Buffer.from(`${req.body.username}:${req.body.password}`).toString('base64')}`,
      } as HeadersInit,
    });
    
    if (loginResponse.status === 200) {
      // Step 3: Merge carts
      const sessionId = req.cookies['connect.sid'] || req.cookies['session'] || 'anonymous';
      
      try {
        await fetch(`${endpoints.cartsUrl}/${customerId}/merge?sessionId=${sessionId}`, {
          method: 'GET',
        });
        console.log('Carts merged for new customer:', customerId);
      } catch (error) {
        console.error('Cart merge error after registration:', error);
      }
      
      // Step 4: Set cookies
      res.setHeader('Set-Cookie', [
        `logged_in=${customerId}; Path=/; HttpOnly; Max-Age=3600`,
        `customerId=${customerId}; Path=/; Max-Age=3600`,
      ]);
    }
    
    res.status(200).json(registerData);
  } catch (error) {
    console.error('Register API error:', error);
    res.status(500).json({ error: 'Failed to register with user service' });
  }
}
