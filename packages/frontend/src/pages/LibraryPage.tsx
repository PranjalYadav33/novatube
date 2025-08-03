import React, {useState, useEffect} from 'react';
import {browse} from '../api/client';

const COOKIE_STORAGE_KEY = '@user_cookie';

const LibraryPage = () => {
  const [loading, setLoading] = useState(true);
  const [playlists, setPlaylists] = useState<any[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  const fetchLibraryData = async () => {
    setLoading(true);
    setError(null);
    try {
      const cookie = localStorage.getItem(COOKIE_STORAGE_KEY);
      if (cookie) {
        setIsLoggedIn(true);
        const response = await browse('FEmusic_liked_playlists', cookie);
        // Simplified parser path
        const parsedPlaylists =
          response.data.contents.singleColumnBrowseResultsRenderer.tabs[0]
            .tabRenderer.content.sectionListRenderer.contents[0]
            .gridRenderer.items;
        setPlaylists(parsedPlaylists);
      } else {
        setIsLoggedIn(false);
        setError('You are not logged in. Please add your cookie in Settings.');
      }
    } catch (e: any) {
      setError(e.message || 'An unknown error occurred');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLibraryData();
  }, []);

  if (loading) {
    return <p>Loading...</p>;
  }

  if (!isLoggedIn || error) {
    return (
      <div>
        <h1>Library</h1>
        <p style={{color: 'red'}}>{error}</p>
        <button onClick={fetchLibraryData}>Retry</button>
      </div>
    );
  }

  return (
    <div>
      <h1>Your Library</h1>
      <ul>
        {playlists.map(item => {
          const playlist = item.musicTwoRowItemRenderer;
          return (
            <li key={playlist.navigationEndpoint.browseEndpoint.browseId}>
              {playlist.title.runs[0].text}
            </li>
          );
        })}
      </ul>
    </div>
  );
};

export default LibraryPage;
