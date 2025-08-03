import axios, {AxiosInstance, AxiosRequestHeaders} from 'axios';
import SHA1 from 'crypto-js/sha1';
import {
  BrowseBody,
  BrowseResponse,
  Context,
  CreatePlaylistBody,
  CreatePlaylistResponse,
  PlayerBody,
  PlayerResponse,
  SearchBody,
  SearchResponse,
} from './types';

const BASE_URL = 'https://music.youtube.com/youtubei/v1/';

const parseCookieString = (cookie: string): Record<string, string> => {
  const cookiePairs = cookie.split('; ');
  const cookieMap: Record<string, string> = {};
  for (const pair of cookiePairs) {
    const [key, value] = pair.split('=');
    cookieMap[key] = value;
  }
  return cookieMap;
};

// This replicates the YouTubeClient object from the Kotlin code
const YOUTUBE_CLIENT = {
  WEB_REMIX: {
    clientName: 'WEB_REMIX',
    clientVersion: '1.20210401.01.00',
    userAgent:
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
    api_key: process.env.YT_API_KEY, // We'll need to handle this
  },
  // Other clients like ANDROID_MUSIC can be added here
};

class YtMusic {
  private apiClient: AxiosInstance;
  private visitorData: string = 'Cgt6SUNYVzB2VkJDbyjGrrSmBg%3D%3D';
  private locale = {
    gl: 'US',
    hl: 'en',
  };
  private cookie?: string;
  private cookieMap: Record<string, string> = {};

  constructor() {
    this.apiClient = axios.create({
      baseURL: BASE_URL,
      headers: {
        'Content-Type': 'application/json',
        'X-Goog-Api-Format-Version': '1',
        'x-origin': 'https://music.youtube.com',
        'X-Goog-Visitor-Id': this.visitorData,
      },
      params: {
        prettyPrint: false,
      },
    });
  }

  public setCookie(cookie: string) {
    this.cookie = cookie;
    this.cookieMap = parseCookieString(this.cookie);
  }

  private getAuthenticationHeaders(): AxiosRequestHeaders {
    if (!this.cookie) {
      return {} as AxiosRequestHeaders;
    }

    const sapisid = this.cookieMap['SAPISID'];
    if (!sapisid) {
      return {Cookie: this.cookie} as AxiosRequestHeaders;
    }

    const timestamp = Math.floor(Date.now() / 1000);
    const hash = SHA1(
      `${timestamp} ${sapisid} https://music.youtube.com`,
    ).toString();
    const authorization = `SAPISIDHASH ${timestamp}_${hash}`;

    return {
      Cookie: this.cookie,
      Authorization: authorization,
    } as AxiosRequestHeaders;
  }

  private getContext(client: typeof YOUTUBE_CLIENT.WEB_REMIX): Context {
    return {
      client: {
        clientName: client.clientName,
        clientVersion: client.clientVersion,
      },
      // ... other context properties
    };
  }

  public async search(
    query: string,
    params?: string,
    continuation?: string,
    authenticated = false,
  ): Promise<SearchResponse> {
    const client = YOUTUBE_CLIENT.WEB_REMIX;
    const body: SearchBody = {
      context: this.getContext(client),
      query: query,
      params: params,
    };

    const headers = {
      'X-YouTube-Client-Name': client.clientName,
      'X-YouTube-Client-Version': client.clientVersion,
      'User-Agent': client.userAgent,
      ...(authenticated ? this.getAuthenticationHeaders() : {}),
    };

    const response = await this.apiClient.post('search', body, {
      params: {
        continuation: continuation,
        ctoken: continuation,
        key: client.api_key,
      },
      headers,
    });

    return response.data;
  }

  // Other methods like browse, player, etc. will be ported here
  public async browse(
    browseId: string,
    params?: string,
    authenticated = false,
  ): Promise<BrowseResponse> {
    const client = YOUTUBE_CLIENT.WEB_REMIX;
    const body: BrowseBody = {
      context: this.getContext(client),
      browseId: browseId,
      params: params,
    };

    const headers = {
      'X-YouTube-Client-Name': client.clientName,
      'X-YouTube-Client-Version': client.clientVersion,
      'User-Agent': client.userAgent,
      ...(authenticated ? this.getAuthenticationHeaders() : {}),
    };

    const response = await this.apiClient.post('browse', body, {
      params: {
        key: client.api_key,
      },
      headers,
    });

    return response.data;
  }

  public async player(
    videoId: string,
    playlistId?: string,
    authenticated = false,
  ): Promise<PlayerResponse> {
    const client = YOUTUBE_CLIENT.WEB_REMIX;
    const body: PlayerBody = {
      context: this.getContext(client),
      videoId: videoId,
      playlistId: playlistId,
      cpn: 'Cg1vdXJfaWRfMV9fMRAA', // This is a magic value, may need to be dynamic
    };

    const headers = {
      'X-YouTube-Client-Name': client.clientName,
      'X-YouTube-Client-Version': client.clientVersion,
      'User-Agent': client.userAgent,
      ...(authenticated ? this.getAuthenticationHeaders() : {}),
    };

    const response = await this.apiClient.post('player', body, {
      params: {
        key: client.api_key,
      },
      headers,
    });

    return response.data;
  }

  public async createPlaylist(
    title: string,
    videoIds?: string[],
  ): Promise<CreatePlaylistResponse> {
    const client = YOUTUBE_CLIENT.WEB_REMIX;
    const body: CreatePlaylistBody = {
      context: this.getContext(client),
      title,
      videoIds,
    };

    const headers = {
      'X-YouTube-Client-Name': client.clientName,
      'X-YouTube-Client-Version': client.clientVersion,
      'User-Agent': client.userAgent,
      ...this.getAuthenticationHeaders(), // This must be an authenticated call
    };

    const response = await this.apiClient.post('playlist/create', body, {
      params: {
        key: client.api_key,
      },
      headers,
    });

    return response.data;
  }
}

export default new YtMusic();
