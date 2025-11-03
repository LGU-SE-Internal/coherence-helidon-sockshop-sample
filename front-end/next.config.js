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
}

module.exports = nextConfig
