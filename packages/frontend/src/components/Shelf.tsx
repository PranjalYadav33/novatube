import React from 'react';
import Item from './Item';
import './Shelf.css';

const Shelf = ({shelf}: {shelf: any}) => {
  if (!shelf || !shelf.contents || shelf.contents.length === 0) {
    return null;
  }

  return (
    <div className="shelf">
      <h2 className="shelf-title">{shelf.title}</h2>
      <div className="shelf-contents">
        {shelf.contents.map((item: any, index: number) => (
          <Item key={item.videoId || `shelf-item-${index}`} item={item} />
        ))}
      </div>
    </div>
  );
};

export default Shelf;
