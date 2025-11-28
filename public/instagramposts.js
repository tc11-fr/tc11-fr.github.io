// Dates
if(document.getElementById('year'))
  document.getElementById('year').textContent = new Date().getFullYear();
if(document.getElementById('maj-date'))
  document.getElementById('maj-date').textContent = dayjs().format('DD MMM YYYY');

// Configuration Instagram
const INSTAGRAM_USERNAME = 'tc11assb';
const RSS_BRIDGE_URL = `https://rss-bridge.org/bridge01/?action=display&bridge=InstagramBridge&context=Username&u=${encodeURIComponent(INSTAGRAM_USERNAME)}&media_type=all&direct_links=on&format=Json`;
const MAX_POSTS = 6;

/**
 * Parse RSS Bridge JSON response and extract post URLs.
 * @param {Object} data - RSS Bridge JSON response
 * @returns {string[]} Array of Instagram post URLs
 */
function parseRssBridgeResponse(data) {
  const postUrls = [];
  if (data && Array.isArray(data.items)) {
    for (const item of data.items) {
      if (postUrls.length >= MAX_POSTS) break;
      // Try 'url' field first, then 'id'
      let url = item.url || item.id || '';
      if (url && url.includes('instagram.com/p/')) {
        postUrls.push(url);
      }
    }
  }
  return postUrls;
}

/**
 * Fetch Instagram posts from RSS Bridge.
 * @returns {Promise<string[]>} Array of Instagram post URLs
 */
async function fetchFromRssBridge() {
  const response = await fetch(RSS_BRIDGE_URL, {
    headers: { 'Accept': 'application/json' }
  });
  if (!response.ok) {
    throw new Error(`RSS Bridge returned status ${response.status}`);
  }
  const data = await response.json();
  return parseRssBridgeResponse(data);
}

/**
 * Fetch Instagram posts from static instagram.json.
 * @returns {Promise<string[]>} Array of Instagram post URLs
 */
async function fetchFromStaticJson() {
  const response = await fetch('/instagram.json');
  if (!response.ok) {
    throw new Error(`instagram.json returned status ${response.status}`);
  }
  return await response.json();
}

/**
 * Merge and deduplicate post URLs, keeping order from fresh posts first.
 * @param {string[]} freshPosts - Posts from RSS Bridge (client-side)
 * @param {string[]} staticPosts - Posts from instagram.json (server-side)
 * @returns {string[]} Merged and deduplicated post URLs
 */
function mergePosts(freshPosts, staticPosts) {
  const seen = new Set();
  const result = [];
  
  // Add fresh posts first (they may have newer content)
  for (const url of freshPosts) {
    const normalized = url.replace(/\/$/, ''); // Remove trailing slash for comparison
    if (!seen.has(normalized)) {
      seen.add(normalized);
      result.push(url);
    }
  }
  
  // Add static posts that aren't already included
  for (const url of staticPosts) {
    const normalized = url.replace(/\/$/, '');
    if (!seen.has(normalized)) {
      seen.add(normalized);
      result.push(url);
    }
  }
  
  return result.slice(0, MAX_POSTS);
}

/**
 * Render Instagram posts in the grid.
 * @param {string[]} posts - Array of Instagram post URLs
 */
function renderInstagramPosts(posts) {
  const grid = document.getElementById('insta-grid');
  if (!grid) return;
  
  posts.forEach(url => {
    const wrapper = document.createElement('div');
    wrapper.innerHTML = `
      <blockquote class="instagram-media" data-instgrm-permalink="${url}" data-instgrm-version="14" style="background:#fff; border:0; margin:0; padding:0; width:100%;">
      </blockquote>
    `;
    grid.appendChild(wrapper);
  });
  
  // Si des posts ont été ajoutés, déclenche le rendu embed
  function processEmbedsWhenReady(){
    if (window.instgrm && window.instgrm.Embeds && typeof window.instgrm.Embeds.process === 'function'){
      window.instgrm.Embeds.process();
    } else {
      setTimeout(processEmbedsWhenReady, 300);
    }
  }
  if (posts.length) processEmbedsWhenReady();
}

// Grille Instagram via oEmbed (chargement dynamique avec fallback)
if(document.getElementById('insta-grid')) {
  (async function() {
    let staticPosts = [];
    let freshPosts = [];
    
    // Always load static posts from instagram.json as fallback
    try {
      staticPosts = await fetchFromStaticJson();
    } catch (e) {
      console.warn('Erreur chargement instagram.json:', e);
    }
    
    // Try to fetch fresh posts from RSS Bridge
    try {
      freshPosts = await fetchFromRssBridge();
    } catch (e) {
      console.warn('Erreur chargement RSS Bridge (using static posts):', e);
    }
    
    // Merge fresh and static posts, deduplicating
    const posts = mergePosts(freshPosts, staticPosts);
    
    if (posts.length > 0) {
      renderInstagramPosts(posts);
    } else {
      console.error('Aucun post Instagram disponible');
    }
  })();
}
