import React from 'react';
import {BrowserRouter, Routes, Route, Link} from 'react-router-dom';
import {PlayerProvider} from './context/PlayerContext';
import HomePage from './pages/HomePage';
import SearchPage from './pages/SearchPage';
import PlayerBar from './components/PlayerBar';
import LibraryPage from './pages/LibraryPage';
import SettingsPage from './pages/SettingsPage';
import './App.css';

function App() {
  return (
    <BrowserRouter>
      <PlayerProvider>
        <div className="app-container">
          <nav className="navbar">
            <Link to="/">Home</Link>
            <Link to="/search">Search</Link>
            <Link to="/library">Library</Link>
            <Link to="/settings">Settings</Link>
          </nav>

          <main className="content">
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/search" element={<SearchPage />} />
              <Route path="/library" element={<LibraryPage />} />
              <Route path="/settings" element={<SettingsPage />} />
            </Routes>
          </main>
          <PlayerBar />
        </div>
      </PlayerProvider>
    </BrowserRouter>
  );
}

export default App;
