/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  output: 'standalone',
  compiler: {
    styledComponents: true,
  },
  images: {
    domains: ['localhost'],
  },
  async rewrites() {
    return [
      // Serve static HTML files from public directory
      {
        source: '/',
        destination: '/index.html',
      },
      {
        source: '/index',
        destination: '/index.html',
      },
      {
        source: '/basket',
        destination: '/basket.html',
      },
      {
        source: '/detail',
        destination: '/detail.html',
      },
      {
        source: '/category',
        destination: '/category.html',
      },
      {
        source: '/checkout1',
        destination: '/checkout1.html',
      },
      {
        source: '/checkout2',
        destination: '/checkout2.html',
      },
      {
        source: '/checkout3',
        destination: '/checkout3.html',
      },
      {
        source: '/checkout4',
        destination: '/checkout4.html',
      },
      {
        source: '/customer-account',
        destination: '/customer-account.html',
      },
      {
        source: '/customer-order',
        destination: '/customer-order.html',
      },
      {
        source: '/customer-orders',
        destination: '/customer-orders.html',
      },
      {
        source: '/customer-wishlist',
        destination: '/customer-wishlist.html',
      },
      // Rewrite API calls from frontend format to Next.js API routes
      {
        source: '/catalogue:path*',
        destination: '/api/catalogue:path*',
      },
      {
        source: '/cart:path*',
        destination: '/api/cart:path*',
      },
      {
        source: '/orders:path*',
        destination: '/api/orders:path*',
      },
      {
        source: '/customers:path*',
        destination: '/api/customers:path*',
      },
      {
        source: '/addresses:path*',
        destination: '/api/addresses:path*',
      },
      {
        source: '/cards:path*',
        destination: '/api/cards:path*',
      },
      {
        source: '/login',
        destination: '/api/login',
      },
      {
        source: '/register',
        destination: '/api/register',
      },
      {
        source: '/tags',
        destination: '/api/tags',
      },
    ];
  },
}

module.exports = nextConfig
