const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');

dotenv.config();

const db = require('./db');

const app = express();
const PORT = Number(process.env.PORT) || 5000;

app.use(cors());
app.use(express.json());

const isPositiveInt = (value) => Number.isInteger(value) && value > 0;

const parseId = (rawValue) => {
  const parsed = Number.parseInt(rawValue, 10);
  return isPositiveInt(parsed) ? parsed : null;
};

const handleServerError = (res, error) => {
  console.error(error);
  return res.status(500).json({ error: 'Internal server error' });
};

app.get('/', (req, res) => {
  res.send('GameLog API is running. Try GET /games');
});

// Phase 1: games / explore
app.get('/games', async (req, res) => {
  try {
    const query = `
      SELECT
        g.game_id,
        g.title,
        g.description,
        g.cover_image_url,
        g.release_date,
        g.developer,
        g.publisher,
        g.platform,
        COALESCE(
          ARRAY_REMOVE(ARRAY_AGG(DISTINCT gen.genre_name), NULL),
          ARRAY[]::text[]
        ) AS genres
      FROM public.games g
      LEFT JOIN public.game_genres gg ON gg.game_id = g.game_id
      LEFT JOIN public.genres gen ON gen.genre_id = gg.genre_id
      GROUP BY g.game_id
      ORDER BY g.game_id ASC;
    `;

    const { rows } = await db.query(query, []);
    return res.json(rows);
  } catch (error) {
    return handleServerError(res, error);
  }
});

app.get('/games/:id', async (req, res) => {
  const gameId = req.params.id;

  if (!gameId) {
    return res.status(400).json({ error: 'Invalid game id' });
  }

  try {
    const query = `
      SELECT
        g.game_id,
        g.title,
        g.description,
        g.cover_image_url,
        g.release_date,
        g.developer,
        g.publisher,
        g.platform,
        COALESCE(
          ARRAY_REMOVE(ARRAY_AGG(DISTINCT gen.genre_name), NULL),
          ARRAY[]::text[]
        ) AS genres
      FROM public.games g
      LEFT JOIN public.game_genres gg ON gg.game_id = g.game_id
      LEFT JOIN public.genres gen ON gen.genre_id = gg.genre_id
      WHERE g.game_id = $1
      GROUP BY g.game_id;
    `;

    const { rows } = await db.query(query, [gameId]);

    if (rows.length === 0) {
      return res.status(404).json({ error: 'Game not found' });
    }

    return res.json(rows[0]);
  } catch (error) {
    return handleServerError(res, error);
  }
});

app.post('/games', async (req, res) => {
  const {
    title,
    description = null,
    cover_image_url = null,
    release_date = null,
    developer = null,
    publisher = null,
    platform = null
  } = req.body;

  if (!title || typeof title !== 'string') {
    return res.status(400).json({ error: 'title is required' });
  }

  try {
    const query = `
      INSERT INTO public.games (
        title,
        description,
        cover_image_url,
        release_date,
        developer,
        publisher,
        platform
      )
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      RETURNING game_id, title, description, cover_image_url, release_date, developer, publisher, platform;
    `;

    const params = [title, description, cover_image_url, release_date, developer, publisher, platform];
    const { rows } = await db.query(query, params);
    return res.status(201).json(rows[0]);
  } catch (error) {
    return handleServerError(res, error);
  }
});

// Phase 2: collection
app.post('/users/:userId/collection', async (req, res) => {
  const userId = req.params.userId;
  const gameId = req.body.game_id;
  const status = req.body.status || 'planned';

  if (!userId || !gameId) {
    return res.status(400).json({ error: 'Valid userId and game_id are required' });
  }

  try {
    const insertQuery = `
      INSERT INTO public.user_games (
        user_id,
        game_id,
        status,
        is_favourite
      )
      VALUES ($1, $2, $3, false)
      ON CONFLICT (user_id, game_id)
      DO NOTHING
      RETURNING *;
    `;

    const insertResult = await db.query(insertQuery, [userId, gameId, status]);
    const wasInserted = insertResult.rows.length > 0;

    const fetchQuery = `
      SELECT
        ug.user_game_id,
        ug.user_id,
        ug.game_id,
        ug.status,
        ug.is_favourite,
        ug.added_at,
        ug.updated_at,
        g.title,
        g.description,
        g.cover_image_url,
        g.release_date,
        g.developer,
        g.publisher,
        g.platform
      FROM public.user_games ug
      JOIN public.games g ON g.game_id = ug.game_id
      WHERE ug.user_id = $1 AND ug.game_id = $2;
    `;

    const { rows } = await db.query(fetchQuery, [userId, gameId]);

    if (rows.length === 0) {
      return res.status(404).json({ error: 'Game not found for collection entry' });
    }

    return res.status(wasInserted ? 201 : 200).json({
      created: wasInserted,
      entry: rows[0]
    });
  } catch (error) {
    return handleServerError(res, error);
  }
});

app.get('/users/:userId/collection', async (req, res) => {
  const userId = req.params.userId;

  if (!userId) {
    return res.status(400).json({ error: 'Invalid userId' });
  }

  try {
    const query = `
      SELECT
        ug.user_game_id,
        ug.user_id,
        ug.game_id,
        ug.status,
        ug.is_favourite,
        ug.added_at,
        ug.updated_at,
        g.title,
        g.description,
        g.cover_image_url,
        g.release_date,
        g.developer,
        g.publisher,
        g.platform
      FROM public.user_games ug
      JOIN public.games g ON g.game_id = ug.game_id
      WHERE ug.user_id = $1
      ORDER BY ug.added_at DESC;
    `;

    const { rows } = await db.query(query, [userId]);
    return res.json(rows);
  } catch (error) {
    return handleServerError(res, error);
  }
});

// Phase 3: favourites
app.post('/users/:userId/favourites/:gameId/toggle', async (req, res) => {
  const userId = req.params.userId;
  const gameId = req.params.gameId;

  if (!userId || !gameId) {
    return res.status(400).json({ error: 'Invalid userId or gameId' });
  }

  try {
    const query = `
      INSERT INTO public.user_games (
        user_id,
        game_id,
        status,
        is_favourite
      )
      VALUES ($1, $2, 'planned', true)
      ON CONFLICT (user_id, game_id)
      DO UPDATE
      SET is_favourite = NOT public.user_games.is_favourite,
          updated_at = NOW()
      RETURNING *;
    `;

    const { rows } = await db.query(query, [userId, gameId]);
    return res.json(rows[0]);
  } catch (error) {
    return handleServerError(res, error);
  }
});

app.get('/users/:userId/favourites', async (req, res) => {
  const userId = req.params.userId;

  if (!userId) {
    return res.status(400).json({ error: 'Invalid userId' });
  }

  try {
    const query = `
      SELECT
        ug.user_game_id,
        ug.user_id,
        ug.game_id,
        ug.status,
        ug.is_favourite,
        ug.added_at,
        g.title,
        g.description,
        g.cover_image_url
      FROM public.user_games ug
      JOIN public.games g ON g.game_id = ug.game_id
      WHERE ug.user_id = $1
        AND ug.is_favourite = true
      ORDER BY ug.updated_at DESC;
    `;

    const { rows } = await db.query(query, [userId]);
    return res.json(rows);
  } catch (error) {
    return handleServerError(res, error);
  }
});

// Phase 4: notes and reminders
app.post('/collection/:userGameId/notes', async (req, res) => {
  const userGameId = req.params.userGameId;

  if (!userGameId) {
    return res.status(400).json({ error: 'Invalid userGameId' });
  }

  const {
    title = null,
    note_text = null,
    media_uri = null,
    latitude = null,
    longitude = null,
    note_type = 'note',
    is_pinned = false,
    task_status = null,
    reminder = null,
    frequency,
    next_trigger_at,
    is_active,
    last_triggered_at,
    snoozed_until,
    timezone_id = null
  } = req.body;

  if (!['note', 'reminder'].includes(note_type)) {
    return res.status(400).json({ error: "note_type must be 'note' or 'reminder'" });
  }

  if (task_status && !['pending', 'completed'].includes(task_status)) {
    return res.status(400).json({ error: "task_status must be 'pending' or 'completed'" });
  }

  const client = await db.pool.connect();

  try {
    await client.query('BEGIN');

    const userGameQuery = `
      SELECT user_id
      FROM public.user_games
      WHERE user_game_id = $1;
    `;

    const userGameResult = await client.query(userGameQuery, [userGameId]);

    if (userGameResult.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Collection entry not found' });
    }

    const { user_id: derivedUserId } = userGameResult.rows[0];

    const noteQuery = `
      INSERT INTO public.game_notes (
        user_id,
        user_game_id,
        title,
        note_text,
        media_uri,
        latitude,
        longitude,
        note_type,
        is_pinned,
        pinned_at,
        task_status
      )
      VALUES (
        $1, $2, $3, $4, $5, $6, $7, $8, $9,
        CASE WHEN $9 = true THEN NOW() ELSE NULL END,
        $10
      )
      RETURNING *;
    `;

    const noteParams = [
      derivedUserId,
      userGameId,
      title,
      note_text,
      media_uri,
      latitude,
      longitude,
      note_type,
      Boolean(is_pinned),
      task_status
    ];

    const noteResult = await client.query(noteQuery, noteParams);
    const note = noteResult.rows[0];

    let reminderRow = null;

    const reminderPayload = reminder && typeof reminder === 'object'
      ? reminder
      : {
          frequency,
          next_trigger_at,
          is_active,
          last_triggered_at,
          snoozed_until
        };

    const hasReminderFields = reminderPayload.frequency !== undefined
      || reminderPayload.next_trigger_at !== undefined
      || reminderPayload.is_active !== undefined
      || reminderPayload.last_triggered_at !== undefined
      || reminderPayload.snoozed_until !== undefined;

    if (note_type === 'reminder' && hasReminderFields) {
      const rawFrequency = typeof reminderPayload.frequency === 'string'
        ? reminderPayload.frequency.trim().toLowerCase()
        : '';
      const reminderFrequencyAliasMap = {
        '5 min': '5_min',
        '20 min': '20_min',
        '1 hour': '1_hour',
        '6 hours': '6_hour',
        '24 hours': '1_day',
        '5_min': '5_min',
        '20_min': '20_min',
        '1_hour': '1_hour',
        '6_hour': '6_hour',
        '1_day': '1_day'
      };
      const normalizedFrequency = reminderFrequencyAliasMap[rawFrequency] || '1_hour';
      const frequencyMinutes = {
        '5_min': 5,
        '20_min': 20,
        '1_hour': 60,
        '6_hour': 360,
        '1_day': 1440
      };
      const nextTriggerAt = new Date(
        Date.now() + (frequencyMinutes[normalizedFrequency] || 60) * 60 * 1000
      ).toISOString();

      const safeTimezoneId = (typeof timezone_id === 'string' && timezone_id.trim().length > 0)
        ? timezone_id.trim()
        : null;

      const reminderQuery = `
        INSERT INTO public.game_reminders (
          note_id,
          frequency,
          is_active,
          next_trigger_at,
          last_triggered_at,
          snoozed_until,
          timezone_id
        )
        VALUES ($1, $2, $3, $4, $5, $6, $7)
        RETURNING *;
      `;

      const reminderParams = [
        note.note_id,
        normalizedFrequency,
        reminderPayload.is_active !== undefined ? Boolean(reminderPayload.is_active) : true,
        nextTriggerAt,
        reminderPayload.last_triggered_at || null,
        reminderPayload.snoozed_until || null,
        safeTimezoneId
      ];

      const reminderResult = await client.query(reminderQuery, reminderParams);
      reminderRow = reminderResult.rows[0];
    }

    await client.query('COMMIT');
    return res.status(201).json({ ...note, reminder: reminderRow });
  } catch (error) {
    await client.query('ROLLBACK');
    return handleServerError(res, error);
  } finally {
    client.release();
  }
});

app.get('/collection/:userGameId/notes', async (req, res) => {
  const userGameId = req.params.userGameId;
  const { type } = req.query;

  if (!userGameId) {
    return res.status(400).json({ error: 'Invalid userGameId' });
  }

  if (type && !['note', 'reminder'].includes(type)) {
    return res.status(400).json({ error: "type must be 'note' or 'reminder'" });
  }

  try {
    let query;
    let params;

    if (type === 'reminder') {
      query = `
        SELECT
          gn.*,
          gr.reminder_id,
          gr.frequency,
          gr.is_active,
          gr.next_trigger_at,
          gr.last_triggered_at,
          gr.snoozed_until,
          gr.timezone_id
        FROM public.game_notes gn
        LEFT JOIN public.game_reminders gr ON gr.note_id = gn.note_id
        WHERE gn.user_game_id = $1
          AND gn.note_type = 'reminder'
          AND gn.is_deleted = false
        ORDER BY gn.is_pinned DESC, gn.pinned_at DESC NULLS LAST, gn.created_at DESC;
      `;
      params = [userGameId];
    } else if (type === 'note') {
      query = `
        SELECT *
        FROM public.game_notes
        WHERE user_game_id = $1
          AND note_type = $2
          AND is_deleted = false
        ORDER BY is_pinned DESC, pinned_at DESC NULLS LAST, created_at DESC;
      `;
      params = [userGameId, 'note'];
    } else {
      query = `
        SELECT
          gn.*,
          gr.reminder_id,
          gr.frequency,
          gr.is_active,
          gr.next_trigger_at,
          gr.last_triggered_at,
          gr.snoozed_until,
          gr.timezone_id
        FROM public.game_notes gn
        LEFT JOIN public.game_reminders gr ON gr.note_id = gn.note_id
        WHERE gn.user_game_id = $1
          AND gn.is_deleted = false
        ORDER BY gn.is_pinned DESC, gn.pinned_at DESC NULLS LAST, gn.created_at DESC;
      `;
      params = [userGameId];
    }

    const { rows } = await db.query(query, params);
    return res.json(rows);
  } catch (error) {
    return handleServerError(res, error);
  }
});

app.get('/users/:userId/notes', async (req, res) => {
  const userId = req.params.userId;
  const { type } = req.query;

  if (!userId) {
    return res.status(400).json({ error: 'Invalid userId' });
  }

  if (type && !['note', 'reminder'].includes(type)) {
    return res.status(400).json({ error: "type must be 'note' or 'reminder'" });
  }

  try {
    const query = `
      SELECT
        gn.*,
        ug.game_id,
        g.title AS game_title,
        gr.reminder_id,
        gr.frequency,
        gr.is_active,
        gr.next_trigger_at,
        gr.last_triggered_at,
        gr.snoozed_until,
        gr.timezone_id
      FROM public.game_notes gn
      JOIN public.user_games ug ON ug.user_game_id = gn.user_game_id
      LEFT JOIN public.games g ON g.game_id = ug.game_id
      LEFT JOIN public.game_reminders gr ON gr.note_id = gn.note_id
      WHERE gn.user_id = $1
        AND gn.is_deleted = false
        AND ($2::note_type_enum IS NULL OR gn.note_type = $2::note_type_enum)
      ORDER BY gn.is_pinned DESC, gn.pinned_at DESC NULLS LAST, gn.created_at DESC;
    `;

    const { rows } = await db.query(query, [userId, type || null]);
    return res.json(rows);
  } catch (error) {
    return handleServerError(res, error);
  }
});

app.patch('/notes/:noteId/pin', async (req, res) => {
  const noteId = req.params.noteId;

  if (!noteId) {
    return res.status(400).json({ error: 'Invalid noteId' });
  }

  try {
    const query = `
      UPDATE public.game_notes
      SET
        is_pinned = NOT is_pinned,
        pinned_at = CASE WHEN NOT is_pinned THEN NOW() ELSE NULL END,
        updated_at = NOW()
      WHERE note_id = $1
        AND is_deleted = false
      RETURNING *;
    `;

    const { rows } = await db.query(query, [noteId]);

    if (rows.length === 0) {
      return res.status(404).json({ error: 'Note not found' });
    }

    return res.json(rows[0]);
  } catch (error) {
    return handleServerError(res, error);
  }
});

app.patch('/notes/:noteId/task-status', async (req, res) => {
  const noteId = req.params.noteId;
  const { task_status } = req.body;

  if (!noteId) {
    return res.status(400).json({ error: 'Invalid noteId' });
  }

  if (!['pending', 'completed'].includes(task_status)) {
    return res.status(400).json({ error: "task_status must be 'pending' or 'completed'" });
  }

  try {
    const query = `
      UPDATE public.game_notes
      SET
        task_status = $2::task_status_enum,
        completed_at = CASE
          WHEN $2::task_status_enum = 'completed'::task_status_enum THEN NOW()
          WHEN $2::task_status_enum = 'pending'::task_status_enum THEN NULL
          ELSE completed_at
        END,
        updated_at = NOW()
      WHERE note_id = $1
        AND note_type = 'reminder'
        AND is_deleted = false
      RETURNING *;
    `;

    const { rows } = await db.query(query, [noteId, task_status]);

    if (rows.length === 0) {
      return res.status(404).json({ error: 'Note not found' });
    }

    return res.json(rows[0]);
  } catch (error) {
    return handleServerError(res, error);
  }
});

app.delete('/notes/:noteId', async (req, res) => {
  const noteId = req.params.noteId;

  if (!noteId) {
    return res.status(400).json({ error: 'Invalid noteId' });
  }

  try {
    const query = `
      UPDATE public.game_notes
      SET
        is_deleted = true,
        is_pinned = false,
        pinned_at = NULL,
        updated_at = NOW()
      WHERE note_id = $1
        AND is_deleted = false
      RETURNING *;
    `;

    const { rows } = await db.query(query, [noteId]);

    if (rows.length === 0) {
      return res.status(404).json({ error: 'Note not found' });
    }

    return res.json(rows[0]);
  } catch (error) {
    return handleServerError(res, error);
  }
});

// Phase 5: profile
app.get('/users/:userId/profile', async (req, res) => {
  const userId = (req.params.userId);

  if (!userId) {
    return res.status(400).json({ error: 'Invalid userId' });
  }

  try {
    const query = `
      SELECT
        u.user_id,
        u.display_name,
        u.email,
        u.avatar_url,
        COALESCE(stats.total_games, 0) AS total_games,
        COALESCE(stats.total_favourites, 0) AS total_favourites,
        COALESCE(stats.total_notes, 0) AS total_notes,
        COALESCE(stats.total_reminders, 0) AS total_reminders,
        COALESCE(stats.completed_tasks, 0) AS completed_tasks,
        COALESCE(activity.total_time_spent, 0) AS total_time_spent,
        activity.last_activity_at
      FROM public.users u
      LEFT JOIN (
        SELECT
          ug.user_id,
          COUNT(DISTINCT ug.user_game_id) AS total_games,
          COUNT(DISTINCT ug.user_game_id) FILTER (WHERE ug.is_favourite = true) AS total_favourites,
          COUNT(gn.note_id) FILTER (WHERE gn.is_deleted = false AND gn.note_type = 'note') AS total_notes,
          COUNT(gn.note_id) FILTER (WHERE gn.is_deleted = false AND gn.note_type = 'reminder') AS total_reminders,
          COUNT(gn.note_id) FILTER (
            WHERE gn.is_deleted = false
              AND gn.note_type = 'reminder'
              AND gn.task_status = 'completed'
          ) AS completed_tasks
        FROM public.user_games ug
        LEFT JOIN public.game_notes gn ON gn.user_game_id = ug.user_game_id
        GROUP BY ug.user_id
      ) stats ON stats.user_id = u.user_id
      LEFT JOIN (
        SELECT
          user_id,
          COALESCE(SUM(duration_seconds), 0) AS total_time_spent,
          MAX(occurred_at) AS last_activity_at
        FROM public.user_activity_logs
        GROUP BY user_id
      ) activity ON activity.user_id = u.user_id
      WHERE u.user_id = $1;
    `;

    const { rows } = await db.query(query, [userId]);

    if (rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    return res.json(rows[0]);
  } catch (error) {
    return handleServerError(res, error);
  }
});

app.post('/users/:userId/activity', async (req, res) => {
  const userId = req.params.userId;
  const {
    action_name,
    entity_type = 'screen',
    entity_id = null,
    action_details = null,
    duration_seconds = 0,
    occurred_at = null
  } = req.body || {};

  if (!userId) {
    return res.status(400).json({ error: 'Invalid userId' });
  }

  const normalizedActionName = typeof action_name === 'string' ? action_name.trim() : '';
  if (!normalizedActionName) {
    return res.status(400).json({ error: 'action_name is required' });
  }

  const normalizedEntityType = typeof entity_type === 'string'
    ? entity_type.trim().toLowerCase()
    : '';
  const allowedEntityTypes = new Set([
    'user',
    'game',
    'user_game',
    'game_note',
    'game_reminder',
    'auth_account',
    'explore',
    'notification',
    'system'
  ]);
  const legacyEntityTypeMap = {
    shell_tab: 'system',
    screen: 'system'
  };
  let resolvedEntityType = allowedEntityTypes.has(normalizedEntityType)
    ? normalizedEntityType
    : (legacyEntityTypeMap[normalizedEntityType] || 'system');

  const normalizedEntityId = typeof entity_id === 'string' ? entity_id.trim() : '';
  const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
  let resolvedEntityId = uuidPattern.test(normalizedEntityId) ? normalizedEntityId : null;

  const normalizedDuration = Number.parseInt(duration_seconds, 10);
  if (!Number.isInteger(normalizedDuration) || normalizedDuration < 0) {
    return res.status(400).json({ error: 'duration_seconds must be a non-negative integer' });
  }

  let normalizedActionDetails = null;
  if (typeof action_details === 'string') {
    const trimmedDetails = action_details.trim();
    if (trimmedDetails) {
      try {
        const parsedDetails = JSON.parse(trimmedDetails);
        if (parsedDetails && typeof parsedDetails === 'object' && !Array.isArray(parsedDetails)) {
          normalizedActionDetails = parsedDetails;
        } else {
          normalizedActionDetails = { label: trimmedDetails };
        }
      } catch (jsonError) {
        normalizedActionDetails = { label: trimmedDetails };
      }
    }
  } else if (action_details && typeof action_details === 'object' && !Array.isArray(action_details)) {
    normalizedActionDetails = action_details;
  }

  if (normalizedActionName.toLowerCase() === 'screen_view') {
    const detailsLabel = normalizedActionDetails && typeof normalizedActionDetails.label === 'string'
      ? normalizedActionDetails.label.trim().toLowerCase()
      : '';

    let screenName = null;
    if (normalizedEntityId && !resolvedEntityId) {
      screenName = normalizedEntityId;
    } else if (detailsLabel.includes('game detail')) {
      screenName = 'game_detail';
    } else if (detailsLabel.includes('collection')) {
      screenName = 'collection';
    } else if (detailsLabel.includes('explore')) {
      screenName = 'explore';
    }

    if (!screenName) {
      screenName = 'unknown_screen';
    }

    if (!resolvedEntityId) {
      resolvedEntityType = 'system';
      resolvedEntityId = null;
    }

    const screenDetails = { screen: screenName };
    if (screenName === 'game_detail') {
      if (detailsLabel.includes('collection')) {
        screenDetails.origin = 'collection';
      } else if (detailsLabel.includes('explore')) {
        screenDetails.origin = 'explore';
      }
    }
    normalizedActionDetails = screenDetails;
  }

  try {
    const query = `
      INSERT INTO public.user_activity_logs (
        user_id,
        action_name,
        entity_type,
        entity_id,
        action_details,
        duration_seconds,
        occurred_at
      )
      VALUES ($1, $2, $3, $4, $5::jsonb, $6, COALESCE($7::timestamptz, NOW()))
      RETURNING *;
    `;

    const params = [
      userId,
      normalizedActionName,
      resolvedEntityType,
      resolvedEntityId,
      normalizedActionDetails ? JSON.stringify(normalizedActionDetails) : null,
      normalizedDuration,
      occurred_at
    ];

    const { rows } = await db.query(query, params);
    return res.status(201).json(rows[0]);
  } catch (error) {
    return handleServerError(res, error);
  }
});

app.get('/users/:userId/activity', async (req, res) => {
  const userId = (req.params.userId);
  const limitValue = Number.parseInt(req.query.limit, 10);
  const limit = Number.isInteger(limitValue) && limitValue > 0 ? Math.min(limitValue, 100) : 20;

  if (!userId) {
    return res.status(400).json({ error: 'Invalid userId' });
  }

  try {
    const query = `
      SELECT
        activity_log_id AS activity_id,
        user_id,
        action_name AS action_type,
        entity_type,
        entity_id,
        action_details,
        duration_seconds,
        occurred_at
      FROM public.user_activity_logs
      WHERE user_id = $1
      ORDER BY occurred_at DESC
      LIMIT $2;
    `;

    const { rows } = await db.query(query, [userId, limit]);
    return res.json(rows);
  } catch (error) {
    return handleServerError(res, error);
  }
});

app.get('/users/resolve', async (req, res) => {
  const authUserId = typeof req.query.authUserId === 'string' ? req.query.authUserId.trim() : '';
  const email = typeof req.query.email === 'string' ? req.query.email.trim() : '';
  const displayName = typeof req.query.displayName === 'string' ? req.query.displayName.trim() : '';

  if (!email) {
    return res.status(400).json({ error: 'email is required' });
  }

  const fallbackDisplayName = displayName || email.split('@')[0] || 'Player';

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    const byEmailQuery = `
      SELECT
        u.user_id,
        u.auth_user_id,
        u.email,
        u.display_name,
        u.avatar_url
      FROM public.users u
      WHERE LOWER(u.email) = LOWER($1)
      LIMIT 1;
    `;
    const emailResult = await client.query(byEmailQuery, [email]);

    if (emailResult.rows.length > 0) {
      await client.query('COMMIT');
      return res.json(emailResult.rows[0]);
    }

    // Intentionally ignore authUserId here because users.auth_user_id is UUID-typed
    // and Firebase uid values are not UUIDs.
    void authUserId;

    const createUserQuery = `
      INSERT INTO public.users (
        email,
        display_name,
        is_active
      )
      VALUES ($1, $2, true)
      RETURNING user_id, auth_user_id, email, display_name, avatar_url;
    `;

    const createResult = await client.query(createUserQuery, [email, fallbackDisplayName]);
    await client.query('COMMIT');
    return res.status(201).json(createResult.rows[0]);
  } catch (error) {
    try {
      await client.query('ROLLBACK');
    } catch (rollbackError) {
      console.error(rollbackError);
    }
    return handleServerError(res, error);
  } finally {
    client.release();
  }
});

app.listen(PORT, () => {
  console.log(`GameLog API server is running on http://localhost:${PORT}`);
});
