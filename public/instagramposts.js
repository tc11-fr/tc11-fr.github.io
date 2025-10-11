// Dates
if(document.getElementById('year'))
  document.getElementById('year').textContent = new Date().getFullYear();
if(document.getElementById('maj-date'))
  document.getElementById('maj-date').textContent = dayjs().format('DD MMM YYYY');

// Leaflet (modifie les coords par l’adresse du club)
if(typeof L !== 'undefined' && document.getElementById('map')) {
  const map = L.map('map').setView([48.8566, 2.3522], 12);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '&copy; OpenStreetMap' }).addTo(map);
  L.marker([48.8566, 2.3522]).addTo(map).bindPopup('TC11 — Nos courts').openPopup();
}

// Grille Instagram via oEmbed (chargement dynamique depuis instagram.json)
if(document.getElementById('insta-grid')) {
  const grid = document.getElementById('insta-grid');
  fetch('/instagram.json')
    .then(res => res.json())
    .then(posts => {
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
    })
    .catch(e => {
      console.error('Erreur chargement instagram.json', e);
    });
}
