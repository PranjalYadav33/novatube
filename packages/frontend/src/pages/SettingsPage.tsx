import React, {useState, useEffect} from 'react';

const COOKIE_STORAGE_KEY = '@user_cookie';

const SettingsPage = () => {
  const [cookieInput, setCookieInput] = useState('');
  const [storedCookie, setStoredCookie] = useState('');

  useEffect(() => {
    // Load the cookie from storage when the component mounts
    const cookie = localStorage.getItem(COOKIE_STORAGE_KEY);
    if (cookie) {
      setStoredCookie(cookie);
    }
  }, []);

  const saveCookie = () => {
    localStorage.setItem(COOKIE_STORAGE_KEY, cookieInput);
    setStoredCookie(cookieInput);
    setCookieInput('');
    alert('Cookie saved successfully!');
  };

  const clearCookie = () => {
    localStorage.removeItem(COOKIE_STORAGE_KEY);
    setStoredCookie('');
    alert('Cookie cleared.');
  };

  return (
    <div>
      <h1>Settings & Login</h1>
      <div style={{marginBottom: '20px'}}>
        <label htmlFor="cookie-input" style={{display: 'block', marginBottom: '5px'}}>
          Enter your YouTube Music cookie:
        </label>
        <textarea
          id="cookie-input"
          value={cookieInput}
          onChange={e => setCookieInput(e.target.value)}
          placeholder="Paste your cookie string here"
          rows={4}
          style={{width: '100%', maxWidth: '500px', padding: '10px'}}
        />
        <button onClick={saveCookie} style={{marginTop: '10px', padding: '10px 20px'}}>
          Save Cookie
        </button>
      </div>

      <div>
        <h3>Currently Stored Cookie:</h3>
        <p style={{fontFamily: 'monospace', wordBreak: 'break-all'}}>
          {storedCookie || 'None'}
        </p>
        <button onClick={clearCookie} style={{backgroundColor: 'red', color: 'white', border: 'none', padding: '10px 20px'}}>
          Clear Cookie
        </button>
      </div>
    </div>
  );
};

export default SettingsPage;
