# Vale Styles Directory

This directory contains Vale style packages and project vocabulary.

## Structure

```
.vale/styles/
├── config/
│   └── vocabularies/
│       └── Portfolio/        # Project-specific vocabulary
│           ├── accept.txt    # Accepted terms (technical jargon, proper nouns)
│           └── reject.txt    # Rejected terms (informal language, ambiguous words)
└── Google/                   # Downloaded Google style guide (gitignored)
```

## Package Management

Packages are defined in `.vale.ini` at project root:

```ini
Packages = Google
```

Run `vale sync` to download packages.

## Vocabulary

### accept.txt
Add project-specific terms that should be accepted:
- Technology names (Angular, PostgreSQL, MapStruct)
- Acronyms (API, CI/CD, JPA)
- Project-specific terms (frontend, backend)

### reject.txt
Add terms that should be flagged:
- Informal language (gonna, wanna)
- Ambiguous words (simply, easy, just)

## Configuration

Main configuration is in `.vale.ini` at project root.
