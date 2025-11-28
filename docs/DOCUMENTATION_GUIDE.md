# Guide de documentation

Ce guide definit les standards et bonnes pratiques pour la documentation du projet Portfolio.

---

## Table of Contents

1. [Framework Diataxis](#framework-diataxis)
2. [Templates par type](#templates-par-type)
3. [Regles de documentation](#regles-de-documentation)
4. [Checklist PR](#checklist-pr)

---

## Framework Diataxis

Ce projet suit le framework Diataxis pour organiser la documentation :

| Type | But | Orientation | Exemples |
|------|-----|-------------|----------|
| **Tutorial** | Apprendre | Etude + Pratique | `setup.md` |
| **How-to** | Accomplir une tache | Action + Pratique | `ci-cd.md`, `local-testing.md` |
| **Reference** | Information factuelle | Etude + Action | `api/*.md`, `error-codes.md` |
| **Explanation** | Comprendre le pourquoi | Etude + Theorie | `architecture.md`, `jwt-implementation.md` |

### Application dans le projet

| Categorie docs/ | Type Diataxis |
|-----------------|---------------|
| `api/` | Reference |
| `architecture/` | Explanation |
| `features/` | Explanation + Reference |
| `security/` | Explanation |
| `development/` | Tutorial + How-to |
| `reference/` | Reference |
| `deployment/` | How-to |
| `operations/` | How-to + Reference |

---

## Templates par type

### Template API (`docs/api/*.md`)

```markdown
# [Resource] API

Brief description (2-3 lines)

---

## Table of Contents
1. [Overview](#overview)
2. [Public Endpoints](#public-endpoints)
3. [Admin Endpoints](#admin-endpoints)
4. [Data Models](#data-models)
5. [Error Codes](#error-codes)

---

## Overview
What this API does

## Public Endpoints
### Get [Resource]
` ` `
GET /api/[resource]
` ` `
**Response:**
...

## Admin Endpoints
(if applicable)

## Data Models
### [Resource]Response
` ` `json
{
  "id": "Long",
  "field": "String"
}
` ` `

## Error Codes
| Code | Message | Description |
|------|---------|-------------|

## Related Documentation
- Links to related docs
```

### Template Feature (`docs/features/*.md`)

```markdown
# [Feature Name]

Brief description (3-5 bullet points)

---

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Configuration](#configuration)
4. [Database Schema](#database-schema)

---

## Overview
What this feature does

## Architecture
### Backend Components
| Component | Location | Purpose |
|-----------|----------|---------|

### Frontend Components
| Component | Location | Purpose |
|-----------|----------|---------|

## Configuration
` ` `yaml
app:
  feature:
    property: value
` ` `

## Database Schema
` ` `sql
CREATE TABLE ...
` ` `

## Related Documentation
- Links to related docs
```

### Template Architecture (`docs/architecture/*.md`)

```markdown
# [Component] Architecture

Brief overview

---

## Table of Contents
1. [Overview](#overview)
2. [Components](#components)
3. [Data Flow](#data-flow)
4. [Configuration](#configuration)

---

## Overview
High-level description

## Components
Diagram + descriptions

## Data Flow
How data moves through the system

## Configuration
Relevant configuration

## Related Documentation
- Links to related docs
```

---

## Regles de documentation

### A documenter

| Element | Obligatoire | Notes |
|---------|-------------|-------|
| APIs publiques | Oui | Tous les endpoints |
| Configuration complexe | Oui | YAML, properties |
| Decisions architecturales | Oui | Choix techniques |
| Securite | Oui | Auth, validation |
| Processus de deploiement | Oui | CI/CD, scripts |

### A ne pas documenter

| Element | Raison |
|---------|--------|
| Code interne evident | Auto-documentant |
| Getters/Setters | Trivial |
| Implementation triviale | Pas de valeur ajoutee |
| Details temporaires | Volatil |
| Code experimental | Non stable |

### Sections interdites

Les sections suivantes sont **interdites** dans la documentation (sauf exceptions notees) :

| Section | Raison | Exception |
|---------|--------|-----------|
| Testing | Dediee a testing-guide.md | `development/testing-guide.md` |
| Troubleshooting | Maintenance difficile | `development/setup.md` |
| Performance | Hors scope | Aucune |
| Future Enhancements | Volatil | Aucune |
| Migration | Hors scope | Aucune |
| References footer | Meta-information | Aucune |

### Longueur recommandee

| Type | Minimum | Cible | Maximum |
|------|---------|-------|---------|
| API doc | 100 | 200-400 | 500 |
| Feature doc | 100 | 200-400 | 500 |
| Architecture doc | 150 | 300-500 | 700 |

---

## Checklist PR

Avant de soumettre une PR modifiant la documentation :

### Contenu

- [ ] Nouveau code public = documentation mise a jour
- [ ] Template correct utilise
- [ ] Liens vers docs connexes ajoutes
- [ ] Table of Contents presente et a jour

### Format

- [ ] Pas de sections interdites (Testing, Troubleshooting, Future)
- [ ] Longueur dans les limites (200-400 lignes)
- [ ] Code examples limites a la configuration
- [ ] Pas d'emojis

### Qualite

- [ ] Liens internes fonctionnels
- [ ] Anglais correct (pas de Franglais)
- [ ] Termes techniques coherents
- [ ] Information factuelle (pas de speculation)

---

## Related Documentation

- [Coverage Matrix](./COVERAGE.md) - Matrice de couverture
