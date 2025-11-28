# Contributing Guide

Thank you for contributing to the TC11 website! 🎾

## 📝 Pull Request Title Convention

All Pull Request titles must follow the **Conventional Commits** convention and be written **in English**.

### Format

```
<type>: <description>
```

or with an optional scope:

```
<type>(<scope>): <description>
```

### Allowed Types

| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation |
| `style` | Formatting (no code change) |
| `refactor` | Code refactoring |
| `perf` | Performance improvement |
| `test` | Adding or updating tests |
| `build` | Build system changes |
| `ci` | CI/CD configuration |
| `chore` | Other changes |
| `revert` | Revert a commit |

### Examples

✅ **Valid:**
- `feat: Add new news page`
- `fix(navigation): Fix mobile menu`
- `docs: Update README`

❌ **Invalid:**
- `Add new page` (missing type)
- `FEAT: new page` (type in uppercase)
- `feat: Ajoute une page` (not in English)

## 🚀 Contribution Process

1. Fork the project
2. Create a branch (`git checkout -b feature/my-feature`)
3. Commit your changes with a conventional message
4. Push the branch (`git push origin feature/my-feature`)
5. Open a Pull Request with a conventional title in English
