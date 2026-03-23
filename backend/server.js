// Import required packages
const express = require('express');
const cors = require('cors');

// Create an Express application
const app = express();

// Server will run on port 5000
const PORT = 5000;

// Enable CORS so your Android app (or other clients) can call this API
app.use(cors());

// Parse JSON request bodies
app.use(express.json());

// In-memory sample data (no database for now)
const games = [
  { id: 1, title: 'The Legend of Zelda: Breath of the Wild', genre: 'Action-Adventure', releaseYear: 2017 },
  { id: 2, title: 'Hades', genre: 'Roguelike', releaseYear: 2020 },
  { id: 3, title: 'Stardew Valley', genre: 'Simulation', releaseYear: 2016 },
  { id: 4, title: 'Elden Ring', genre: 'Action RPG', releaseYear: 2022 },
  { id: 5, title: 'Minecraft', genre: 'Sandbox', releaseYear: 2011 }
];

// Basic health/check route (optional but useful while testing)
app.get('/', (req, res) => {
  res.send('GameLog API is running. Try GET /games');
});

// GET /games endpoint
// Returns a JSON array of sample games
app.get('/games', (req, res) => {
  console.log('GET /games called');
  res.json(games);
});

// GET /games/:id endpoint
// Returns one game by id
app.get('/games/:id', (req, res) => {
  console.log('GET /games/:id called');

  const id = parseInt(req.params.id, 10);
  const game = games.find((item) => item.id === id);

  if (!game) {
    return res.status(404).json({ error: 'Game not found' });
  }

  res.json(game);
});

// POST /games endpoint
// Adds a new game to the in-memory array
app.post('/games', (req, res) => {
  console.log('POST /games called');

  const { title, genre, releaseYear } = req.body;

  // Basic validation for required fields
  if (!title || !genre) {
    return res.status(400).json({ error: 'title and genre are required' });
  }

  // Generate a new id (max existing id + 1)
  const maxId = games.length > 0 ? Math.max(...games.map((item) => item.id)) : 0;
  const newGame = {
    id: maxId + 1,
    title,
    genre,
    releaseYear: releaseYear || null
  };

  games.push(newGame);
  res.status(201).json(newGame);
});

// PUT /games/:id endpoint
// Replaces/updates a full game resource by id
app.put('/games/:id', (req, res) => {
  console.log('PUT /games/:id called');

  const id = parseInt(req.params.id, 10);
  const game = games.find((item) => item.id === id);

  if (!game) {
    return res.status(404).json({ error: 'Game not found' });
  }

  const { title, genre, releaseYear } = req.body;

  // For PUT, title and genre are required
  if (!title || !genre) {
    return res.status(400).json({ error: 'title and genre are required' });
  }

  game.title = title;
  game.genre = genre;
  game.releaseYear = releaseYear || null;

  res.json(game);
});

// PATCH /games/:id endpoint
// Partially updates a game resource by id
app.patch('/games/:id', (req, res) => {
  console.log('PATCH /games/:id called');

  const id = parseInt(req.params.id, 10);
  const game = games.find((item) => item.id === id);

  if (!game) {
    return res.status(404).json({ error: 'Game not found' });
  }

  const { title, genre, releaseYear } = req.body;

  // Only update fields that are provided
  if (title !== undefined) {
    game.title = title;
  }

  if (genre !== undefined) {
    game.genre = genre;
  }

  if (releaseYear !== undefined) {
    game.releaseYear = releaseYear;
  }

  res.json(game);
});

// DELETE /games/:id endpoint
// Removes a game by id
app.delete('/games/:id', (req, res) => {
  console.log('DELETE /games/:id called');

  const id = parseInt(req.params.id, 10);
  const gameIndex = games.findIndex((item) => item.id === id);

  if (gameIndex === -1) {
    return res.status(404).json({ error: 'Game not found' });
  }

  games.splice(gameIndex, 1);
  res.json({ message: 'Game deleted successfully' });
});

// Start the server
app.listen(PORT, () => {
  console.log(`GameLog API server is running on http://localhost:${PORT}`);
});
