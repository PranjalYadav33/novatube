import React, {useState} from 'react';
import {search} from '../api/client';
import Shelf from '../components/Shelf';

const SearchPage = () => {
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [shelves, setShelves] = useState<any[]>([]);
  const [error, setError] = useState<string | null>(null);

  const handleSearch = async () => {
    if (!query) return;
    setLoading(true);
    setError(null);
    setShelves([]);
    try {
      const response = await search(query);
      // Simplified parser path
      const parsedShelves =
        response.data.contents.tabbedSearchResultsRenderer.tabs[0].tabRenderer
          .content.sectionListRenderer.contents;
      setShelves(parsedShelves);
    } catch (e: any) {
      setError(e.message || 'An unknown error occurred');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h1>Search Page</h1>
      <div style={{display: 'flex', alignItems: 'center', marginBottom: '20px'}}>
        <input
          type="text"
          value={query}
          onChange={e => setQuery(e.target.value)}
          placeholder="Search for a song..."
          style={{padding: '10px', fontSize: '16px', width: '300px', marginRight: '10px'}}
          onKeyDown={e => e.key === 'Enter' && handleSearch()}
        />
        <button onClick={handleSearch} disabled={loading} style={{padding: '10px 20px', fontSize: '16px'}}>
          {loading ? 'Searching...' : 'Search'}
        </button>
      </div>

      {error && <p style={{color: 'red'}}>Error: {error}</p>}
      {loading && <p>Loading...</p>}
      <div>
        {shelves.map((shelf, index) => (
          <Shelf
            key={
              shelf.musicCardShelfRenderer?.shelfId ||
              shelf.musicShelfRenderer?.shelfId ||
              `shelf-${index}`
            }
            shelf={shelf.musicCardShelfRenderer || shelf.musicShelfRenderer}
          />
        ))}
      </div>
    </div>
  );
};

export default SearchPage;
