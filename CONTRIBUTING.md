# Guide de contribution

Merci de contribuer au site du TC11 ! 🎾

## 📝 Convention pour les titres de Pull Request

Tous les titres de Pull Request doivent suivre la convention **Conventional Commits**.

### Format

```
<type>: <description>
```

ou avec un scope optionnel :

```
<type>(<scope>): <description>
```

### Types autorisés

| Type | Description |
|------|-------------|
| `feat` | Nouvelle fonctionnalité |
| `fix` | Correction de bug |
| `docs` | Documentation |
| `style` | Mise en forme (pas de changement de code) |
| `refactor` | Refactorisation du code |
| `perf` | Amélioration des performances |
| `test` | Ajout ou modification de tests |
| `build` | Changements du système de build |
| `ci` | Configuration CI/CD |
| `chore` | Autres changements |
| `revert` | Annulation d'un commit |

### Exemples

✅ **Valides :**
- `feat: Ajoute une nouvelle page d'actualités`
- `fix(navigation): Corrige le menu mobile`
- `docs: Met à jour le README`

❌ **Invalides :**
- `Ajoute une nouvelle page` (pas de type)
- `FEAT: nouvelle page` (type en majuscules)

## 🚀 Processus de contribution

1. Forker le projet
2. Créer une branche (`git checkout -b feature/ma-fonctionnalite`)
3. Commiter vos changements avec un message conventionnel
4. Pousser la branche (`git push origin feature/ma-fonctionnalite`)
5. Ouvrir une Pull Request avec un titre conventionnel
