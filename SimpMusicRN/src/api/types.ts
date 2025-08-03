// This file will contain all the type definitions for the YouTube Music API.

// A generic representation of a client context
export interface Context {
  client: {
    clientName: string;
    clientVersion: string;
    // ... and so on
  };
  user?: {};
  request?: {};
}

// The body of a POST request to the /search endpoint
export interface SearchBody {
  context: Context;
  query?: string;
  params?: string;
}

// A placeholder for the search response.
// This will need to be fleshed out based on actual API responses.
export interface SearchResponse {
  contents: any;
  // ... other properties
}

// The body of a POST request to the /browse endpoint
export interface BrowseBody {
  context: Context;
  browseId?: string;
  params?: string;
}

// A placeholder for the browse response.
export interface BrowseResponse {
  contents: any;
  // ... other properties
}

// The body of a POST request to the /playlist/create endpoint
export interface CreatePlaylistBody {
  context: Context;
  title: string;
  videoIds?: string[];
}

export interface CreatePlaylistResponse {
  playlistId: string;
  // ... other properties
}

// Add other request/response types here as more functions are ported.
