(function (){
  'use strict';

  var session      = require("express-session");
  var RedisStore   = require('connect-redis').default;
  var { createClient } = require('redis');

  // Create Redis client for connect-redis v7+
  var redisClient = createClient({
    socket: {
      host: process.env.REDIS_HOST || 'session-db',
      port: process.env.REDIS_PORT || 6379
    },
    legacyMode: false
  });

  redisClient.connect().catch(console.error);

  module.exports = {
    session: {
      name: 'md.sid',
      secret: 'sooper secret',
      resave: false,
      saveUninitialized: true
    },

    session_redis: {
      store: new RedisStore({ 
        client: redisClient,
        prefix: 'sockshop:'
      }),
      name: 'md.sid',
      secret: 'sooper secret',
      resave: false,
      saveUninitialized: true
    }
  };
}());
