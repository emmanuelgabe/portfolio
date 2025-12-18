const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

function ensureVersionPrefix(version) {
    // Always ensure version starts with 'v'
    if (version && !version.startsWith('v')) {
        return `v${version}`;
    }
    return version;
}

function getGitVersion() {
    // Check if VERSION is provided as environment variable (used in Docker builds)
    if (process.env.VERSION) {
        console.log(`Using VERSION from environment: ${process.env.VERSION}`);
        return ensureVersionPrefix(process.env.VERSION);
    }

    // Try to get version from git
    try {
        const version = execSync('git describe --tags --always --dirty', { encoding: 'utf-8' }).trim();
        return ensureVersionPrefix(version || '0.0.1-SNAPSHOT');
    } catch (error) {
        console.warn('Unable to get Git version, using default: v0.0.1-SNAPSHOT');
        return 'v0.0.1-SNAPSHOT';
    }
}

function generateVersionFile() {
    const version = getGitVersion();
    const versionFilePath = path.join(__dirname, '..', 'src', 'environments', 'version.ts');

    const content = `// This file is auto-generated. Do not edit manually.
// Generated on: ${new Date().toISOString()}

export const VERSION = '${version}';
`;

    fs.writeFileSync(versionFilePath, content, 'utf-8');
    console.log(`Version file generated: ${version}`);
}

generateVersionFile();