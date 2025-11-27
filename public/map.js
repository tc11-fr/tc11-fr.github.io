// Leaflet - Carte des installations TC11
if(typeof L !== 'undefined' && document.getElementById('map')) {
  // Liste des installations TC11
  const installations = [
    { name: 'C.S. Alain Mimoun', coords: [48.8535, 2.4087], terrains: 4, surface: 'béton poreux', url: 'https://www.paris.fr/lieux/tennis-alain-mimoun-ex-paul-valery-2965' },
    { name: 'Candie', coords: [48.8543, 2.3760], terrains: 3, surface: 'gazon synthétique', url: 'https://www.paris.fr/lieux/tennis-candie-19092' },
    { name: 'Carnot', coords: [48.8584, 2.3844], terrains: 1, surface: 'synthétique', url: 'https://www.paris.fr/lieux/tennis-carnot-3318' },
    { name: 'La Faluère', coords: [48.8700, 2.3929], terrains: 1, surface: 'béton poreux', url: 'https://www.paris.fr/lieux/tennis-la-faluere-2964' },
    { name: 'Les Lilas', coords: [48.8748, 2.4127], terrains: 1, surface: 'terre battue', url: 'https://www.google.com/maps/place/Centre+de+Comit%C3%A9+de+Tennis+de+Paris-Est+Les+Lilas/' },
    { name: 'Philippe Auguste', coords: [48.8527, 2.3921], terrains: 1, surface: 'synthétique', url: 'https://www.paris.fr/lieux/tennis-philippe-auguste-17244' },
    { name: 'Thiéré', coords: [48.8546, 2.3870], terrains: 1, surface: 'béton poreux', url: 'https://www.paris.fr/lieux/tennis-thiere-19075' }
  ];

  // Calculer le centre de toutes les installations
  const latSum = installations.reduce((sum, i) => sum + i.coords[0], 0);
  const lngSum = installations.reduce((sum, i) => sum + i.coords[1], 0);
  const center = [latSum / installations.length, lngSum / installations.length];

  const map = L.map('map').setView(center, 13);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '&copy; OpenStreetMap' }).addTo(map);

  // Ajouter un marqueur pour chaque installation
  const markers = installations.map(installation => {
    const popupContent = `
      <strong>${installation.name}</strong><br>
      ${installation.terrains} terrain${installation.terrains > 1 ? 's' : ''}<br>
      Revêtement : ${installation.surface}<br>
      <a href="${installation.url}" target="_blank" rel="noopener">Plus d'infos →</a>
    `;
    return L.marker(installation.coords).addTo(map).bindPopup(popupContent);
  });

  // Ajuster la vue pour afficher tous les marqueurs
  const group = L.featureGroup(markers);
  map.fitBounds(group.getBounds().pad(0.1));
}
