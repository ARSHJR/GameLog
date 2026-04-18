const { Pool } = require('pg');

const databaseUrl = process.env.DATABASE_URL;
const sslEnabled = String(process.env.DB_SSL || 'true').toLowerCase() === 'true';

if (!databaseUrl) {
  throw new Error('Missing DATABASE_URL environment variable');
}

const pool = new Pool({
  connectionString: databaseUrl,
  ssl: sslEnabled ? { rejectUnauthorized: false } : false
});

module.exports = {
  query: (text, params) => pool.query(text, params),
  pool
};
