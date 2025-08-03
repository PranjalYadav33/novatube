import React, {useState, useEffect} from 'react';
import {browse} from '../api/client';
import Shelf from '../components/Shelf';

const HomePage = () => {
  const [loading, setLoading] = useState(true);
  const [shelves, setShelves] = useState<any[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchHomeData = async () => {
      try {
        const response = await browse('FEmusic_home');
        // Simplified parser path
        const parsedShelves =
          response.data.contents.singleColumnBrowseResultsRenderer.tabs[0]
            .tabRenderer.content.sectionListRenderer.contents;
        setShelves(parsedShelves);
      } catch (e: any) {
        setError(e.message || 'An unknown error occurred');
      } finally {
        setLoading(false);
      }
    };

    fetchHomeData();
  }, []);

  return (
    <div>
      <h1>Home Page</h1>
      {loading && <p>Loading...</p>}
      {error && <p style={{color: 'red'}}>Error: {error}</p>}
      <div>
        {shelves.map((shelf, index) => (
          <Shelf
            key={shelf.musicCarouselShelfRenderer?.shelfId || `shelf-${index}`}
            shelf={shelf.musicCarouselShelfRenderer}
          />
        ))}
      </div>
    </div>
  );
};

export default HomePage;
