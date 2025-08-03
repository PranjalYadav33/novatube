import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:3000/api', // The backend proxy URL
});

export const search = (query: string, cookie?: string) => {
  return apiClient.post('/search', {
    query,
    cookie,
    authenticated: !!cookie,
  });
};

export const browse = (browseId: string, cookie?: string) => {
  return apiClient.post('/browse', {
    browseId,
    cookie,
    authenticated: !!cookie,
  });
};

export const getPlayer = (videoId: string, cookie?: string) => {
  return apiClient.post('/player', {
    videoId,
    cookie,
    authenticated: !!cookie,
  });
};
