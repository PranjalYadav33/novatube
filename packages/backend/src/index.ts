import express from 'express';
import cors from 'cors';
import YtMusic from './api/YtMusic';
import {Request, Response} from 'express';

const app = express();
const port = process.env.PORT || 3000;

// Middleware
app.use(cors()); // Allow requests from the frontend
app.use(express.json()); // Parse JSON bodies

// Root route for health check
app.get('/', (req, res) => {
  res.send('Backend server is running!');
});

// API routes will be added here in the next step
app.post('/api/search', async (req: Request, res: Response) => {
  try {
    const {query, params, continuation, authenticated, cookie} = req.body;

    const ytMusic = new YtMusic();
    if (authenticated && cookie) {
      ytMusic.setCookie(cookie);
    }

    const data = await ytMusic.search(query, params, continuation, authenticated);
    res.json(data);
  } catch (error: any) {
    res.status(500).json({error: error.message});
  }
});

app.post('/api/browse', async (req: Request, res: Response) => {
  try {
    const {browseId, params, authenticated, cookie} = req.body;

    const ytMusic = new YtMusic();
    if (authenticated && cookie) {
      ytMusic.setCookie(cookie);
    }

    const data = await ytMusic.browse(browseId, params, authenticated);
    res.json(data);
  } catch (error: any) {
    res.status(500).json({error: error.message});
  }
});

app.post('/api/player', async (req: Request, res: Response) => {
  try {
    const {videoId, playlistId, authenticated, cookie} = req.body;

    const ytMusic = new YtMusic();
    if (authenticated && cookie) {
      ytMusic.setCookie(cookie);
    }

    const data = await ytMusic.player(videoId, playlistId, authenticated);
    res.json(data);
  } catch (error: any) {
    res.status(500).json({error: error.message});
  }
});

app.listen(port, () => {
  console.log(`Backend server listening on port ${port}`);
});
