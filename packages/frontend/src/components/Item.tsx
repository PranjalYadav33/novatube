import React from 'react';
import {usePlayer} from '../context/PlayerContext';
import './Item.css';

const getThumbnailUrl = (thumbnails: any[]) => {
  if (!thumbnails || thumbnails.length === 0) {
    return 'https://via.placeholder.com/150'; // Placeholder image
  }
  return thumbnails[thumbnails.length - 1].url;
};

const Item = ({item}: {item: any}) => {
  const {playTrack} = usePlayer();
  const musicItem = item.musicTwoRowItemRenderer || item.musicResponsiveListItemRenderer;

  if (!musicItem) return null;

  const thumbnailUrl = getThumbnailUrl(musicItem.thumbnail.musicThumbnailRenderer.thumbnail.thumbnails);
  const title = musicItem.title.runs[0].text;
  const artists = musicItem.subtitle?.runs.map((run: any) => run.text).join('');

  return (
    <div className="item" onClick={() => playTrack(musicItem.navigationEndpoint.watchEndpoint)}>
      <img src={thumbnailUrl} alt={title} className="item-thumbnail" />
      <div className="item-title">{title}</div>
      <div className="item-subtitle">{artists}</div>
    </div>
  );
};

export default Item;
