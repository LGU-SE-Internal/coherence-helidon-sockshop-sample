// Backend service endpoints configuration
// Maintains compatibility with existing Helidon backend services

export const getBackendEndpoints = () => {
  const domain = process.env.BACKEND_DOMAIN || '';
  
  return {
    catalogueUrl: `http://catalogue${domain}`,
    tagsUrl: `http://catalogue${domain}/tags`,
    cartsUrl: `http://carts${domain}/carts`,
    ordersUrl: `http://orders${domain}`,
    customersUrl: `http://user${domain}/customers`,
    addressUrl: `http://user${domain}/addresses`,
    cardsUrl: `http://user${domain}/cards`,
    loginUrl: `http://user${domain}/login`,
    registerUrl: `http://user${domain}/register`,
  };
};
